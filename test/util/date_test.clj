(ns util.date-test
  (:require [clojure.test :refer :all]
            [util.date :refer :all])
  (:import (org.joda.time DateTime)))

(deftest converts-a-date-to-a-date-time-object
  (let [result (parse-date "1987-11-29")]
    (is (= "1987-11-29T00:00:00.000Z" (str result)))
    (is (instance? DateTime result))))

(deftest converts-a-invalid-date-to-nil
  (is (nil? (parse-date "\\N")))
  (is (nil? (parse-date ""))))
