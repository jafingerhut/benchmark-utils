(ns com.andyfingerhut.bench.double-vec-100k-dot-product-set2
  (:require [criterium.core :as crit]
            [uncomplicate.fluokitten.core :as fk]
            [uncomplicate.neanderthal.native :as nn]
            [uncomplicate.neanderthal.core :as nc]
            [com.andyfingerhut.bench :as b]))

;; At least some of the benchmark in this namespace require
;; Neanderthal, and the point of my benchmarking Neanderthal is to see
;; how fast it can be with the Intel Math Kernel Library in use.  So
;; far I have not been able to get this library working from Clojure
;; on macOS, so I would like to keep these tests separate from the
;; ones in namespace
;; com.andyfingerhut.bench.double-vec-100k-dot-product

(set! *warn-on-reflection* true)

(def n 100000)

(defn neanderthal-double-vectors-fluokitten-foldmap [_]
  (b/print-common-info)
  (println "===== Neanderthal double vectors, compute using fluokitten foldmap =====")
  (let [nx (nn/dv (range n))
        ny (nn/dv (range n))]
    (println "(class nx)=" (class nx))
    (println "dot product calculated by: (foldmap p+ 0.0 p* nx ny)")
    (println "dot product result:"
             (fk/foldmap p+ 0.0 p* nx ny))
    (crit/with-progress-reporting
      (crit/bench
       (fk/foldmap p+ 0.0 p* nx ny)
       :verbose)))
  (shutdown-agents))

(defn neanderthal-double-vectors-mkl-dot [_]
  (b/print-common-info)
  (println "===== Neanderthal double vectors, compute using Intel MKL dot =====")
  (let [nx (nn/dv (range n))
        ny (nn/dv (range n))]
    (println "(class nx)=" (class nx))
    (println "dot product calculated by: (nc/dot nx ny)")
    (println "dot product result:"
             (nc/dot nx ny))
    (crit/with-progress-reporting
      (crit/bench
       (nc/dot nx ny)
       :verbose)))
  (shutdown-agents))
