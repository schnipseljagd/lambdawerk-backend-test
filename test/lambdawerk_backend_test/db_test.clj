(ns lambdawerk-backend-test.db-test
  (:require [clojure.test :refer :all]
            [lambdawerk-backend-test.db :refer :all]
            [util.date :refer [parse-date]]))

(def simple-statement
  "INSERT INTO person (phone, fname, lname, dob) VALUES (?, ?, ?, ?) ON CONFLICT (fname, lname, dob) DO UPDATE SET phone = EXCLUDED.phone WHERE person.phone <> EXCLUDED.phone")

(def multi-insert-statement
  "INSERT INTO person (phone, fname, lname, dob) VALUES (?, ?, ?, ?), (?, ?, ?, ?) ON CONFLICT (fname, lname, dob) DO UPDATE SET phone = EXCLUDED.phone WHERE person.phone <> EXCLUDED.phone")

(def additional-field-statement
  "INSERT INTO person (phone, foo, fname, lname, dob) VALUES (?, ?, ?, ?, ?) ON CONFLICT (fname, lname, dob) DO UPDATE SET phone = EXCLUDED.phone, foo = EXCLUDED.foo WHERE person.phone <> EXCLUDED.phone")

(defn make-example-person []
  {:firstname     "foo"
   :lastname      "bar"
   :date-of-birth (parse-date "1987-11-29")
   :phone         "123456789"})

(deftest generates-simple-statement
  (let [sql (-> [(make-example-person)]
                (insert-or-update-persons-statement)
                (first))]
    (is (= simple-statement sql))))

(deftest generates-multi-insert-statement
  (let [sql (-> [(make-example-person)
                 (make-example-person)]
                (insert-or-update-persons-statement)
                (first))]
    (is (= multi-insert-statement sql))))

(deftest generates-statement-with-additional-field
  (let [sql (-> [(-> (make-example-person)
                     (assoc :foo "lalalal"))]
                (insert-or-update-persons-statement)
                (first))]
    (is (= additional-field-statement sql))))
