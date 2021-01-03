#! /bin/bash

# Cause an Ubuntu Linux system to update its date and time to one
# retrieved from google.com.  Require a network connection.

# Found via some searching how to update the system date and time on
# recent Ubuntu Linux systems (here recent means 16.04 and later, as
# of 20.04 being the latest LTS version).

# https://askubuntu.com/questions/81293/what-is-the-command-to-update-time-and-date-from-internet

# The answer beginning with this sentence is the one I am using here:

# This is a nice little code I found to update your time in case you
# have issues with ntp

# wget options used:
# -q / --quiet - Turn off wget's output

# -S / --server-response - Print the headers sent by HTTP servers and
#     -responses sent by FTP servers

# -O- - write output to standard output (the '-' indicates standard output)

# --max-redirect=<number> - specifies the max number of redirections
#     to follow for a resource.  The deault is 20, which is usually far
#     more than necessary.

sudo date --universal --set="$(wget -qSO- --max-redirect=0 google.com 2>&1 | grep Date: | cut -d' ' -f5-8)Z"
