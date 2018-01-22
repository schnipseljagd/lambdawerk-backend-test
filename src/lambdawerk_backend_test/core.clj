(ns lambdawerk-backend-test.core
  (:require [clojure.java.io :as io]
            [clojure.instant :refer [read-instant-date]]
            [clojure.spec.alpha :as s]
            [clojure.string :refer [blank?]]
            [clojure.spec.test.alpha :as stest]
            [lambdawerk-backend-test.xml-reader :as xml]
            [clojure.java.jdbc :as j])
  (:import (java.io Reader)))

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
    (read-instant-date s)
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

(defn do-something [member]
  (s/assert ::person member)
  member)

(defn read-member-updates [^Reader reader]
  (-> reader
      (xml/parse)
      (xml/get-members)
      (->> (pmap (comp do-something ->person xml/member->map)))))

(defn update-persons-table [persons]
  (doseq [persons-chunk (partition 10 persons)]
    (prn persons-chunk)))

(s/check-asserts true)

(comment
  ;(with-open [reader (io/reader "update-file.xml")]
  (time (-> (io/reader "update-file.xml")
            (read-member-updates)
            (->> (take 100))
            (update-persons-table))))

