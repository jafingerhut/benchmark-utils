#! /bin/bash

# See if any of the process created by run-with-top.sh are still
# running.

echo "elapsed-times.sh"
ps axguw | grep elapsed-times.sh | egrep -v '[g]rep'
echo "top-every-2-sec.sh"
ps axguw | grep top-every-2-sec.sh | egrep -v '[g]rep'
echo "cycle-ntp-periodically.sh"
ps axguw | grep cycle-ntp-periodically.sh | egrep -v '[g]rep'
echo "top"
ps axguw | egrep '\btop\b' | egrep -v '[g]rep'
echo "prepend-times.sh"
ps axguw | grep prepend-times.sh | egrep -v '[g]rep'
echo "java"
ps axguw | egrep '\bjava\b' | egrep -v '[g]rep'
echo "clj"
ps axguw | grep '\bclj\b' | egrep -v '[g]rep'
echo "clojure"
ps axguw | grep '\bclojure\b' | egrep -v '[g]rep'
