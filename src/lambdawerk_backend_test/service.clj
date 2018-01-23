(ns lambdawerk-backend-test.service
  (:require [clojure.spec.alpha :as s]
            [clojure.string :refer [blank?]]
            [miner.strgen :as sg]
            [clojure.spec.test.alpha :as stest]
            [clj-time.spec :as time-spec]
            [util.date :refer [parse-date]]
            [clojure.data.xml :as xml]
            [lambdawerk-backend-test.xml-members :as members])
  (:import (java.io Reader)))

(s/check-asserts true)

(s/def ::firstname (s/and string? (complement blank?)))
(s/def ::lastname (s/and string? (complement blank?)))

(def phone-regex #"^[0-9]{10}$")
(s/def ::phone (s/spec (s/and string? #(re-matches phone-regex %))
                       :gen #(sg/string-generator phone-regex)))

(s/def ::date-of-birth (s/nilable ::time-spec/date-time))

(s/def ::person (s/keys :req-un [::firstname ::lastname ::phone ::date-of-birth]))

(comment
  (s/exercise ::person)
  (s/exercise-fn `validate-person)
  (stest/check `validate-person))

(defn- clean-person [person]
  (update person :date-of-birth parse-date))

(defn- validate-person [person]
  (s/assert ::person person)
  person)

(defn xml->persons [^Reader reader]
  (-> reader
      (xml/parse)
      (members/get-all)
      (->> (map (comp validate-person clean-person members/member->map)))))
