(ns lambdawerk-backend-test.db
  (:require [hikari-cp.core :refer [make-datasource close-datasource]]
            [clojure.java.jdbc :as j]
            [util.async :as async]))

(def insert-counter (atom 0))

(def insert-or-update-persons-table-query
  "insert into person (fname,lname,dob) values (?, ?, ?) on conflict (fname,lname,dob) do update set phone = ? where person.phone != ?")

(defn insert-or-update-persons-table [datasource persons]
  (let [result (j/execute! {:datasource datasource}
                           (into
                             [insert-or-update-persons-table-query]
                             (map (fn [{:keys [firstname
                                               lastname
                                               phone
                                               date-of-birth]}]
                                    [firstname
                                     lastname
                                     date-of-birth
                                     phone
                                     phone])
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
