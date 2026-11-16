#!/bin/bash
# Audits an OpenSSH server configuration for risky settings.
# Usage: audit.sh CONFIG
#
# Today it greps for a few bad lines. It misses defaults, key=value lines, lower-case
# keywords and "first value wins", and it reports settings inside Match blocks. Rewrite it.
set -u

config=$1
grep "PermitRootLogin yes" "$config"
grep "PermitEmptyPasswords yes" "$config"
grep "PasswordAuthentication yes" "$config"
grep "X11Forwarding yes" "$config"
