#! /bin/bash

# Run some command CMD_WITH_ARGS given as arguments as this command's
# arguments.

# Run it with prepend-times.sh so that its output lines are
# timestamped.

# While it is running, also in parallel run several other processes in
# the background, all of them also run with prepend-times.sh so that
# their output lines are timestamped:

# + top-every-2-sec.sh - This is useful to know if other processes are
#   using significant memory, CPU, or other resources, that might slow
#   down the process of interest.

# + elapsed-times.sh - This is a very simple process showing the
#   relationship between output of date / ts command times (from Linux
#   system time), versus elapsed time in one JVM process.  My testing
#   has shown that if you suspend a guest Linux VM that is running
#   this code, unless NTP, VirtualBox guest additions, or some other
#   program updates the system time, the system time will continue
#   where it left off when the system was suspended, so it falls
#   behind actual time.  The elapsed times also continue as if the
#   system was never suspended, so neither of these values alone is
#   enough to detect such a suspension.

# + cycle-ntp-periodically.sh - This is a very simple process that
#   disables NTP, then re-enables it, every 5 seconds.  The disabling
#   and re-enabling seems to be an effective way to force the system's
#   time to update to that of the NTP servers that it communicates
#   with, _if_ such systems are accessible over the network.  If all
#   network interfaces are disabled, for example, then even this does
#   not cause the system time to update.  The reason for running this
#   program is that any suspension of a guest VM system should be
#   noticeable in the output of elapsed-times.sh as sudden "quick
#   advances" in the system time.

# Record the output of each sub-process, and the main process, in
# separate files.

# The intent is that later we will be able to analyze these output
# files together to determine if anything odd performance-wise
# happened on the system that might have affected the performance of
# CMD_WITH_ARGS.

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
    READLINK="greadlink"
    which "${READLINK}" 2>&1 > /dev/null
    if [ $? != 0 ]
    then
	2>&1 echo "No command 'greadlink' installed."
	2>&1 echo "It can be installed using Homebrew"
	2>&1 echo "in the package named 'coreutils'."
    fi
elif [ `get_os` == "ubuntu" ]
then
    READLINK="readlink"
fi

if [ $# -lt 2 ]
then
    2>&1 echo "usage: `basename $0` <base-output-filename> <cmd> [ arg1 arg2 ... ]"
    exit 1
fi

OUT_BASE_FNAME="$1"
shift

OUT_TOP_FNAME="${OUT_BASE_FNAME}-top.txt"
OUT_JAVATIME_FNAME="${OUT_BASE_FNAME}-javatime.txt"
OUT_LOG_FNAME="${OUT_BASE_FNAME}-log.txt"
OUT_CYCLENTP_FNAME="${OUT_BASE_FNAME}-cyclentp.txt"

THIS_SCRIPT_FILE_MAYBE_RELATIVE="$0"
THIS_SCRIPT_DIR_MAYBE_RELATIVE="${THIS_SCRIPT_FILE_MAYBE_RELATIVE%/*}"
THIS_SCRIPT_DIR_ABSOLUTE=`"${READLINK}" -f "${THIS_SCRIPT_DIR_MAYBE_RELATIVE}"`

TMPF1=`mktemp`
TMPF2=`mktemp`
TMPF3=`mktemp`
TMPF4=`mktemp`
TMPF5=`mktemp`

( "${THIS_SCRIPT_DIR_ABSOLUTE}/elapsed-times.sh" & echo $! > ${TMPF1} ) | ( "${THIS_SCRIPT_DIR_ABSOLUTE}/prepend-times.sh" javatime: & echo $! > ${TMPF2} ) > "${OUT_JAVATIME_FNAME}" &

"${THIS_SCRIPT_DIR_ABSOLUTE}/top-every-2-sec.sh" "${TMPF3}" | "${THIS_SCRIPT_DIR_ABSOLUTE}/prepend-times.sh" top: > "${OUT_TOP_FNAME}" &
CHILD_PROCESS_PID2=$!

# The program cycle-ntp-periodically.sh has been written such that it
# should exit when it sees that the file named by its first argument
# no longer exists.
sudo "${THIS_SCRIPT_DIR_ABSOLUTE}/cycle-ntp-periodically.sh" ${TMPF4} | ( "${THIS_SCRIPT_DIR_ABSOLUTE}/prepend-times.sh" javatime: > "${OUT_CYCLENTP_FNAME}" & echo $! > ${TMPF5} ) &

# Hack: Give enough time for subshells above to write to TMPF files
sleep 1
CHILD_PROCESS_PID1a=`cat ${TMPF1}`
CHILD_PROCESS_PID1b=`cat ${TMPF2}`
#echo "elapsed-time.sh pid: ${CHILD_PROCESS_PID1a}"
#echo "prepend-times.sh #1 pid: ${CHILD_PROCESS_PID1b}"
TOP_PROCESS_PID=`cat ${TMPF3}`
#echo "top-every-2-sec.sh pid: ${CHILD_PROCESS_PID2}"
#echo "top pid: ${TOP_PROCESS_PID}"
CHILD_PROCESS_PID3b=`cat ${TMPF5}`
#echo "prepend-times.sh #2 pid: ${CHILD_PROCESS_PID3b}"

clean_up() {
    /bin/rm -f "${TMPF1}"
    /bin/rm -f "${TMPF2}"
    /bin/rm -f "${TMPF3}"
    /bin/rm -f "${TMPF4}"
    /bin/rm -f "${TMPF5}"
    #echo "Killing child process with pid ${CHILD_PROCESS_PID1a}"
    kill ${CHILD_PROCESS_PID1a}
    #echo "Killing child process with pid ${CHILD_PROCESS_PID1b}"
    kill ${CHILD_PROCESS_PID1b}
    #echo "Killing child process with pid ${CHILD_PROCESS_PID2}"
    kill ${CHILD_PROCESS_PID2}
    #echo "Killing top process with pid ${TOP_PROCESS_PID}"
    kill ${TOP_PROCESS_PID}
    #echo "Killing child process with pid ${CHILD_PROCESS_PID3b}"
    kill ${CHILD_PROCESS_PID3b}
    exit
}

# Extra output to see what sub-processes of this process were created
#THIS_SCRIPT_PID=$$
#set -x
#pstree -aAp $THIS_SCRIPT_PID
#ps -s $THIS_SCRIPT_PID
#set +x

# If we die, kill the child processes
trap clean_up SIGHUP SIGINT SIGTERM

$* | "${THIS_SCRIPT_DIR_ABSOLUTE}/prepend-times.sh" log: > "${OUT_LOG_FNAME}"

clean_up
