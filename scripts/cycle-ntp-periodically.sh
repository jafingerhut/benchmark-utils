#! /bin/bash

# See cycle-ntp.sh for notes on what this does

# I have had difficulty finding a way to reliably kill this process
# off after starting it from a bash script in the background.  I do
# not know what the issue is there, but as a workaround, take an
# argument that is a file name.  Continue executing only as long as
# that file exists.  Exit if the file no longer exists.  Thus a parent
# process can create a file (e.g. a temporary one with mktemp), start
# this script running, and delete the file when it wants this process
# to quit.

if [ $# -ne 1 ]
then
    1>&2 echo "usage: `basename $0` <keep-going-file-name>"
    exit 1
fi

KEEP_GOING_FILE="$1"

while [ -e "${KEEP_GOING_FILE}" ]
do
    sleep 5
    echo "Date & time before cycling NTP off then on again:"
    date
    sudo timedatectl set-ntp off
    sudo timedatectl set-ntp on
    echo "After:"
    date
done
