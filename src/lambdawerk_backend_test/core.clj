(ns lambdawerk-backend-test.core
  (:require [clojure.java.io :as io]
            [lambdawerk-backend-test.service :refer [xml->persons]]
            [lambdawerk-backend-test.db :as db]))

; ALTER TABLE person ADD UNIQUE (fname,lname,dob);
; insert into person (fname,lname,dob) values ('DENAIJAHA', 'VILLOLOBOS', '1982-07-03') on conflict (fname,lname,dob) do update set (phone) = ('6607186022') where person.phone != '6607186022';

; Are there other clients running writes/reads on the database in production?
; Is the production database already under load?
; Should it be configurable how much load the update task generates on the production database?
; What is more important a fast import or being a good neighbour on the production database (if there are other clients)?

(defn run-persons-update []
  (with-open [reader (io/reader "local-setup/update-file.xml")]
    (-> reader
        (xml->persons)
        (->> (take 10000))
        (db/update-persons-table {:transaction-chunk-size 10000
                                  :number-of-executors    4}
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
