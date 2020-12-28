(ns com.andyfingerhut.process-jdk9)

(set! *warn-on-reflection* true)

(defn process-id []
  (. (java.lang.ProcessHandle/current) (pid)))
