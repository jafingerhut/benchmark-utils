#! /bin/bash

# Cause an Ubuntu Linux system to update its date and time using NTP.
# Require a network connection to one or more NTP servers.

# Found via some searching how to update the system date and time on
# recent Ubuntu Linux systems (here recent means 16.04 and later, as
# of 20.04 being the latest LTS version).

# https://askubuntu.com/questions/81293/what-is-the-command-to-update-time-and-date-from-internet

# The answer beginning with this sentence is the one I am using here:

# As of 2018 with a fresh installed Ubuntu 16.04 LTS

# TBD: If my local clock was ever later than the new time, does NTP
# cause the time to jump backwards suddenly, or does it slow down the
# clock rate advancement a bit until it matches, then go back to
# advancing at the normal rate?

echo "Date & time before cycling NTP off then on again:"
date
sudo timedatectl set-ntp off
sudo timedatectl set-ntp on
echo "After:"
date
