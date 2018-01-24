(ns lambdawerk-backend-test.core
  (:gen-class)
  (:require [clojure.java.io :as io]
            [lambdawerk-backend-test.service :refer [xml->persons]]
            [lambdawerk-backend-test.db :as db]))

(defn run-persons-update []
  (with-open [reader (io/reader "local-setup/update-file.xml")]
    (-> reader
        (xml->persons)
        ;(->> (take 10000))                                  ; to allow shorter test runs
        (db/update-persons-table {:batch-size          1000
                                  :number-of-executors 4}
                                 {:minimum-idle      1
                                  :maximum-pool-size 10
                                  :pool-name         "db-pool"
                                  :adapter           "postgresql"
                                  :username          "postgres"
                                  :password          "password"
                                  :database-name     "postgres"
                                  :server-name       "localhost"}))))

(comment
  (time (run-persons-update)))

(defn -main []
  (time (run-persons-update)))
