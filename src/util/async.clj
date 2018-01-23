(ns util.async
  (:import (java.util.concurrent Executors ExecutorService)))

(defn run-in-parallel [data function number-of-executors]
  (let [service (Executors/newFixedThreadPool number-of-executors)
        submit (fn [function data] (.submit ^ExecutorService service
                                            ^Callable (fn [] (function data))))
        result (->> data
                    (map (partial submit function))
                    (doall)
                    (map deref)
                    (doall))]
    (.shutdown service)
    result))
