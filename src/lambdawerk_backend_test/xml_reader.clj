(ns lambdawerk-backend-test.xml-reader
  (:require [clojure.data.xml :as xml])
  (:import (clojure.data.xml Element)))

(defn parse [source]
  (xml/parse source))

(defn get-members [^Element {:keys [tag content]}]
  {:pre [(= :members tag)]}
  content)

(defn member->map [^Element {:keys [tag content]}]
  {:pre [(= :member tag)]}
  (letfn [(element->key-value [^Element {:keys [tag content]}]
            {:pre [(= 1 (count content))]}
            [tag (first content)])]
    (->> content
         (mapcat element->key-value)
         (apply hash-map))))
