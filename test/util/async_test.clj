(ns util.async-test
  (:require [clojure.test :refer :all]
            [util.async :refer :all]))

(deftest throws-exception-when-executor-fails
  (is (thrown? Exception (run-in-parallel [1 2 3]
                                          (fn [_] (throw (ex-info "foo" {})))
                                          2))))

(deftest returns-a-list-with-results
  (let [input [0 2 4 6 8 10]
        result (run-in-parallel input
                                (fn [in]
                                  (inc in))
                                2)]
    (is input result)))
