#! /bin/bash

java -version 2>&1
clojure -X:clj com.andyfingerhut.bench.double-vec-100k-dot-product/clojure-persistent-vector
clojure -X:clj com.andyfingerhut.bench.double-vec-100k-dot-product/clojure-gvec-primitive-doubles
clojure -X:clj com.andyfingerhut.bench.double-vec-100k-dot-product/java-array-boxed-doubles
clojure -X:clj com.andyfingerhut.bench.double-vec-100k-dot-product/java-array-primitive-doubles-areduce
clojure -X:clj com.andyfingerhut.bench.double-vec-100k-dot-product/java-array-primitive-doubles-fluokitten-foldmap
clojure -X:clj com.andyfingerhut.bench.double-vec-100k-dot-product-set2/neanderthal-double-vectors-fluokitten-foldmap
clojure -X:clj com.andyfingerhut.bench.double-vec-100k-dot-product-set2/neanderthal-double-vectors-mkl-dot
