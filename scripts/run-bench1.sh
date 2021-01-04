#! /bin/bash

is_osx ()
{
    [[ "$OSTYPE" =~ ^darwin ]] || return 1
}

is_ubuntu ()
{
    [[ "$(cat /etc/issue 2> /dev/null)" =~ Ubuntu ]] || return 1
}

function get_os() {
  for os in osx ubuntu; do
    is_$os; [[ $? == ${1:-0} ]] && echo $os
  done
}

if [ `get_os` == "osx" ]
then
    # TBD how best to determine similar info on OSX.  The most detail
    # I know how to get from the command line is using the command:

    # system_profiler -detailLevel full SPHardwareDataType

    # But that does not show things like MacBookPro11,2 like you can
    # see by using the GUI to select "About This Mac" under the Apple
    # icon menu, nor does it contain any info about the processor
    # model, which looks like it is provided to the public on this web
    # site:

    # https://everymac.com/ultimate-mac-lookup/

    echo
elif [ `get_os` == "ubuntu" ]
then
    echo "Number of CPU cores: `grep -c process /proc/cpuinfo`"
    echo "CPU `grep 'model name' /proc/cpuinfo | head -n 1`"
    lsb_release -a
fi

java -version 2>&1
#clojure -X:clj com.andyfingerhut.bench.double-vec-100k-dot-product/clojure-persistent-vector
#clojure -X:clj com.andyfingerhut.bench.double-vec-100k-dot-product/clojure-gvec-primitive-doubles
#clojure -X:clj com.andyfingerhut.bench.double-vec-100k-dot-product/java-array-boxed-doubles
#clojure -X:clj com.andyfingerhut.bench.double-vec-100k-dot-product/java-array-primitive-doubles-areduce
#clojure -X:clj com.andyfingerhut.bench.double-vec-100k-dot-product/java-array-primitive-doubles-fluokitten-foldmap
#clojure -X:clj com.andyfingerhut.bench.double-vec-100k-dot-product-set2/neanderthal-double-vectors-fluokitten-foldmap
clojure -X:clj com.andyfingerhut.bench.double-vec-100k-dot-product-set2/neanderthal-double-vectors-mkl-dot
