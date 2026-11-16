#!/bin/bash
# Audits an OpenSSH server configuration for risky settings.
# Usage: audit.sh CONFIG
set -u

if [ "$#" -ne 1 ]; then
    echo "usage: audit.sh CONFIG" >&2
    exit 2
fi
config=$1
if [ ! -f "$config" ] || [ ! -r "$config" ]; then
    echo "error: cannot read $config" >&2
    exit 2
fi

# shellcheck disable=SC2016
awk '
    function shown(key, default) {
        return (key in value) ? value[key] : default " (default)"
    }
    function effective(key, default) {
        return tolower((key in value) ? value[key] : default)
    }
    function report(severity, text) {
        print severity " " text
        findings++
        if (severity == "HIGH") high++
    }
    function weak(list, pattern,    parts, n, i, out) {
        if (substr(list, 1, 1) == "-") return ""
        if (substr(list, 1, 1) == "+" || substr(list, 1, 1) == "^") list = substr(list, 2)
        n = split(list, parts, ",")
        out = ""
        for (i = 1; i <= n; i++) {
            if (tolower(parts[i]) ~ pattern) out = (out == "") ? parts[i] : out "," parts[i]
        }
        return out
    }

    {
        sub(/\r$/, "")
        sub(/^[ \t]+/, "")
        sub(/[ \t]+$/, "")
        if ($0 == "" || substr($0, 1, 1) == "#") next
        if (!match($0, /^[A-Za-z0-9]+/)) next
        key = tolower(substr($0, 1, RLENGTH))
        rest = substr($0, RLENGTH + 1)
        sub(/^[ \t]*=?[ \t]*/, "", rest)
        if (key == "match") exit
        if (!(key in value)) value[key] = rest
    }

    END {
        findings = 0; high = 0
        if (effective("permitrootlogin", "prohibit-password") == "yes")
            report("HIGH", "PermitRootLogin " shown("permitrootlogin", "") ": root may log in with a password")
        if (effective("permitemptypasswords", "no") == "yes")
            report("HIGH", "PermitEmptyPasswords " shown("permitemptypasswords", "") ": accounts without a password may log in")
        if (effective("passwordauthentication", "yes") == "yes")
            report("MEDIUM", "PasswordAuthentication " shown("passwordauthentication", "yes") ": allow keys only")
        tries = effective("maxauthtries", "6")
        if (tries !~ /^[0-9]+$/ || tries + 0 < 1 || tries + 0 > 4)
            report("MEDIUM", "MaxAuthTries " shown("maxauthtries", "6") ": allow at most 4 attempts")
        if (effective("x11forwarding", "no") == "yes")
            report("LOW", "X11Forwarding " shown("x11forwarding", "") ": disable unless needed")
        if ("ciphers" in value) {
            list = weak(value["ciphers"], "cbc|arcfour|3des")
            if (list != "") report("HIGH", "Ciphers " list ": weak algorithms")
        }
        if ("macs" in value) {
            list = weak(value["macs"], "(md5|sha1)([-@]|$)|^umac-64")
            if (list != "") report("HIGH", "MACs " list ": weak algorithms")
        }
        print "findings: " findings " (high: " high ")"
        exit (high > 0) ? 1 : 0
    }
' "$config"
