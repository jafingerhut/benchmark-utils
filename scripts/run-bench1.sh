#! /bin/bash

java -version 2>&1
clojure -M:clj -i src/com/andyfingerhut/bench_dot1.clj
