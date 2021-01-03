#! /bin/bash

# By using exec, there is only ever one process at a time for this
# script, so if it is killed, that is the only one that needs to be
# killed to "clean up".

exec clojure -X:clj com.andyfingerhut.periodic/elapsed-times :period-msec 1000
