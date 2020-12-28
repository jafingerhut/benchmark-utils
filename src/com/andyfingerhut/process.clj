(ns com.andyfingerhut.process)

(def jdk9-or-later
  (try
    (if (Class/forName "java.lang.ProcessHandle")
      true)
    (catch ClassNotFoundException e
      false)))

(defn process-id-some-linux-like-systems
  "This seems to work on some Linux and OS X / macOS systems running
  JDK 8."
  []
  (let [s (. (java.lang.management.ManagementFactory/getRuntimeMXBean)
             (getName))
        ;;_ (println (format "some-linux-like-systems s='%s'" s))
        m (re-find #"^(\d+)@" s)]
    (if m
      (Long/parseLong (nth m 1)))))

(if jdk9-or-later
  (let [pid (requiring-resolve 'com.andyfingerhut.process-jdk9/process-id)]
    (defn process-id
      "Return process id of running JVM procss."
      []
      (pid)))
  (defn process-id []
    (process-id-some-linux-like-systems)))


(comment

(require '[com.andyfingerhut.process :as p])
p/jdk9-or-later
(p/process-id-some-linux-like-systems)
(p/process-id)

)
