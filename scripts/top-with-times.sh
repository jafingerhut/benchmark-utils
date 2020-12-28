#! /bin/bash

# Run top in batch mode (i.e. no screen redrawing) once every 2
# seconds on all processes, until killed.

######################################################################
# On macOS 10.14.6 at least:

# `-s 2` causes output to be generated once every 2 seconds.

# Use `-l <num>` to limit it to print a report that many times.

# Use `-pid <pid>` to limit output to only the system-wide statistics,
# plus information about that one process.  Without that, gives
# information about all processes.

# top -l 0 -s 2

######################################################################
# On Ubuntu 18.04:

# `-d 2` causes output to be generated once every 2 seconds.

# Add `-n <num>` to limit it to print a report that many times before
# exiting.

# Use `-p <pid>` to limit output to only the system-wide statistics,
# plus information about that one process.  Without that, gives
# information about all processes.

# top -b -d 2

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
    top -l 0 -s 2
elif [ `get_os` == "ubuntu" ]
then
    top -b -d 2
else
    2>&1 echo "Unknown OS not supported by this script."
    exit 1
fi
