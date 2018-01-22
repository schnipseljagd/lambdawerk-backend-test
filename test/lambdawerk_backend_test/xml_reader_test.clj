(ns lambdawerk-backend-test.xml-reader-test
  (:require [clojure.test :refer :all]
            [clojure.data.xml :refer [element]]
            [lambdawerk-backend-test.xml-reader :refer :all]))

(defn member-field-element [key & values]
  (apply element key {} values))

(defn member-element [fields]
  (apply element :member {} fields))

(defn members-element [members]
  (apply element :members {} members))

(deftest returns-a-list-of-member-elements
  (let [members (list (member-element ())
                      (member-element ()))]
    (is (= members (get-members (members-element members))))))

(deftest expects-a-members-element
  (is (thrown? AssertionError (get-members (element :foo {} ())))))

(deftest converts-member-fields-into-map
  (is (= {:foo  "bar"
          :lala "haha"} (-> (list (member-field-element :foo "bar")
                                  (member-field-element :lala "haha"))
                            (member-element)
                            (member->map)))))

(deftest expects-a-member-element
  (is (thrown? AssertionError (member->map (element :foo {} ())))))

(deftest expects-member-fields-to-have-exactly-one-value
  (is (thrown? AssertionError (-> (list (member-field-element :foo "foo" "bar"))
                                  (member-element)
                                  (member->map)))))
