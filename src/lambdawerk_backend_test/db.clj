(ns lambdawerk-backend-test.db
  (:require [hikari-cp.core :refer [make-datasource close-datasource]]
            [clojure.java.jdbc :as j]
            [util.async :as async]
            [util.measure :refer [take-time]]
            [clj-time.jdbc]
            [honeysql.core :as sql]
            [honeysql.helpers :refer [insert-into values where]]
            [honeysql-postgres.format]
            [honeysql-postgres.helpers :refer [upsert on-conflict do-update-set]]))

(defn insert-or-update-persons-statement [persons]
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

(defn insert-or-update-persons-table
  "Inserts a new person if the identifier (firstname, lastname, date-of-birth) doesn't exist.
   Updates an existing person only if the phone number differs from the existing one.
   Updates the phone number and possibly a list of additional keys.
   Returns a tuple with measured time in msecs and the number of applied inserts and updates."
  [datasource persons]
  (take-time
    (let [statement (insert-or-update-persons-statement persons)]
      (first
        (j/execute! {:datasource datasource} statement)))))

(defn update-persons-table [persons
                            {:keys [batch-size number-of-executors]}
                            datasource-options]
  (let [datasource (make-datasource datasource-options)
        results (-> (partition batch-size persons)
                    (async/run-in-parallel (partial insert-or-update-persons-table datasource)
                                           number-of-executors))]
    (close-datasource datasource)
    results))
