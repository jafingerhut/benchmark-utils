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

which ts 2>&1 > /dev/null
if [ $? != 0 ]
then
    2>&1 echo "No command 'ts' installed."
    if [ `get_os` == "osx" ]
    then
	2>&1 echo "It can be installed using MacPorts or Homebrew"
	2>&1 echo "in the package named 'moreutils'."
    elif [ `get_os` == "ubuntu" ]
    then
	2>&1 echo "It can be installed using the command:"
	2>&1 echo "    sudo apt-get install moreutils"
    fi
    exit 1
fi

if [ `get_os` == "osx" ]
then
    top -l 0 -s 2 | TZ=UTC ts 'UTC %Y-%m-%d %H:%M:%S%.S top: '
elif [ `get_os` == "ubuntu" ]
then
    top -b -d 2 | TZ=UTC ts 'UTC %Y-%m-%d %H:%M:%S top: '
else
    2>&1 echo "Unknown OS not supported by this script."
    exit 1
fi
