(ns lambdawerk-backend-test.db
  (:require [hikari-cp.core :refer [make-datasource]]
            [clojure.java.jdbc :as j]
            [util.async :as async]
            [util.measure :refer [take-time]]
            [honeysql.core :as sql]
            [honeysql.helpers :refer [insert-into values where]]
            [honeysql-postgres.format]
            [honeysql-postgres.helpers :refer [upsert on-conflict do-update-set]]))

(defn insert-or-update-persons-statement
  "Inserts a new person if the identifier (firstname, lastname, date-of-birth) doesn't exist.
   Updates an existing person only if the phone number differs from the existing one.
   Updates the phone number and possibly a list of additional keys.
   Returns a vector with an SQL statement."
  [persons]
  (let [insert-values (->> persons
                           (map #(clojure.set/rename-keys % {:firstname     :fname
                                                             :lastname      :lname
                                                             :date-of-birth :dob})))
        update-keys (-> insert-values
                        (first)
                        (dissoc :fname :lname :dob)
                        (keys))
        do-update-set!! (fn [m values]
                          (apply do-update-set m values))
        excluded-phone (sql/raw "EXCLUDED.phone")]

    (-> (insert-into :person)
        (values insert-values)
        (upsert (-> (on-conflict :fname :lname :dob)
                    (do-update-set!! update-keys)
                    (where [:<> :person.phone excluded-phone])))
        sql/format)))

(defn batch-execute-in-parallel
  "Executes SQL statements in parallel.
   The statement-generator-fn is applied with a batch of coll.
   Returns a tuple with measured time in msecs and the sum of the results of the applied statement."
  [coll
   {:keys [batch-size number-of-executors]}
   datasource-options
   statement-generator-fn]
  (let [execute! (fn [datasource batch]
                   (take-time
                     (->> (statement-generator-fn batch)
                          (j/execute! {:datasource datasource})
                          (reduce +))))]
    (with-open [datasource (make-datasource datasource-options)]
      (-> (partition-all batch-size coll)
          (async/run-in-parallel (partial execute! datasource)
                                 number-of-executors)))))

(defn batch-execution-results [result]
  (map second result))

(defn batch-execution-latencies [result]
  (map first result))
