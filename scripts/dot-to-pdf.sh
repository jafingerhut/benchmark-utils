#! /bin/bash

for j in *.dot
do
    echo $j
    dot -Tpdf $j > `basename $j .dot`.pdf
done
