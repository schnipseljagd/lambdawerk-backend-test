(ns lambdawerk-backend-test.core
  (:require [clojure.java.io :as io]
            [clojure.spec.alpha :as s]
            [clojure.string :refer [blank?]]
            [clojure.spec.test.alpha :as stest]
            [lambdawerk-backend-test.xml-reader :as xml]
            [clojure.java.jdbc :as j]
            [hikari-cp.core :refer [make-datasource close-datasource]]
            [miner.strgen :as sg]
            [util.date :refer [parse-date]]
            [clj-time.jdbc]
            [util.async :as async])
  (:import (java.io Reader)
           (java.util.concurrent Executors ExecutorService)))

(s/def ::firstname (s/and string? (complement blank?)))
(s/def ::lastname (s/and string? (complement blank?)))

(def phone-regex #"^[0-9]{10}$")
(s/def ::phone (s/spec (s/and string? #(re-matches phone-regex %))
                       :gen #(sg/string-generator phone-regex)))

(s/def ::date-of-birth (s/nilable :clj-time.spec/date-time))

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

(defn clean-person [person]
  (update person :date-of-birth parse-date))

(defn validate-person [person]
  (s/assert ::person person)
  person)

(defn xml->persons [^Reader reader]
  (-> reader
      (xml/parse)
      (xml/get-members)
      (->> (pmap (comp validate-person clean-person xml/member->map)))))

(def insert-counter (atom 0))

(def insert-or-update-persons-table-query
  "insert into person (fname,lname,dob) values (?, ?, ?) on conflict (fname,lname,dob) do update set phone = ? where person.phone != ?")

(defn insert-or-update-persons-table [datasource persons]
  (let [result (j/execute! {:datasource datasource}
                           (into
                             [insert-or-update-persons-table-query]
                             (map (fn [{:keys [firstname lastname phone date-of-birth]}]
                                    [firstname lastname date-of-birth (str phone) (str phone)])
                                  persons))
                           {:multi? true})]
    (prn "insert " (swap! insert-counter inc) " " result)))

(defn update-persons-table [persons
                            {:keys [transaction-chunk-size number-of-executors]}
                            datasource-options]
  (let [datasource (make-datasource datasource-options)]
    (-> (partition transaction-chunk-size persons)
        (async/run-in-parallel (partial insert-or-update-persons-table datasource)
                               number-of-executors))
    (close-datasource datasource)))

(s/check-asserts true)

(comment
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

