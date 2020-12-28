(ns com.andyfingerhut.bench-dot1
  (:require [criterium.core :as crit]
            [com.andyfingerhut.bench :as b]))

(b/print-common-info)
(println "===== Clojure vectors of boxed doubles =====")
(def n 100000)
(def cvx (vec (map double (range n))))
(def cvy (vec (map double (range n))))
(println "(class cvx)=" (class cvx))
(println "dot product calculated by: (reduce + (map * cvx cvy))")
(println "dot product result:" (reduce + (map * cvx cvy)))
(crit/with-progress-reporting
  (crit/bench
   (reduce + (map * cvx cvy))
   :verbose))
