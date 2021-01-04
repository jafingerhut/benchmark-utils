(ns com.andyfingerhut.bench.analyze.results
  (:require [clojure.string :as str]
            [clojure.pprint :as pp]
            [clojure.java.io :as io]
            [com.andyfingerhut.bench.analyze.utils :as u]))


(defn lines->linemaps
  "Given a sequence of strings representing consecutive lines of some
  file, return a sequence of maps describing those lines with
  keys :linenum and :line, where values of :line are the strings in
  the input sequence, and values of :linenum are integer numbers of
  the lines, counting from 1 and increasing."
  [lines]
  (map-indexed (fn [idx line]
                 {:linenum (inc idx),
                  :line line})
               lines))

(comment

(pprint (lines->linemaps ["a" "b" "c"]))
;; ({:linenum 1, :line "a"}
;;  {:linenum 2, :line "b"}
;;  {:linenum 3, :line "c"})

)
  
  
(defn line-timestamp
  "Given a string, return a map describing its contents.
  If the string begins with the following pattern (where UTC must be
  exact match, not an arbitrary 3 characters, but all decimal digits
  may be any digits:

  UTC 2020-12-29 19:28:05.359187

  then the returned map has a key :timestamp-str equal to that string,
  without the \"UTC \" prefix, and a key :rest-of-str equal to the
  rest of the string after that.
  
  If the beginning of line does not match that pattern, then the
  returned map has only the key :rest-of-str equal to the input
  string, and no key :timestamp-str"
  [line-str]
  (let [m (re-matches #"UTC (\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}\.\d{6})(.*)"
                      line-str)]
    (if m
      {:timestamp-str (m 1), :rest-of-str (m 2)}
      {:rest-of-str line-str})))

(comment

(def s1 "UTC 2020-12-29 19:28:05.359187 log: openjdk version \"11.0.9.1\" 2020-11-04")
(def s1 "UTC 2020-12-29 19:28:05. log: openjdk version \"11.0.9.1\" 2020-11-04")
(def s1 "UTC 2020-12-29 19:28:05:359187 log: openjdk version \"11.0.9.1\" 2020-11-04")
(line-timestamp s1)

)


(def date-time-formatter (java.time.format.DateTimeFormatter/ofPattern
                          "yyyy-MM-dd HH:mm:ss.SSSSSS"))


(defn timestr->datetime [timestamp-str]
  (try
    (java.time.LocalDateTime/parse timestamp-str date-time-formatter)
    (catch java.time.format.DateTimeParseException e
      e)))

(comment

(def s1 "2020-12-29 19:28:05.359187")
(def s1 "2020-13-29 19:28:05.359187")
(def s1 "2020-12-32 19:28:05.359187")
(def s1 "2020-12-29 24:28:05.359187")
(def s1 "2020-12-29 19:60:05.359187")
(def s1 "2020-12-29 19:28:60.359187")
(def x (timestr->datetime s1))
x
(class x)
(instance? Exception x)

)


(def line-origins
  [{:prefix "top: ", :origin :top}
   {:prefix " top: ", :origin :top}])

(defn line-origin [s]
  (some (fn [{:keys [prefix origin]}]
          (if (str/starts-with? s prefix)
            {:origin origin,
             :rest-of-str (subs s (count prefix))}))
        line-origins))

(comment

(def s1 "top: foo")
(def s1 " top: foo")
(def s1 "  top: foo")
(line-origin s1)
  
)

(defn parse-and-check-timestamps
  "Given a sequence of lines, possibly read from a file, attempt to
  parse a date and time string at the beginning of every line.

  If any problems are found, then a map is returned with at least
  these keys:

  :error - a keyword describing what kind of problem was found.

  :error-data - some data structure, which can differ from one value
  of :error to another, giving more detail about what line or lines
  the error was found in.

  If no problems are found, then return a sequence of maps, one
  describing each line's contents, each having these keys:

  :line - a string containing the original input line.

  :linenum - integer line numbers, starting with the first being line
  number 1.

  :timestamp-str - the beginning part of the input line that is the
  time and date representation.

  :localdatetime - the string in :timestamp-str parsed as a date and
  time, and returned as a java.time LocalDateTime object.

  :origin - the origin of the log line, e.g. the top program, or a
  separate JVM process performing benchmarking.

  :usec - an integer number of microseconds of this line's timestamp,
  after the first line's timestamp.  This value is 0 if the two line's
  timestamps are the same, or negative if the later line's timestamp
  is earlier than the first line's timestamp.

  :rest-of-str - The rest of the input line after the timestamp and
  origin strings have been removed."
  [lines]
  (let [lms (->> lines
                 ;; seq of strings -> seq of maps with
                 ;; keys :line :linenum
                 lines->linemaps
                 ;; Add key :rest-of-str, and maybe :timestamp-str
                 (map (fn [m]
                        (merge m (line-timestamp (:line m))))))
        first-line-no-timestamp (first (filter #(nil? (:timestamp-str %)) lms))]
    (if first-line-no-timestamp
      {:error :line-without-timestamp
       :error-data first-line-no-timestamp}
      (let [lms (->> lms
                     ;; For maps with :timestamp-str a string of
                     ;; appropriate format, parse and convert it
                     ;; to :localdatetime
                     (map (fn [m]
                            (assoc m :localdatetime
                                   (timestr->datetime (:timestamp-str m)))))
                     ;; Remove prefix of :rest-of-str value that
                     ;; indicates which program generated it, adding a
                     ;; key :origin that records where the line came
                     ;; from.
                     (map (fn [m] (merge m (line-origin (:rest-of-str m))))))
            first-error-parsing-datetime
            (first (filter #(instance? Exception (:localdatetime %)) lms))]
        (if first-error-parsing-datetime
          {:error :error-parsing-timestamp
           :error-data first-error-parsing-datetime}
          (let [first-time (:localdatetime (first lms))
                ;; Add integer-valued :usec key to each map,
                ;; containing the number of microseconds after
                ;; first-time that the line's timestamp is.  This
                ;; value will be negative if its timestamp is before
                ;; first-time.
                lms (->> lms
                         (map (fn [m]
                                (assoc m :usec
                                       (. first-time (until
                                                      (:localdatetime m)
                                                      java.time.temporal.ChronoUnit/MICROS))))))
                ;; Find any pair of consecutive lines where the later
                ;; line has a _smaller_ timestamp than the earlier
                ;; line.  Two consecutive lines with identical
                ;; timestamps are OK.
                lms-pairs (partition 2 1 lms)
                descending-line-pair (first (filter #(> (:usec (first %))
                                                        (:usec (second %)))
                                                    lms-pairs))]
            (if descending-line-pair
              {:error :later-line-timestamp-earlier-than-previous-line
               :error-data descending-line-pair}
              lms)))))))

(comment

(def l1 ["UTC 2020-12-29 19:28:04.470236 top: top - 11:28:04 up"
         "UTC 2020-12-29 19:28:04.470351 top: Tasks: 215 total,"
         "UTC 2020-12-29 19:28:04.470365 top: %Cpu(s):  2.9 us,"])
;; no errors

(def l1 ["UTC 2020-12-29 19:28:04.470236 top: top - 11:28:04 up"
         "UTC 2020-12-29 19:28:04.470351 top: Tasks: 215 total,"
         "UTC 2020-12-29 19:28:04:470365 top: %Cpu(s):  2.9 us,"])
;; :error :line-without-timestamp on line 3

(def l1 ["UTC 2020-12-29 19:28:04.470236 top: top - 11:28:04 up"
         "UTC 2020-12-29 19:28:60.470351 top: Tasks: 215 total,"
         "UTC 2020-12-29 19:28:04.470365 top: %Cpu(s):  2.9 us,"])
;; :error :error-parsing-timestamp on line 2

(def l1 ["UTC 2020-12-29 19:28:04.470236 top: top - 11:28:04 up"
         "UTC 2020-12-29 19:28:04.470351 top: Tasks: 215 total,"
         "UTC 2020-12-29 19:28:04.470350 top: %Cpu(s):  2.9 us,"])
;; :error :later-line-timestamp-earlier-than-previous-line between lines 2 and 3

(def lms (parse-and-check-timestamps l1))
(pprint lms)

)

(defn top-first-output-line? [s]
  (boolean
   (re-find #"^\s*top\s+-\s+\S+\s+up\s+.+\s+\d+\s+user,\s+load average:" s)))

(comment
(def l1 " top - 11:28:06 up  5:27,  1 user,  load average: 0.80, 0.99, 0.69")
(def l1 " top - 20:14:33 up 4 min,  1 user,  load average: 1.40, 1.06, 0.47")
(def l1 " top - 09:40:33 up 1 day, 28 min,  1 user,  load average: 0.16, 0.32, 0.37")
(top-first-output-line? l1)
)


;; Source: Ubuntu 18.04.5 Linux man page for top
;; Default CPU state percentages shown in top output:

;; us - time running un-niced user processes
;; sy - time running kernel processes
;; ni - time running niced user processes
;; id - time spent in the kernel idle handler
;; wa - time waiting for I/O completion
;; hi - time spent servicing hardware interrupts
;; si - time spent servicing software interrupts
;; st - time stolen from this vm by the hypervisor

(defn parse-top-total-cpu-use-line [s]
  (let [m (re-find #"^\s*%Cpu\(s\):\s*(\d+\.\d+)\s+us,\s+(\d+\.\d+)\s+sy,\s+(\d+\.\d+)\s+ni,\s+(\d+\.\d+)\s+id,\s+(\d+\.\d+)\s+wa,\s+(\d+\.\d+)\s+hi,\s+(\d+\.\d+)\s+si,\s+(\d+\.\d+)\s+st\s*$"
                   s)]
    (if m
      {:user-cpu-percent (Double/parseDouble (m 1))
       :kernel-cpu-percent (Double/parseDouble (m 2))
       :niced-process-cpu-percent (Double/parseDouble (m 3))
       :idle-cpu-percent (Double/parseDouble (m 4))
       :waiting-io-cpu-percent (Double/parseDouble (m 5))
       :service-hardware-interrupt-cpu-percent (Double/parseDouble (m 6))
       :service-software-interrupt-cpu-percent (Double/parseDouble (m 7))
       :time-stolen-from-this-vm-by-hypervisor-cpu-percent (Double/parseDouble (m 8))})))

(comment
(def l1 "%Cpu(s): 20.7 us,  0.5 sy,  0.0 ni, 78.8 id,  0.0 wa,  0.0 hi,  0.0 si,  0.0 st")
(def l1 "%Cpu(s):100.0 us,  0.0 sy,  0.0 ni,  0.0 id,  0.0 wa,  0.0 hi,  0.0 si,  0.0 st")
(pp/pprint (parse-top-total-cpu-use-line l1))
)

(defn parse-top-total-mem-use-line [s]
  (let [m (re-find #"^\s*KiB Mem\s*:\s*(\d+)\s+total\s*,\s*(\d+)\s+free\s*,\s*(\d+)\s+used\s*,\s*(\d+)\s+buff/cache\s*$"
                   s)]
    (if m
      {:total-mem-kib (Long/parseLong (m 1))
       :free-mem-kib (Long/parseLong (m 2))
       :used-mem-kib (Long/parseLong (m 3))
       :cache-mem-kib (Long/parseLong (m 4))})))

(comment
(def l1 "KiB Mem :  4030264 total,   547848 free,   829628 used,  2652788 buff/cache")
(pprint (parse-top-total-mem-use-line l1))
)

;; Source: Ubuntu 18.04.5 Linux man page for top
;; Description of fields:

;; PID -- Process Id

;;   The task's unique process ID, which periodically wraps, though
;;   never restarting at zero.  In kernel terms, it is a dispatchable
;;   entity defined by a task_struct.
;;
;;   This value may also be used as: a process group ID (see PGRP); a
;;   session ID for the session leader (see SID); a thread group ID
;;   for the thread group leader (see TGID); and a TTY process group
;;   ID for the process group leader (see TPGID).

;; USER -- User Name

;;   The effective user name of the task's owner.

;; PR -- Priority

;;   The scheduling priority of the task.  If you see `rt' in this
;;   field, it means the task is running under real time scheduling
;;   priority.
;;
;;   Under linux, real time priority is somewhat misleading since
;;   traditionally the operating itself was not preemptible.  And
;;   while the 2.6 kernel can be made mostly preemptible, it is not
;;   always so.

;; NI -- Nice Value

;;   The nice value of the task.  A negative nice value means higher
;;   priority, whereas a positive nice value means lower priority.
;;   Zero in this field simply means priority will not be adjusted in
;;   determining a task's dispatch-ability.

;; VIRT -- Virtual Memory Size (KiB)

;;   The total amount of virtual memory used by the task.  It includes
;;   all code, data and shared libraries plus pages that have been
;;   swapped out and pages that have been mapped but not used.
;;
;;   See `OVERVIEW, Linux Memory Types' for additional details.

;; RES -- Resident Memory Size (KiB)

;;   A subset of the virtual address space (VIRT) representing the
;;   non-swapped physical memory a task is currently using.  It is
;;   also the sum of the RSan, RSfd and RSsh fields.
;;
;;   It can include private anonymous pages, private pages mapped to
;;   files (including program images and shared libraries) plus shared
;;   anonymous pages.  All such memory is backed by the swap file
;;   represented separately under SWAP.
;;
;;   Lastly, this field may also include shared file-backed pages
;;   which, when modified, act as a dedicated swap file and thus will
;;   never impact SWAP.
;;
;;   See `OVERVIEW, Linux Memory Types' for additional details.

;; SHR -- Shared Memory Size (KiB)

;;   A subset of resident memory (RES) that may be used by other
;;   processes.  It will include shared anonymous pages and shared
;;   file-backed pages.  It also includes private pages mapped to
;;   files representing program images and shared libraries.
;;
;;   See `OVERVIEW, Linux Memory Types' for additional details.

;; S -- Process Status

;;   The status of the task which can be one of:
;;       D = uninterruptible sleep
;;       R = running
;;       S = sleeping
;;       T = stopped by job control signal
;;       t = stopped by debugger during trace
;;       Z = zombie
;;
;;   Tasks shown as running should be more properly thought of as
;;   ready to run -- their task_struct is simply represented on the
;;   Linux run-queue.  Even without a true SMP machine, you may see
;;   numerous tasks in this state depending on top's delay interval
;;   and nice value.

;; %CPU - CPU Usage

;;   The task's share of the elapsed CPU time since the last screen
;;   update, expressed as a percentage of total CPU time.
;;
;;   In a true SMP environment, if a process is multi-threaded and top
;;   is not operating in Threads mode, amounts greater than 100% may
;;   be reported.  You toggle Threads mode with the `H' interactive
;;   command.
;;
;;   Also for multi-processor environments, if Irix mode is Off, top
;;   will operate in Solaris mode where a task's cpu usage will be
;;   divided by the total number of CPUs.  You toggle Irix/Solaris
;;   modes with the `I' interactive command.

;; %MEM -- Memory Usage (RES)

;;   A task's currently resident share of available physical memory.
;;
;;   See `OVERVIEW, Linux Memory Types' for additional details.

;; TIME+ -- CPU Time, hundredths

;;   The same as TIME, but reflecting more granularity through
;;   hundredths of a second.

;; COMMAND -- Command Name or Command Line

;;   Display the command line used to start a task or the name of the
;;   associated program.  You toggle between command line and name
;;   with `c', which is both a command-line option and an interactive
;;   command.
;;
;;   When you've chosen to display command lines, processes without a
;;   command line (like kernel threads) will be shown with only the
;;   program name in brackets, as in this example:
;;       [kthreadd]
;;
;;   This field may also be impacted by the forest view display mode.
;;   See the `V' interactive command for additional information
;;   regarding that mode.
;;
;;   Note: The COMMAND field, unlike most columns, is not fixed-width.
;;   When displayed, it plus any other variable width columns will be
;;   allocated all remaining screen width (up to the maximum 512
;;   characters).  Even so, such variable width fields could still
;;   suffer truncation.  This is especially true for this field when
;;   command lines are being displayed (the `c' interactive command.)
;;   See topic 5c. SCROLLING a Window for additional information on
;;   accessing any truncated data.

(defn top-column-heading-line? [s]
  (re-find #"^\s*PID\s+USER\s+PR\s+NI\s+VIRT\s+RES\s+SHR\s+S\s+%CPU\s+%MEM\s+TIME\+\s+COMMAND\s*$"
           s))

(defn parse-top-process-line [s]
  (let [m (re-find #"^\s*(\d+)\s+(\S+)\s+(-?\d+|rt)\s+(-?\d+)\s+(\d+)\s+(\d+)\s+(\d+)\s+(\S+)\s+(\d+\.\d+)\s+(\d+\.\d+)\s+(\S+)\s+(\S(.*\S))\s*$"
                   s)]
    (if m
      {:pid (Long/parseLong (m 1))
       :user (m 2)
       :priority (let [pri (m 3)]
                   (if (= pri "rt")
                     :real-time
                     (Long/parseLong pri)))
       :nice (Long/parseLong (m 4))
       :virtual-memory-size-kib (Long/parseLong (m 5))
       :resident-memory-size-kib (Long/parseLong (m 6))
       :shared-memory-size-kib (Long/parseLong (m 7))
       :process-status (m 8)
       :cpu-usage-percent (Double/parseDouble (m 9))
       :memory-usage-percent (Double/parseDouble (m 10))
       :cpu-time (m 11)
       :command (m 12)})))

(comment
(def l1 " 26 root       0 -20       0      0      0 I   0.0  0.0   0:00.00 kworker/2:+")
(def l1 " 28 root     -51   0       0      0      0 S   0.0  0.0   0:00.00 idle_injec+")
(def l1 " 12 root      rt   0       0      0      0 S   0.0  0.0   0:00.35 migration/0")
(pprint (parse-top-process-line l1))
)


(defn parse-one-top-output
  "Given a sequence of maps, each describing one line of output from
  the Ubuntu 18.04 Linux top command with default parameters, where
  the key :rest-of-str contains strings that are the output from the
  top command, and :usec contains integer representing the relative
  time when the output line was printed, parse and extract out several
  parts of this output, returning a map containing the following keys:

  :usec - Time of the first line of output.

  :top-heading-lines - sequence of line maps for all of the lines up
  to and including the one that contains headings line that begins \"
  PID USER \"

  :top-process-lines - sequence of line maps for all other lines, one
  for each that contains statistics about one process.

  :total-cpu-use-info - a map as returned by function
  parse-top-total-cpu-use-line, the result of being called on the one
  heading line that should recognize and parse it.

  :total-mem-use-info - a map as returned by function
  parse-top-total-mem-use-line, the result of being called on the one
  heading line that should recognize and parse it.

  :pid->proc - a map from integer process id values to maps containing
  info about the process with that process id.  Each such map contains
  these keys:

    :line-info - The map in the input `linemaps` sequence from which
    the process information was parsed and extracted.

    :process-info - A map returned from function
    parse-top-process-line.  See its documentation for details."
  [linemaps]
  (let [[up-to-hdr after-hdr]
        (u/split-until #(top-column-heading-line? (:rest-of-str %))
                       linemaps)
        total-cpu-use-info (some #(parse-top-total-cpu-use-line
                                   (:rest-of-str %))
                                 up-to-hdr)
        total-mem-use-info (some #(parse-top-total-mem-use-line
                                   (:rest-of-str %))
                                 up-to-hdr)
        after-hdr (remove (fn [lm] (str/blank? (:rest-of-str lm))) after-hdr)
        proc-infos (mapv (fn [lm]
                           {:line-info lm
                            :process-info
                            (parse-top-process-line (:rest-of-str lm))})
                         after-hdr)
        first-nil-proc-info (first (filter #(nil? (:process-info %))
                                           proc-infos))
        pid->proc (group-by #(get-in % [:process-info :pid]) proc-infos)
        first-duplicate-pid (first (filter #(> (count (val %)) 1) pid->proc))
        tmp (map :process-info proc-infos)
        ;; Extra debug output if there is a line that has
        ;; no :resident-memory-size-kib key, to more quickly isolate
        ;; the problem.
        _ (when-not (every? #(contains? % :resident-memory-size-kib) tmp)
            (pp/pprint
             (first (remove (fn [pi]
                              (contains? (get pi :process-info)
                                         :resident-memory-size-kib))
                            proc-infos))))
        total-resident-memory-size-kib (reduce + (map :resident-memory-size-kib
                                                      tmp))
        total-shared-memory-size-kib (reduce + (map :shared-memory-size-kib
                                                    tmp))
        total-cpu-usage-percent (reduce + (map :cpu-usage-percent
                                               tmp))
        total-memory-usage-percent (reduce + (map :memory-usage-percent
                                                  tmp))]
    (cond
      (not (top-column-heading-line? (:rest-of-str (last up-to-hdr))))
      {:error :no-column-heading-line,
       :error-data linemaps}

      (nil? total-cpu-use-info)
      {:error :no-total-cpu-use-line,
       :error-data up-to-hdr}

      (nil? total-mem-use-info)
      {:error :no-total-mem-use-line,
       :error-data up-to-hdr}

      first-nil-proc-info
      {:error :unrecognized-process-info-line
       :error-data first-nil-proc-info}

      first-duplicate-pid
      {:error :multiple-processes-with-same-pid,
       :error-data (val first-duplicate-pid)}
      
      :else
      {:top-heading-lines up-to-hdr,
       :top-process-lines after-hdr,
       :usec (:usec (first linemaps))
       :total-cpu-use-info total-cpu-use-info
       :total-mem-use-info total-mem-use-info
       :total-resident-memory-size-kib total-resident-memory-size-kib
       :total-shared-memory-size-kib total-shared-memory-size-kib
       :total-cpu-usage-percent total-cpu-usage-percent
       :total-memory-usage-percent total-memory-usage-percent
       :pid->proc (into {} (for [[k v] pid->proc]
                             [k (first v)]))})))

(comment

(def lms
  [{:usec 0,
    :rest-of-str "top - 11:28:04 up  5:27,  1 user,  load average: 0.80, 0.99, 0.69"}
   {:rest-of-str "Tasks: 215 total,   1 running, 167 sleeping,   0 stopped,   0 zombie"}
   {:rest-of-str "%Cpu(s):  2.9 us,  0.2 sy,  0.0 ni, 96.9 id,  0.0 wa,  0.0 hi,  0.0 si,  0.0 st"}
   {:rest-of-str "KiB Mem :  4030264 total,   547848 free,   829628 used,  2652788 buff/cache"}
   {:rest-of-str "KiB Swap:  2097148 total,  2096624 free,      524 used.  2901400 avail Mem "}
   {:rest-of-str ""}
   {:rest-of-str "  PID USER      PR  NI    VIRT    RES    SHR S  %CPU %MEM     TIME+ COMMAND"}
   {:rest-of-str " 1568 andy      20   0 1067584 106848  57704 S   6.2  2.7   0:32.52 Xorg"}
   {:rest-of-str " 1803 andy      20   0 4152920 294476 114176 S   6.2  7.3   1:52.41 gnome-shell"}])

(def lms
  [{:usec 0,
    :rest-of-str "top - 11:28:04 up  5:27,  1 user,  load average: 0.80, 0.99, 0.69"}
   {:rest-of-str "Tasks: 215 total,   1 running, 167 sleeping,   0 stopped,   0 zombie"}
   {:rest-of-str "%Cpu(s):  2.9 us,  0.2 sy,  0.0 ni, 96.9 id,  0.0 wa,  0.0 hi,  0.0 si,  0.0 st"}
   {:rest-of-str "KiB Mem :  4030264 total,   547848 free,   829628 used,  2652788 buff/cache"}
   {:rest-of-str "KiB Swap:  2097148 total,  2096624 free,      524 used.  2901400 avail Mem "}
   {:rest-of-str ""}
   {:rest-of-str "  PID_ USER      PR  NI    VIRT    RES    SHR S  %CPU %MEM     TIME+ COMMAND"}
   {:rest-of-str " 1803 andy      20   0 4152920 294476 114176 S   6.2  7.3   1:52.41 gnome-shell"}])
;; {:error :no-column-heading-line, ...}

(def lms
  [{:usec 0,
    :rest-of-str "top - 11:28:04 up  5:27,  1 user,  load average: 0.80, 0.99, 0.69"}
   {:rest-of-str "Tasks: 215 total,   1 running, 167 sleeping,   0 stopped,   0 zombie"}
   ;;{:rest-of-str "%Cpu(s):  2.9 us,  0.2 sy,  0.0 ni, 96.9 id,  0.0 wa,  0.0 hi,  0.0 si,  0.0 st"}
   {:rest-of-str "KiB Mem :  4030264 total,   547848 free,   829628 used,  2652788 buff/cache"}
   {:rest-of-str "KiB Swap:  2097148 total,  2096624 free,      524 used.  2901400 avail Mem "}
   {:rest-of-str ""}
   {:rest-of-str "  PID USER      PR  NI    VIRT    RES    SHR S  %CPU %MEM     TIME+ COMMAND"}
   {:rest-of-str " 1803 andy      20   0 4152920 294476 114176 S   6.2  7.3   1:52.41 gnome-shell"}])
;; {:error :no-total-cpu-use-line, ...}

(def lms
  [{:usec 0,
    :rest-of-str "top - 11:28:04 up  5:27,  1 user,  load average: 0.80, 0.99, 0.69"}
   {:rest-of-str "Tasks: 215 total,   1 running, 167 sleeping,   0 stopped,   0 zombie"}
   {:rest-of-str "%Cpu(s):  2.9 us,  0.2 sy,  0.0 ni, 96.9 id,  0.0 wa,  0.0 hi,  0.0 si,  0.0 st"}
   ;;{:rest-of-str "KiB Mem :  4030264 total,   547848 free,   829628 used,  2652788 buff/cache"}
   {:rest-of-str "KiB Swap:  2097148 total,  2096624 free,      524 used.  2901400 avail Mem "}
   {:rest-of-str ""}
   {:rest-of-str "  PID USER      PR  NI    VIRT    RES    SHR S  %CPU %MEM     TIME+ COMMAND"}
   {:rest-of-str " 1803 andy      20   0 4152920 294476 114176 S   6.2  7.3   1:52.41 gnome-shell"}])
;; {:error :no-total-mem-use-line, ...}

(def lms
  [{:usec 0,
    :rest-of-str "top - 11:28:04 up  5:27,  1 user,  load average: 0.80, 0.99, 0.69"}
   {:rest-of-str "Tasks: 215 total,   1 running, 167 sleeping,   0 stopped,   0 zombie"}
   {:rest-of-str "%Cpu(s):  2.9 us,  0.2 sy,  0.0 ni, 96.9 id,  0.0 wa,  0.0 hi,  0.0 si,  0.0 st"}
   {:rest-of-str "KiB Mem :  4030264 total,   547848 free,   829628 used,  2652788 buff/cache"}
   {:rest-of-str "KiB Swap:  2097148 total,  2096624 free,      524 used.  2901400 avail Mem "}
   {:rest-of-str ""}
   {:rest-of-str "  PID USER      PR  NI    VIRT    RES    SHR S  %CPU %MEM     TIME+ COMMAND"}
   {:rest-of-str " 1803 andy      2.0   0 4152920 294476 114176 S   6.2  7.3   1:52.41 gnome-shell"}])
;; {:error :unrecognized-process-info-line, ...}

(def lms
  [{:usec 0,
    :rest-of-str "top - 11:28:04 up  5:27,  1 user,  load average: 0.80, 0.99, 0.69"}
   {:rest-of-str "Tasks: 215 total,   1 running, 167 sleeping,   0 stopped,   0 zombie"}
   {:rest-of-str "%Cpu(s):  2.9 us,  0.2 sy,  0.0 ni, 96.9 id,  0.0 wa,  0.0 hi,  0.0 si,  0.0 st"}
   {:rest-of-str "KiB Mem :  4030264 total,   547848 free,   829628 used,  2652788 buff/cache"}
   {:rest-of-str "KiB Swap:  2097148 total,  2096624 free,      524 used.  2901400 avail Mem "}
   {:rest-of-str ""}
   {:rest-of-str "  PID USER      PR  NI    VIRT    RES    SHR S  %CPU %MEM     TIME+ COMMAND"}
   {:rest-of-str " 1803 andy      20   0 1067584 106848  57704 S   6.2  2.7   0:32.52 Xorg"}
   {:rest-of-str " 1803 andy      20   0 4152920 294476 114176 S   6.2  7.3   1:52.41 gnome-shell"}])
;; {:error :multiple-processes-with-same-pid, ...}

(def x (parse-one-top-output lms))
(pprint x)

)

(defn read-top-output-and-validate
  "Given a file name, read it, split it into lines, parse it with
  parse-and-check-timestamps, break it into groups of lines starting
  at lines that appear to be the first line of output from the 'top'
  command according to the function top-first-output-line?, then parse
  each group to extract some information from that instance of top
  producing output.

  If all goes well, return a sequence of maps, one for each occurrence
  of 'top' output found for one point in time.  See
  parse-one-top-output for the contents of these maps.

  If any problems are found, return one map that contains at least the
  keys :error and :error-data, describing the problem found."
  [fname]
  (let [lines (->> fname slurp str/split-lines)
        lms (parse-and-check-timestamps lines)]
    (if (and (map? lms) (contains? lms :error))
      lms
      (let [lmgs (u/partition-starting-at #(top-first-output-line?
                                            (:rest-of-str %))
                                          lms)
            top-infos (map parse-one-top-output lmgs)
            first-error (first (filter #(contains? % :error) top-infos))]
        (if first-error
          first-error
          top-infos)))))

(defn summarize-top-infos [top-infos]
  (if (and (map? top-infos) (contains? top-infos :error))
    (do
      (println "Error while reading or parsing found:")
      (pp/pprint top-infos))
    (let [first-ti (first top-infos)
          last-ti (last top-infos)
          top-times (map :usec top-infos)
          deltas (map - (rest top-times) top-times)
          ;; Difference between (a) total mem used in summary line,
          ;; minus (b) total RESident memory of all processes:
          total-mem-differences
          (map (fn [ti]
                 (let [a (get-in ti [:total-mem-use-info :used-mem-kib])
                       b (get ti :total-resident-memory-size-kib)]
                   {:usec (:usec ti)
                    :summary-used-mem-kib a
                    :total-resident-mem-kib b
                    :diff (- a b)}))
               top-infos)
          ;; Difference between (a) %MEM for a single process, minus
          ;; (b) RESident memory of the process divided by total
          ;; memory of the system (times 100 to make it in units of
          ;; percent)
          ;; tbd
          percent-mem-differences
          (mapcat (fn [ti]
                    (map (fn [m]
                           (let [pi (:process-info m)
                                 a (:memory-usage-percent pi)
                                 total-mem (get-in ti [:total-mem-use-info
                                                       :total-mem-kib])
                                 b (* 100.0 (/ (:resident-memory-size-kib pi)
                                               total-mem))]
                             {:usec (:usec ti)
                              :pid (:pid pi)
                              :mem-pct a
                              :res-pct-of-total-mem b
                              :diff (- a b)}))
                         (vals (:pid->proc ti))))
                  top-infos)

          ;; TBD: Should probably print this number out in the log
          ;; file somewhere, so it is not hard-coded in this program.
          num-cpus 4

          ;; Difference between (a) total %CPU all processes,
          ;; minus (b) total of user and system %Cpu from summary
          ;; line, multiplied by number of CPUs
          total-cpu-percent-differences
          (map (fn [ti]
                 (let [a (get ti :total-cpu-usage-percent)
                       cpu (get ti :total-cpu-use-info)
                       b (* num-cpus
                            (+ (get cpu :user-cpu-percent)
                               (get cpu :kernel-cpu-percent)))]
                   {:usec (:usec ti)
                    :total-cpu-percent a
                    :user-plus-sys-cpu b
                    :diff (- a b)}))
               top-infos)
          ]
      (println "Total lines:" (reduce + (map (fn [top-info]
                                               (+ (count (:top-heading-lines top-info))
                                                  (count (:top-process-lines top-info))))
                                             top-infos)))
      (println "Number of top outputs found:" (count top-infos))
      (println "Elapsed time from first to last output (sec):"
               (u/usec-to-sec (:usec last-ti)))
      (println "Elapsed times between starting each top output (sec):"
               "min" (u/usec-to-sec (apply min deltas))
               "avg" (u/usec-to-sec (apply u/avg deltas))
               "max" (u/usec-to-sec (apply max deltas)))

      ;; I have seen number (a) be about 1 GByte when (b) was about 2
      ;; Gbytes, for a huge difference.  I don't know whether these
      ;; two values are supposed to be related at all, or whether
      ;; there might be soe other function of the stats of the
      ;; individual processes that might be closer to (a).
      #_(println "Difference between (a) total mem used in summary line, minus (b) total RESident memory of all processes:"
               "min" (apply min-key :diff total-mem-differences)
               "max" (apply max-key :diff total-mem-differences))

      ;; The maximum differences I saw across several hundred outputs
      ;; of the top command were in the range -0.05 to +0.05, which is
      ;; exactly the range you would expect if the value in the %MEM
      ;; column was calculated by rounding off the formula I compared
      ;; it against, to the nearest multiple of 0.1.
      (println "Difference between (a) %MEM for a single process, minus (b) RESident memory of the process divided by total memory of the system (times 100 to make it in units of percent):"
               "min" (format "%.3f" (apply min (map :diff percent-mem-differences)))
               "max" (format "%.3f" (apply max (map :diff percent-mem-differences))))

      ;; I have seen huge differences between these two values, e.g.:
      ;; (a) was 14.4%, but (b) was 4*(26.0+0.9) = 107.6%
      ;;     %Cpu(s): 26.0 us,  0.9 sy,  0.0 ni, 73.0 id,  0.0 wa,  0.0 hi,  0.0 si,  0.0 st
      ;; in the other direction:
      ;; (a) was 109.2%, but (b) was 4*(11.9 + 0.1) = 48.0%
      ;;     %Cpu(s): 11.9 us,  0.1 sy,  0.0 ni, 87.8 id,  0.0 wa,  0.0 hi,  0.1 si,  0.0 st
      ;; The top man page from Ubuntu 18.04 Linux says that both of
      ;; these quantities are based on the most recent time period
      ;; only, so I have no guesses why these values can differ so
      ;; much.
      #_(println "Difference between (a) total %CPU all processes, minus (b) total of user and system %Cpu from summary line, multiplied by number of CPUs (from /proc/cpuinfo on Linux):"
               "min" (apply min-key :diff total-cpu-percent-differences)
               "max" (apply max-key :diff total-cpu-percent-differences))
      )))


(defn summarize-top-file [m]
  (let [fname (:fname m)
        ti (read-top-output-and-validate fname)]
    (summarize-top-infos ti)))


(comment

(require '[com.andyfingerhut.bench.analyze.results :as res])
(in-ns 'com.andyfingerhut.bench.analyze.results)
(in-ns 'user)


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Experiments with DateTimeFormatter and LocalDateTime classes, to
;; learn a little bit about their behavior.
  
(def dt1 "2020-12-29 19:28:05")
(def dt1 "2020-12-29 19:28:05.123456")
(def dt2 "2020-12-29 19:28:05.123457")
(def dt3 "2020-12-29 19:28:07.000000")
(def dtf1 (java.time.format.DateTimeFormatter/ofPattern "yyyy-MM-dd HH:mm:ss.SSSSSS"))
(def ldt1 (java.time.LocalDateTime/parse dt1 dtf1))
ldt1
;; #object[java.time.LocalDateTime 0x4dad7e1c "2020-12-29T19:28:05"]
(def ldt2 (java.time.LocalDateTime/parse dt2 dtf1))
(def ldt3 (java.time.LocalDateTime/parse dt3 dtf1))

(. ldt1 (until ldt2 java.time.temporal.ChronoUnit/MICROS))
;; 1

(. ldt2 (until ldt1 java.time.temporal.ChronoUnit/MICROS))
;; -1

(. ldt1 (until ldt3 java.time.temporal.ChronoUnit/MICROS))
;; 1876544

(. ldt3 (until ldt1 java.time.temporal.ChronoUnit/MICROS))
;; -1876544


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Experiments reading and extracting info from log lines in files.

(use 'clojure.pprint)
(use 'clojure.repl)
(require '[clojure.set :as set])

(def fname "/home/andy/clj/benchmark-utils/ubuntu-18.04.5-openjdk-11.0.9.1-run1-log.txt")
(def fname "/home/andy/clj/benchmark-utils/ubuntu-18.04.5-openjdk-11.0.9.1-run1-top.txt")
(def fname "/home/andy/clj/benchmark-utils/ubuntu-18.04.5-openjdk-11.0.9.1-run2-top.txt")
(def fname "/home/andy/clj/benchmark-utils/ubuntu-18.04.5-openjdk-11.0.9.1-run3-top.txt")
(def fname "/home/andy/clj/benchmark-utils/ubuntu-18.04.5-openjdk-11.0.9.1-suspend-vm-top.txt")
(def fname "/home/andy/clj/benchmark-utils/ubuntu-18.04.5-openjdk-11.0.9.1-close-laptop-30sec-top.txt")

;; Combining some of the steps below into a few function calls:
(def ti (read-top-output-and-validate fname))
(summarize-top-infos ti)

(def cpu-use-key-renaming
  {:user-cpu-percent :user
   :kernel-cpu-percent :sys
   :niced-process-cpu-percent :nice
   :idle-cpu-percent :idle
   :waiting-io-cpu-percent :wait
   :service-hardware-interrupt-cpu-percent :hwint
   :service-software-interrupt-cpu-percent :swint
   :time-stolen-from-this-vm-by-hypervisor-cpu-percent :stolen})

(def total-use-key-renaming
  {:total-resident-memory-size-kib :tot-res
   :total-shared-memory-size-kib :tot-shr
   :total-cpu-usage-percent :tot-%cpu
   :total-memory-usage-percent :tot-%mem})

(def cpus
  (->> ti
       (map (fn [m]
              (let [total-mem-kib (get-in m [:total-mem-use-info :total-mem-kib])
                    m (-> m
                          (assoc :sec (u/usec-to-sec (:usec m))
                                 :tot (reduce + (vals (:total-cpu-use-info m))))
                          (merge (set/rename-keys (:total-cpu-use-info m)
                                                  cpu-use-key-renaming))
                          (set/rename-keys total-use-key-renaming)
                          )
                    m (-> m
                          (assoc :user-sysx4 (* 4 (+ (:user m) (:sys m)))
                                 :tot-res-pct (* 100.0 (/ (:tot-res m) total-mem-kib))) )
                    m (-> m
                          (assoc :res-pct-dif (- (:tot-res-pct m) (:tot-%mem m))))
                    m (-> m
                          (update :sec #(format "%.2f" %))
                          (update :tot #(format "%.2f" %))
                          (update :user-sysx4 #(format "%.2f" %))
                          (update :tot-%cpu #(format "%.2f" %))
                          (update :tot-%mem #(format "%.2f" %))
                          (update :tot-res-pct #(format "%.2f" %))
                          (update :res-pct-dif #(format "%.2f" %))
                          )
                    ]
                m)))))
(pprint (nth cpus 0))
(clojure.pprint/print-table [:sec
                             :tot :user :sys
                             :user-sysx4 :tot-%cpu
                             :tot-res :res-pct-dif :tot-%mem :tot-shr
                             :nice :wait :hwint :swint :stolen]
                            cpus)
(doc clojure.pprint/print-table)


(def lines (->> fname slurp str/split-lines))
(count lines)
(def lms (parse-and-check-timestamps lines))
(and (map? lms) (contains? lms :error))
(count lms)
(pprint (take 4 lms))

;; All of the deltas I saw were over 2.0 sec, averaged about 2.015
;; sec, and one was just under 2.2 sec.

;; For any process that exists in two consecutive top outputs, it
;; should have the same PID and at least usually the same USER and
;; COMMAND (although I believe those can be changed by a process's
;; actions).  Its TIME+ value should never decrease.

;; TBD: Should "Tasks: <n> total" always equal the number of processes
;; with info printed?

;; TBD: Should "<n> running" always equal the number of processes with
;; status "R"?

;; From the first top output after a JVM process running a Criterium
;; benchmark prints "Sampling ...", until it prints "Final GC..."
;; about 1 minute later, what is the min, max, and avg %CPU of that
;; process?

;; What is the min, max, and avg of the total of %CPU among all other
;; processes?

)
