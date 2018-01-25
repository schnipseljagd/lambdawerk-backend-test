(ns lambdawerk-backend-test.db-integration-test
  (:require [clojure.test :refer :all]
            [lambdawerk-backend-test.db :refer :all]
            [clojure.java.jdbc :as j]
            [hikari-cp.core :refer [make-datasource]]
            [honeysql.core :as sql]
            [honeysql.helpers :refer [insert-into values where]])
  (:import (clojure.lang ExceptionInfo)))

(def datasource-options lambdawerk-backend-test.core/datasource-options)

(defn ensure-test-table-exists []
  (with-open [datasource (make-datasource datasource-options)]
    (j/execute! {:datasource datasource} ["CREATE TEMPORARY TABLE IF NOT EXISTS  test (foo integer NOT NULL)"])))

(comment
  (with-open [datasource (make-datasource datasource-options)]
    (j/query {:datasource datasource} ["select * from test"])))

(defn execute [test-data gen-fn]
  (batch-execute-in-parallel test-data
                             {:batch-size 2 :number-of-executors 2}
                             datasource-options
                             gen-fn))

(deftest batch-execute-in-parallel-returns-results-of-the-applied-statement
  (ensure-test-table-exists)

  (let [gen-fn (fn [batch] (-> (insert-into :test)
                               (values (partition-all 1 batch))
                               sql/format))]
      (is (= [2 2] (-> (execute [1 2 3 4] gen-fn)
                       (batch-execution-results))))
      (is (= [2 1] (-> (execute [1 2 3] gen-fn)
                       (batch-execution-results))))))

(deftest batch-execute-in-parallel-fails
  (let [test-data [1 2 3 4]
        gen-fn (fn [_] (throw (ex-info "fooo" {})))]
    (is (thrown-with-msg? Exception
                          #"fooo"
                          (execute test-data gen-fn)))))
