(ns util.measure)

(defmacro take-time
  "Evaluates expr and returns a tuple with the measured time and the value of the expression.
   Measured time is in msecs."
  {:added "1.0"}
  [expr]
  `(let [start# (. System (nanoTime))
         ret# ~expr]
     [(/ (double (- (. System (nanoTime)) start#)) 1000000.0)
      ret#]))
