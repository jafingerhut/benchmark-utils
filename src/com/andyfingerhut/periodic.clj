(ns com.andyfingerhut.periodic)

(set! *warn-on-reflection* true)


(defn print-elapsed-time-every [period-msec]
  (let [start-time (System/nanoTime)]
    (loop []
      (Thread/sleep period-msec)
      (println "elapsed nsec" (- (System/nanoTime) start-time))
      (flush)
      (recur))))
    

(defn elapsed-times [m]
  (let [period-msec (:period-msec m)]
    (print-elapsed-time-every period-msec)))
