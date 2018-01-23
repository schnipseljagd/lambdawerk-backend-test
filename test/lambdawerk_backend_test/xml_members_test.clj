(ns lambdawerk-backend-test.xml-members-test
  (:require [clojure.test :refer :all]
            [clojure.data.xml :refer [element]]
            [lambdawerk-backend-test.xml-members :refer :all]))

(defn member-field-element [key & values]
  (apply element key {} values))

(defn member-element [fields]
  (apply element :member {} fields))

(defn members-element [members]
  (apply element :members {} members))

(deftest get-all-returns-a-list-of-members
  (let [members (list (member-element ())
                      (member-element ()))]
    (is (= members (get-all (members-element members))))))

(deftest get-all-expects-a-members-element
  (is (thrown? AssertionError (get-all (element :foo {} ())))))

(deftest member->map-returns-the-fields
  (is (= {:foo  "bar"
          :lala "haha"} (-> (list (member-field-element :foo "bar")
                                  (member-field-element :lala "haha"))
                            (member-element)
                            (member->map)))))

(deftest member->map-expects-a-member-element
  (is (thrown? AssertionError (member->map (element :foo {} ())))))

(deftest member->map-expects-fields-with-one-value
  (is (thrown? AssertionError (-> (list (member-field-element :foo "foo" "bar"))
                                  (member-element)
                                  (member->map)))))
