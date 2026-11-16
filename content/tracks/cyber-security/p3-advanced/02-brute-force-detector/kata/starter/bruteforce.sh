#!/bin/bash
# Reports brute-force sources in an OpenSSH authentication log.
# Usage: bruteforce.sh LOG [THRESHOLD]
#
# Today it counts every "Failed" line per word 11 and prints all of them. Rewrite it so that
# it follows the README: sshd lines only, the IP from the end of the line, SUSPECT and ALERT
# lines, and the exit statuses.
set -u

log=$1
grep Failed "$log" | awk '{ print $11 }' | sort | uniq -c
