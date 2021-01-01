(ns com.andyfingerhut.bench.analyze.utils)


(defn usec-to-sec [usec]
  (/ usec 1e6))


(defn avg
  "Returns the average of the nums."
  [& xs]
  (double (/ (reduce + xs) (count xs))))

(comment
(avg 1 2 3)
(avg 1 2)
)


(defn take-until
  "Returns a lazy sequence of successive items from coll up to and
  including the first item for which (pred item) returns logical
  true. pred must be free of side-effects, or all items from coll if
  there is no such item."
  [pred coll]
  (lazy-seq
   (when-let [s (seq coll)]
     (if (pred (first s))
       (cons (first s) nil)
       (cons (first s) (take-until pred (rest s)))))))

(defn drop-until
  "Returns a lazy sequence of the items in coll starting just after
  the first for which (pred item) returns logical true, or an empty
  sequence if there is no such item in the sequence."
  [pred coll]
  (let [step (fn [pred coll]
               (let [s (seq coll)]
                 (if s
                   (if (pred (first s))
                     (rest s)
                     (recur pred (rest s))))))]
    (lazy-seq (step pred coll))))

(comment
(take-until #(= 1 %) [0 0 1 1 1 0 0])
(take-until #(= 1 %) [1 0 1 1 1 0 0])
(take-until #(= 1 %) [])
(take-until #(= 1 %) [0 0 0 0])

(drop-until #(= 1 %) [0 0 1 1 1 0 0])
(drop-until #(= 1 %) [1 0 1 1 1 0 0])
(drop-until #(= 1 %) [])
(drop-until #(= 1 %) [0 0 0 0])
)

(defn split-until
  "Returns a vector of [(take-while pred coll) (drop-until pred coll)]"
  [pred coll]
  [(take-until pred coll) (drop-until pred coll)])

(defn partition-starting-at
  "Applies `pred` to each value in coll, splitting it each time `pred`
  returns a logical true value (i.e. neither nil nor false).  Returns
  a lazy seq of partitions."
  [pred coll]
  (lazy-seq
   (when-let [s (seq coll)]
     (let [fst (first s)
           fv (pred fst)
           run (if fv
                 (cons fst (take-while #(not (pred %)) (next s)))
                 (take-while #(not (pred %)) s))]
       (cons run (partition-starting-at pred (lazy-seq (drop (count run) s))))))))

(comment

(partition-starting-at #(= 1 %) [1 0 0 1 1 0 0 1])
;; => ((1 0 0) (1) (1 0 0) (1))
(partition-starting-at #(= 1 %) [0 0 0 1 1 0 0 1])
;; => ((0 0 0) (1) (1 0 0) (1))
(partition-starting-at #(= 1 %) [0])
;; => ((0))
(partition-starting-at #(= 1 %) [1])
;; => ((1))

)
