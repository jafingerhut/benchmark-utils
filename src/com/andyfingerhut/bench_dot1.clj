(ns com.andyfingerhut.bench-dot1
  (:require [criterium.core :as crit]))

(def n 100000)

(println "Clojure version:" (clojure-version))
(println "===== Clojure vectors of boxed doubles =====")
(def cvx (vec (map double (range n))))
(def cvy (vec (map double (range n))))
(println "(class cvx)=" (class cvx))
(println "dot product calculated by: (reduce + (map * cvx cvy))")
(println "dot product result:" (reduce + (map * cvx cvy)))
(crit/with-progress-reporting
  (crit/bench
   (reduce + (map * cvx cvy))
   :verbose))
