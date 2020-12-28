#! /bin/bash

# Run some command CMD_WITH_ARGS given as arguments as this command's
# arguments.

# Run it with prepend-times.sh so that its output lines are
# timestamped.

# While it is running, also in parallel run top-every-2-sec.sh, also
# with its output lines timestamped.

# Record the outputs in two separate files.

# The intent is that later we will be able to analyze these two output
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
OUT_LOG_FNAME="${OUT_BASE_FNAME}-log.txt"

THIS_SCRIPT_FILE_MAYBE_RELATIVE="$0"
THIS_SCRIPT_DIR_MAYBE_RELATIVE="${THIS_SCRIPT_FILE_MAYBE_RELATIVE%/*}"
THIS_SCRIPT_DIR_ABSOLUTE=`"${READLINK}" -f "${THIS_SCRIPT_DIR_MAYBE_RELATIVE}"`

TMPF=`mktemp`
"${THIS_SCRIPT_DIR_ABSOLUTE}/top-every-2-sec.sh" "${TMPF}" | "${THIS_SCRIPT_DIR_ABSOLUTE}/prepend-times.sh" top: > "${OUT_TOP_FNAME}" &
CHILD_PROCESS_PID=$!

# Hack: Give enough time for top-every-2-sec.sh to write to file
# ${TMPF}
sleep 1
TOP_PROCESS_PID="`cat ${TMPF}`"
/bin/rm -f "${TMPF}"

clean_up() {
    echo "Killing child process with pid ${CHILD_PROCESS_PID}"
    kill ${CHILD_PROCESS_PID}
    echo "Killing top process with pid ${TOP_PROCESS_PID}"
    kill ${TOP_PROCESS_PID}
    exit
}

# Kill the child process
trap clean_up SIGHUP SIGINT SIGTERM

"$*" | "${THIS_SCRIPT_DIR_ABSOLUTE}/prepend-times.sh" log: > "${OUT_LOG_FNAME}"

clean_up
