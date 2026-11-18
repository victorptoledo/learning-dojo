#!/usr/bin/env bash
# Usage: accounts.sh [--root DIR] COMMAND ... - manages the accounts of a system image:
# DIR/etc/passwd, DIR/etc/group, DIR/etc/shadow and the homes under DIR/home.
set -uo pipefail
export LC_ALL=C

usage() {
    echo "usage: accounts.sh [--root DIR] check|list|add|lock|unlock|del|audit [options] [NAME]" >&2
    exit 2
}

# die MESSAGE [STATUS]
die() {
    echo "accounts.sh: $1" >&2
    exit "${2:-1}"
}

check() {
    awk -F: '
        function report(message) { printf "%s:%d: %s\n", file, FNR, message; problems++ }
        FNR == 1 { file = FILENAME; sub(/.*\//, "", file) }
        pass == "pre" { known_gid[$3] = 1; next }
        /^$/ { next }
        file == "passwd" {
            if (NF != 7) { report("expected 7 fields, got " NF); next }
            if ($1 !~ /^[a-z_][a-z0-9_-]*$/ || length($1) > 32) report("bad user name: " $1)
            if ($1 in user) report("duplicate user: " $1)
            else { user[$1] = 1; order[++users] = $1 }
            if ($3 !~ /^[0-9]+$/) report("bad uid: " $3)
            else if ($3 == 0 && $1 != "root") report("uid 0 for " $1 " (only root may have uid 0)")
            else if ($3 in uid_owner) report("uid " $3 " already used by " uid_owner[$3])
            else uid_owner[$3] = $1
            if ($4 !~ /^[0-9]+$/) report("bad gid: " $4)
            else if (!($4 in known_gid)) report("gid " $4 " of " $1 " has no group")
            next
        }
        file == "group" {
            if (NF != 4) { report("expected 4 fields, got " NF); next }
            if ($1 in group) report("duplicate group: " $1)
            else { group[$1] = 1; groups++ }
            if ($3 !~ /^[0-9]+$/) report("bad gid: " $3)
            else if ($3 in gid_owner) report("gid " $3 " already used by " gid_owner[$3])
            else gid_owner[$3] = $1
            count = split($4, member, ",")
            for (i = 1; i <= count; i++) if (!(member[i] in user)) report("unknown member " member[i] " in " $1)
            next
        }
        file == "shadow" {
            if (NF != 9) { report("expected 9 fields, got " NF); next }
            if (!($1 in user)) report("unknown user " $1)
            if ($2 == "") report("empty password for " $1)
            in_shadow[$1] = 1
        }
        END {
            for (i = 1; i <= users; i++)
                if (!(order[i] in in_shadow)) { print "shadow: no entry for " order[i]; problems++ }
            if (problems) exit 1
            printf "ok: %d users, %d groups\n", users, groups
        }' pass=pre "$group" pass=main "$passwd" "$group" "$shadow"
}

list_users() {
    awk -F: '
        pass == "group" {
            name[$3] = $1
            count = split($4, member, ",")
            for (i = 1; i <= count; i++) {
                if (member[i] in extra) extra[member[i]] = extra[member[i]] "," $1
                else extra[member[i]] = $1
            }
            next
        }
        $3 >= 1000 && $3 < 60000 {
            printf "%s uid=%s group=%s groups=%s shell=%s\n", $1, $3, ($4 in name ? name[$4] : $4), ($1 in extra ? extra[$1] : "-"), $7
        }' pass=group "$group" pass=passwd "$passwd"
}

root=/
if [ "${1:-}" = --root ]; then
    [ "$#" -ge 2 ] || usage
    root=$2
    shift 2
fi
base=${root%/}
etc="$base/etc"
passwd="$etc/passwd" group="$etc/group" shadow="$etc/shadow"
for file in "$passwd" "$group" "$shadow"; do
    [ -r "$file" ] || die "cannot read $file" 2
done

command=${1:-}
[ "$#" -gt 0 ] && shift
case $command in
    check) [ "$#" -eq 0 ] || usage; check ;;
    list) [ "$#" -eq 0 ] || usage; list_users ;;
    *) usage ;;
esac
