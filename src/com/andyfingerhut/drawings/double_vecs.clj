(ns com.andyfingerhut.drawings.double-vecs
  (:require [cljol.dig9 :as d]
            [uncomplicate.neanderthal.native :as nn]
            [ubergraph.core :as uber]))

(set! *warn-on-reflection* true)

(def n 100)

(def opts {})

(defn graph-with-and-wo-doubles [obj-coll base-fname opts]
  (let [fname  (str base-fname ".dot")
        fname2 (str base-fname "-no-doubles.dot")
        g (d/sum obj-coll opts)
        g2 (uber/remove-nodes*
	     g (filter (fn [n] (instance? Double (uber/attr g n :obj)))
                       (uber/nodes g)))]
    (d/view-graph g  {:save {:filename fname  :format :dot}})
    (d/view-graph g2 {:save {:filename fname2 :format :dot}})))

(defn clojure-persistent-vector [_]
  (println "===== Clojure vectors of boxed doubles =====")
  (let [cvx (vec (map double (range n)))]
    (graph-with-and-wo-doubles
     [cvx] (str "clojure-persistent-vector-" n "-boxed-doubles") opts)))

(defn clojure-gvec-primitive-doubles [_]
  (println "===== Clojure persistent vectors of primitive doubles =====")
  (let [cvx (apply vector-of :double (map double (range n)))]
    (graph-with-and-wo-doubles
     [cvx] (str "clojure-gvec-" n "-primitive-doubles") opts)))

(defn java-array-boxed-doubles [_]
  (println "===== Java arrays of boxed doubles, compute using areduce =====")
  (let [cax (object-array (map double (range n)))]
    (graph-with-and-wo-doubles
     [cax] (str "java-array-" n "-boxed-doubles") opts)))

(defn java-array-primitive-doubles [_]
  (println "===== Java arrays of primitive doubles =====")
  (let [cax (double-array (range n))]
    (graph-with-and-wo-doubles
     [cax] (str "java-array-" n "-primitive-doubles") opts)))

(defn neanderthal-double-vector [_]
  (println "===== Neanderthal double vector =====")
  (let [nx (nn/dv (range n))]
    (graph-with-and-wo-doubles
     [nx] (str "neanderthal-double-vector-" n) opts)))

(defn neanderthal-2-double-vectors [_]
  (println "===== Neanderthal 2 double vectors (to see if they share any objects) =====")
  (let [nx (nn/dv (range n))
        ny (nn/dv (range n))]
    (graph-with-and-wo-doubles
     [nx ny] (str "neanderthal-2-double-vector-" n) opts)))

(defn draw-all [m]
  (clojure-persistent-vector m)
  (clojure-gvec-primitive-doubles m)
  (java-array-boxed-doubles m)
  (java-array-primitive-doubles m)
  (neanderthal-double-vector m)
  (neanderthal-2-double-vectors m)
  (shutdown-agents))
