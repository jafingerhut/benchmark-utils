(ns com.andyfingerhut.bench
  (:require [com.andyfingerhut.process :as p]))

(defn print-common-info []
  (println "pid:" (p/process-id))
  (println "Clojure version:" (clojure-version)))
