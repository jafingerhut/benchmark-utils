#! /bin/bash

clojure -X:clj com.andyfingerhut.bench.analyze.results/summarize-top-file :fname \"$1\"
