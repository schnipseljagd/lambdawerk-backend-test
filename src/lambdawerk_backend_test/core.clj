(ns lambdawerk-backend-test.core
  (:require [clojure.java.io :as io]
            [clojure.instant :refer [read-instant-timestamp]]
            [clojure.spec.alpha :as s]
            [clojure.string :refer [blank?]]
            [clojure.spec.test.alpha :as stest]
            [lambdawerk-backend-test.xml-reader :as xml]
            [clojure.java.jdbc :as j]
            [hikari-cp.core :refer [make-datasource close-datasource]])
  (:import (java.io Reader)
           (java.util.concurrent Executors ExecutorService)))

(s/def ::firstname (s/and string? (complement blank?)))
(s/def ::lastname (s/and string? (complement blank?)))
(s/def ::phone (s/int-in 1000000000 10000000000))
(s/def ::date-of-birth (s/nilable inst?))

(s/def ::person (s/keys :req-un [::firstname ::lastname ::phone ::date-of-birth]))

(comment
  (s/exercise ::person)
  (stest/check `do-something))


; ALTER TABLE person ADD UNIQUE (fname,lname,dob);
; insert into person (fname,lname,dob) values ('DENAIJAHA', 'VILLOLOBOS', '1982-07-03') on conflict (fname,lname,dob) do update set (phone) = ('6607186022') where person.phone != '6607186022';

; Are there other clients running writes/reads on the database in production?
; Is the production database already under load?
; Should it be configurable how much load the update task generates on the production database?
; What is more important a fast import or being a good neighbour on the production database (if there are other clients)?

(def broken-dates (atom {}))

(defn parse-date-of-birth [s]
  (try
    (read-instant-timestamp s)
    (catch RuntimeException e
      (swap! broken-dates update s (fnil inc 0))
      nil)))

(defn parse-int [s]
  (try
    (Long/parseLong s)
    (catch RuntimeException e)))

(defn ->person [{:keys [date-of-birth] :as member}]
  (-> member
      (update :date-of-birth parse-date-of-birth)
      (update :phone parse-int)))

(defn validate-person [member]
  (s/assert ::person member)
  member)

(defn xml->persons [^Reader reader]
  (-> reader
      (xml/parse)
      (xml/get-members)
      (->> (pmap (comp validate-person ->person xml/member->map)))))

(def insert-counter (atom 0))

(defn run-in-parallel [data function number-of-executors]
  (let [service (Executors/newFixedThreadPool number-of-executors)
        submit (fn [function data] (.submit ^ExecutorService service
                                            ^Callable (fn [] (function data))))]
    (->> data
         (map (partial submit function))
         (doall)
         (map deref)
         (doall))))

(comment
  (run-in-parallel [1 2 3 4 5 6 7 8 9 10]
                   (fn [in] (prn in))
                   4))

(def datasource-options {:auto-commit       false
                         :minimum-idle      1
                         :maximum-pool-size 10
                         :pool-name         "db-pool"
                         :adapter           "postgresql"
                         :username          "postgres"
                         :password          "password"
                         :database-name     "postgres"
                         :server-name       "localhost"})

(defn insert-or-update-persons-table [datasource persons]
  (j/with-db-transaction
    [t-con {:datasource datasource}]
    (doseq [{:keys [firstname lastname phone date-of-birth]} persons]
      (j/execute! t-con
                  ["insert into person (fname,lname,dob) values (?, ?, ?) on conflict (fname,lname,dob) do update set phone = ? where person.phone != ?"
                   firstname
                   lastname
                   date-of-birth
                   (str phone)
                   (str phone)])))

  (prn "insert: " (swap! insert-counter inc)))

(defn update-persons-table [persons
                            {:keys [transaction-chunk-size number-of-executors]}
                            datasource-options]
  (let [datasource (make-datasource datasource-options)]
    (-> (partition transaction-chunk-size persons)
        (run-in-parallel (partial insert-or-update-persons-table datasource)
                         number-of-executors))
    (close-datasource datasource)))

(s/check-asserts true)

(comment
  (j/query pg-db "select * from person limit 10")
  (j/execute! pg-db "ALTER TABLE person ADD UNIQUE (fname,lname,dob)")
  ;(with-open [reader (io/reader "update-file.xml")]
  (time (-> (io/reader "update-file.xml")
            (xml->persons)
            (update-persons-table {:transaction-chunk-size 10000
                                   :number-of-executors    4}
                                  {:minimum-idle      1
                                   :maximum-pool-size 10
                                   :pool-name         "db-pool"
                                   :adapter           "postgresql"
                                   :username          "postgres"
                                   :password          "password"
                                   :database-name     "postgres"
                                   :server-name       "localhost"}))))

