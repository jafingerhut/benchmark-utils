(ns com.andyfingerhut.benchmark-utils
  (:require [clojure.string :as str]))

(def dbgl (atom []))

(defn prepending-writer
  "Given a java.io.Writer `wrtr` that you want to write output to, but
  you want every line prepended with a string returned from a function
  `prefix-fn`, calling `prepending-writer` with those arguments will
  return another java.io.Writer that you make normal method calls on,
  and it will call `prefix-fn` just before the first character is
  written to each line (that first cahracter of the line could be a
  newline, if the line contains no characters before the newline is
  printed).

  One way to use this to prefix a constant string \"my-prefix: \" to
  each line printed to the Clojure standard output writer `*out*` is
  as follows:

  (defn my-prefix-fn []
    \"my-prefix: \")

  (let [orig-out *out*]
    (binding [*out* (prepending-writer orig-out my-prefix-fn)]
      ;; Code here that writes to *out*, e.g. calls to Clojure's
      ;; functions `print` and/or `println`.
      ))

  `prefix-fn` is a function so that you can choose to return different
  values on each call, e.g. a line number stored in a Clojure atom
  with a new incremented value returned on each call, or a string
  containing the current time.

  No check is made whether the string returned by `prefix-fn` itself
  contains newlines.  If it does, no attempt is made to recognize
  this, or print a prefix before those lines.  In addition, no check
  is made to ensure that the return value is a string."
  [wrtr prefix-fn]
  (let [at-line-begin (atom true)]
    (proxy [java.io.BufferedWriter] [wrtr]
      (close []
        ;;(swap! dbgl conj {:call :close})
        (. wrtr (close)))
      (newLine []
        ;;(swap! dbgl conj {:call :newLine})
        (if @at-line-begin
          (. wrtr (write (prefix-fn))))
        (. wrtr (newLine))
        (reset! at-line-begin true))
      (flush []
        ;;(swap! dbgl conj {:call :flush})
        (. wrtr (flush)))
      (write
        ([arg]
         ;; The one arg could be a string, or a single primitive Java
         ;; char, or an int containing the code point of a character
         ;; to write.
         ;;(swap! dbgl conj {:call :write-1 :args [arg]})
         ;; If called with empty string, do nothing and leave
         ;; at-line-begin value unchanged.
         (when (not= arg "")
           (if @at-line-begin
             (. wrtr (write (prefix-fn))))
           (if-not (string? arg)
             (. wrtr (write arg))
             ;; Try to handle all possible strings, no matter how many
             ;; embedded newlines they may contain somewhere in them.
             (let [lines-seq (re-seq #"[^\n]*\r?\n" arg)
                   total-len (reduce + (map count lines-seq))
                   lines-seq (if (< total-len (count arg))
                               (concat lines-seq [(subs arg total-len)])
                               lines-seq)]
               (. wrtr (write (first lines-seq)))
               (doseq [line (rest lines-seq)]
                 (. wrtr (write (prefix-fn)))
                 (. wrtr (write line)))))
           (if (or (and (string? arg) (str/ends-with? arg "\n"))
                   (and (char? arg) (= arg \newline))
                   (and (integer? arg) (= arg (int \newline))))
             (reset! at-line-begin true)
             (reset! at-line-begin false))))
        ([cbuf-or-str off len]
         ;;(swap! dbgl conj {:call :write-3 :args [cbuf-or-str off len]})
         (. wrtr (write cbuf-or-str off len)))))))



(def cur-line-number (atom 0))

(reset! cur-line-number 0)

(defn line-number-prefix []
  (let [n (swap! cur-line-number inc)]
    (str n " ")))


(defn set-jvm-default-timezone-to-utc []
  (java.util.TimeZone/setDefault (java.util.TimeZone/getTimeZone "UTC")))

(defn date-time-str []
  (let [now (java.util.Date.)
        arr (object-array [now])
        nsec (String/format "%1$tN" arr)]
    ;; Return microsec resolution string, to match the resolution
    ;; produced by the `ts` command.
    (str (String/format "%1$tZ %1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS." arr)
         (subs nsec 0 6))))


(comment

(require '[com.andyfingerhut.benchmark-utils :as bu])
(in-ns 'com.andyfingerhut.benchmark-utils)
  
(reset! dbgl [])
(count @dbgl)
(pprint @dbgl)
(class (first (:args (nth @dbgl 0))))
(pprint (take-last 20 @dbgl))

(date-time-str)

(clojure.pprint/pprint (sort (seq (java.util.TimeZone/getAvailableIDs))))
(set-jvm-default-timezone-to-utc)
;; set the default time zone of the JVM to be UTC
(java.util.TimeZone/getDefault)

(defn date-time-plus-label []
  (str (date-time-str) " bench: "))

(let [orig-out *out*]
  (binding [*out* (prepending-writer orig-out date-time-plus-label)]
    (println "foo")
    (print "baz\nguh")
    (println "bar")
    (print "")
    (print "blue")))

(require '[criterium.core :as crit])
(def v1000 (vec (range 1000)))

(let [orig-out *out*]
  (binding [*out* (prepending-writer orig-out date-time-plus-label)]
    (crit/with-progress-reporting
      (crit/quick-bench
       (reduce + v1000)))))

(let [orig-out *out*]
  (binding [*out* (prepending-writer orig-out date-time-plus-label)]
    (crit/with-progress-reporting
      (crit/bench
       (reduce + v1000)))))

)
