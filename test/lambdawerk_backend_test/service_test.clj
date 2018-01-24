(ns lambdawerk-backend-test.service-test
  (:require [clojure.test :refer :all]
            [lambdawerk-backend-test.service :refer :all]
            [clojure.java.io :as io])
  (:import (clojure.lang ExceptionInfo)))

(def example-xml "<members><member><firstname>00226501</firstname><lastname>MCGREWJR</lastname><date-of-birth>1936-02-01</date-of-birth><phone>9796740198</phone></member><member><firstname>00226501</firstname><lastname>SCHENERLEIN</lastname><date-of-birth>\\N</date-of-birth><phone>5709742596</phone></member></members>")
(def invalid-member-xml "<members><member><firstname>00226501</firstname><lastname>MCGREWJR</lastname><date-of-birth>1936-02-01</date-of-birth><phone>broken</phone></member></members>")

(defn str->input-stream [^String s]
  (io/input-stream (.getBytes s)))

(deftest xml->persons-returns-a-list-of-persons
  (let [persons (-> example-xml
                    (str->input-stream)
                    (xml->persons))]
    (is (= 2 (count persons)))))

(deftest xml->persons-populates-persons-correctly
  (let [persons (-> example-xml
                    (str->input-stream)
                    (xml->persons))]
    (are [n person] (= (nth persons n) person)
                    0 {:firstname     "00226501"
                       :lastname      "MCGREWJR"
                       :date-of-birth "1936-02-01"
                       :phone         "9796740198"}
                    1 {:firstname     "00226501"
                       :lastname      "SCHENERLEIN"
                       :date-of-birth ""
                       :phone         "5709742596"})))

(deftest xml->persons-validates-persons
  (is (thrown-with-msg? ExceptionInfo
                        #"Spec assertion failed"
                        (-> invalid-member-xml
                            (str->input-stream)
                            (xml->persons)
                            (doall)))))
