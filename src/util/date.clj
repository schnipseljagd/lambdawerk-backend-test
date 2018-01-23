(ns util.date
  (:require [clj-time.format :as f]
            [clj-time.core :as t]))

(defn parse-date [^String string]
  (try
    (f/parse (f/formatter :year-month-day) string)
    (catch RuntimeException e
      nil)))
