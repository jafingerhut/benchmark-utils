(ns com.andyfingerhut.bench.double-vec-100k-dot-product
  (:require [criterium.core :as crit]
            [uncomplicate.fluokitten.core :as fk]
            [uncomplicate.neanderthal.native :as nn]
            [uncomplicate.neanderthal.core :as nc]
            [com.andyfingerhut.bench :as b]))

(set! *warn-on-reflection* true)

(def n 100000)

(defn clojure-persistent-vector [_]
  (b/print-common-info)
  (println "===== Clojure vectors of boxed doubles =====")
  (let [cvx (vec (map double (range n)))
        cvy (vec (map double (range n)))]
    (println "(class cvx)=" (class cvx))
    (println "dot product calculated by: (reduce + (map * cvx cvy))")
    (println "dot product result:"
             (reduce + (map * cvx cvy)))
    (crit/with-progress-reporting
      (crit/bench
       (reduce + (map * cvx cvy))
       :verbose))))

(defn clojure-gvec-primitive-doubles [_]
  (b/print-common-info)
  (println "===== Clojure persistent vectors of primitive doubles =====")
  (let [cvx (apply vector-of :double (map double (range n)))
        cvy (apply vector-of :double (map double (range n)))]
    (println "(class cvx)=" (class cvx))
    (println "dot product calculated by: (reduce + (map * cvx cvy))")
    (println "dot product result:"
             (reduce + (map * cvx cvy)))
    (crit/with-progress-reporting
      (crit/bench
       (reduce + (map * cvx cvy))
       :verbose))))

(defn java-array-boxed-doubles [_]
  (b/print-common-info)
  (println "===== Java arrays of boxed doubles, compute using areduce =====")
  (let [cax (object-array (map double (range n)))
        cay (object-array (map double (range n)))]
    (println "(class cax)=" (class cax))
    (println "dot product calculated by: (areduce ...)")
    (println "dot product result:"
             (areduce ^objects cax i ret 0.0
                      (+ ret (* (aget ^objects cax i)
                                (aget ^objects cay i)))))
    (crit/with-progress-reporting
      (crit/bench
       (areduce ^objects cax i ret 0.0
                (+ ret (* (aget ^objects cax i)
                          (aget ^objects cay i))))
       :verbose))))

(defn java-array-primitive-doubles-areduce [_]
  (b/print-common-info)
  (println "===== Java arrays of primitive doubles, compute using areduce =====")
  (let [cax (double-array (range n))
        cay (double-array (range n))]
    (println "(class cax)=" (class cax))
    (println "dot product calculated by: (areduce ...)")
    (println "dot product result:"
             (areduce ^doubles cax i ret 0.0
                      (+ ret (* (aget ^doubles cax i)
                                (aget ^doubles cay i)))))
    (crit/with-progress-reporting
      (crit/bench
       (areduce ^doubles cax i ret 0.0
                (+ ret (* (aget ^doubles cax i)
                          (aget ^doubles cay i))))
       :verbose))))

(defn p+ ^double [^double x ^double y]
  (+ x y))

(defn p* ^double [^double x ^double y]
  (* x y))

(defn java-array-primitive-doubles-fluokitten-foldmap [_]
  (b/print-common-info)
  (println "===== Java arrays of primitive doubles, compute using fluokitten foldmap =====")
  (let [cax (double-array (range n))
        cay (double-array (range n))]
    (println "(class cax)=" (class cax))
    (println "dot product calculated by: (foldmap p+ 0.0 p* cax cay)")
    (println "dot product result:"
             (fk/foldmap p+ 0.0 p* cax cay))
    (crit/with-progress-reporting
      (crit/bench
       (fk/foldmap p+ 0.0 p* cax cay)
       :verbose))))

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
       :verbose))))

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
       :verbose))))
