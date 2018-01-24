(ns lambdawerk-backend-test.service
  (:require [clojure.spec.alpha :as s]
            [clojure.string :refer [blank?]]
            [miner.strgen :as sg]
            [clojure.spec.test.alpha :as stest]
            [clojure.data.xml :as xml]
            [lambdawerk-backend-test.xml-members :as members])
  (:import (java.io Reader)))

(s/check-asserts true)

(s/def ::firstname (s/and string? (complement blank?)))
(s/def ::lastname (s/and string? (complement blank?)))

(def phone-regex #"^[0-9]{10}$")
(s/def ::phone (s/spec (s/and string? #(re-matches phone-regex %))
                       :gen #(sg/string-generator phone-regex)))

(def date-of-birth-regex #"^([0-9]{4}-[0-9]{2}-[0-9]{2})?$")
(s/def ::date-of-birth (s/spec (s/and string? #(re-matches date-of-birth-regex %))
                               :gen #(sg/string-generator date-of-birth-regex)))

(s/def ::person (s/keys :req-un [::firstname ::lastname ::phone ::date-of-birth]))

(comment
  (s/exercise ::person))

(defn clean-date-of-birth [dob]
  (try
    (clojure.instant/read-instant-date dob)
    dob
    (catch RuntimeException e
      "")))

(defn- clean-person [person]
  (update person :date-of-birth clean-date-of-birth))

(defn- validate-person [person]
  (s/assert ::person person)
  person)

(defn xml->persons
  "Reads the XML member list and transforms it into a person map list."
  [^Reader reader]
  (-> reader
      (xml/parse)
      (members/get-all)
      (->> (map (comp validate-person clean-person members/member->map)))))
