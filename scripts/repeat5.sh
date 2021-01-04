#! /bin/bash

NCPU=`grep -c process /proc/cpuinfo`

for j in 1 2 3 4 5
do
	set -x
	./scripts/run-with-top.sh testing-${NCPU}cpu-rep${j} ./scripts/run-bench1.sh
	set +x
done
