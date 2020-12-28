#! /bin/bash

# On this page:
# https://stackoverflow.com/questions/21564/is-there-a-unix-utility-to-prepend-timestamps-to-stdin

# I learned about the `unbuffer` and `ts` commands, which are
# available on Ubuntu Linux in packages `expect` and `moreutils`.

# ts can format dates the same as the `strftime` C library call.  This
# format looks straightforward to parse AND MERGE SORT for machines,
# yet still readable for humans:

# %Y year in decimal including the century, e.g. 2019
# %m month in decimal with leading 0, in range 01 to 12
# %d the day of the month as a decimal number with leading 0, in range 01 to 31

# %H the hour as a decimal number using a 24-hour clock, with leading
#     0, range 00 to 23.
# %M minute as a decimal number with leading 0, range 00 to 59
# %S second as a decimal number with leading 0, range 00 to 60 to
#     allow for occasional leap seconds
# %.S microseconds as a decimal number with leading 0, range 000000 to
#     999999

# top -b -d 2 -p <pid> | TZ=UTC ts 'UTC %Y-%m-%d %H:%M:%S%.S'

# After `brew install moreutils` on macOS:

# /usr/bin/top -l 5 -s 2 -pid 24043 | TZ=UTC ts 'UTC %Y-%m-%d %H:%M:%S%.S'

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

TZ=UTC ts 'UTC %Y-%m-%d %H:%M:%S%.S top: '
