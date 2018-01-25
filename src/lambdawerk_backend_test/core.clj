(ns lambdawerk-backend-test.core
  (:gen-class)
  (:require [clojure.java.io :as io]
            [lambdawerk-backend-test.service :refer [xml->persons]]
            [lambdawerk-backend-test.db :as db]
            [com.stuartsierra.frequencies :as freq]
            [util.measure :refer [take-time]]
            [clojure.pprint :refer [pprint]]))

(defn stats
  "All latencies are in msecs."
  [[overall-latency results]]
  (let [stats (->> results
                   (db/batch-execution-latencies)
                   (frequencies)
                   (freq/stats))
        number-of-updates (->> results
                               (db/batch-execution-results)
                               (reduce +))]
    {:overall-latency           overall-latency
     :batch-write-latency-stats stats
     :number-of-updates         number-of-updates}))

(def datasource-options
  {:minimum-idle      1
   :maximum-pool-size 10
   :pool-name         "db-pool"
   :adapter           "postgresql"
   :username          "postgres"
   :password          "password"
   :database-name     "postgres"
   :server-name       "localhost"})

(defn run-persons-update []
  (with-open [reader (io/reader "local-setup/update-file.xml")]
    (-> reader
        (xml->persons)
        ;(->> (take 10000))                                  ; to allow shorter test runs
        (db/batch-execute-in-parallel {:batch-size        1000
                                       :number-of-executors 4}
                                      datasource-options
                                      db/insert-or-update-persons-statement))))

(defn run []
  (stats
    (take-time
      (run-persons-update))))

(comment
  (run))

(defn -main []
  (pprint (run)))
