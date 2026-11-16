#!/bin/bash
# Reports brute-force sources in an OpenSSH authentication log.
# Usage: bruteforce.sh LOG [THRESHOLD]
set -u

usage() {
    echo "usage: bruteforce.sh LOG [THRESHOLD]" >&2
    exit 2
}

[ "$#" -ge 1 ] && [ "$#" -le 2 ] || usage
log=$1
threshold=${2:-5}

if [ ! -f "$log" ] || [ ! -r "$log" ]; then
    echo "error: cannot read $log" >&2
    exit 2
fi
if ! [[ $threshold =~ ^[0-9]+$ ]] || [ "$((10#$threshold))" -lt 1 ]; then
    echo "error: threshold must be a positive integer" >&2
    exit 2
fi
threshold=$((10#$threshold))

# The username is attacker-controlled, so the IP and port are read from the end of the line.
# shellcheck disable=SC2016
awk -v threshold="$threshold" '
    function user_between(prefix, stop,    start) {
        start = index($0, prefix) + length(prefix)
        if (substr($0, start, 13) == "invalid user ") {
            start += 13
        }
        return substr($0, start, stop - start)
    }

    { sub(/\r$/, "") }

    !/ sshd\[[0-9]+\]: / { next }

    /: Failed password for / && match($0, / from ([0-9A-Fa-f:.]+) port [0-9]+ ssh2$/, m) {
        ip = m[1]
        user = user_between(": Failed password for ", RSTART)
        if (!(ip in fails)) {
            ips[++nips] = ip
        }
        fails[ip]++
        if (!((ip, user) in seen)) {
            seen[ip, user] = 1
            names[ip] = (ip in names) ? names[ip] SUBSEP user : user
        }
        next
    }

    match($0, /: Accepted (password|publickey|keyboard-interactive\/pam) for /, a) &&
    match($0, / from ([0-9A-Fa-f:.]+) port [0-9]+ ssh2(: .*)?$/, m) {
        stop = RSTART
        ip = m[1]
        prefix = substr($0, index($0, ": Accepted "))
        prefix = substr(prefix, 1, index(prefix, " for ") + 4)
        user = user_between(prefix, stop)
        before = (ip in fails) ? fails[ip] : 0
        if (before >= threshold) {
            alerts[++nalerts] = "ALERT " ip " accepted user=" user " after " before " failures"
        }
    }

    END {
        n = 0
        for (i = 1; i <= nips; i++) {
            ip = ips[i]
            if (fails[ip] >= threshold) {
                order[++n] = sprintf("%012d %s", 999999999999 - fails[ip], ip)
            }
        }
        asort(order)
        for (i = 1; i <= n; i++) {
            ip = substr(order[i], 14)
            count = split(names[ip], list, SUBSEP)
            asort(list)
            joined = list[1]
            for (j = 2; j <= count; j++) {
                joined = joined "," list[j]
            }
            print "SUSPECT " ip " failures=" fails[ip] " users=" joined
        }
        for (i = 1; i <= nalerts; i++) {
            print alerts[i]
        }
        exit (nalerts > 0) ? 1 : 0
    }
' "$log"
