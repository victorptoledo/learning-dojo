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

valid_name() {
    [[ $1 =~ ^[a-z_][a-z0-9_-]{0,31}$ ]]
}

# has_entry FILE NAME: FILE has a line for NAME.
has_entry() {
    awk -F: -v name="$2" '$1 == name { found = 1 } END { exit !found }' "$1"
}

login_shell() {
    grep -v '^[[:space:]]*#' "$etc/shells" | grep -qxF -- "$1"
}

# The smallest id from 1000 up that no line of FILE uses in its third field.
free_id() {
    awk -F: '{ used[$3] = 1 } END { for (id = 1000; id < 60000; id++) if (!(id in used)) { print id; exit } }' "$1"
}

today() {
    echo $(( $(date +%s) / 86400 ))
}

# replace FILE: standard input becomes the new FILE. The old one stays as FILE- (like
# shadow-utils), the mode is kept, and FILE is never half written: the new content goes to
# FILE+ and a rename puts it in place.
replace() {
    local file=$1
    if ! cat > "$file+"; then
        rm -f -- "$file+"
        die "cannot write $file+"
    fi
    chmod --reference="$file" -- "$file+"
    cp -p -- "$file" "$file-"
    mv -f -- "$file+" "$file"
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

add_user() {
    local comment="" shell=/bin/bash groups="" name uid gid home group_name
    while [ "$#" -gt 1 ]; do
        case $1 in
            -c) comment=$2 ;;
            -s) shell=$2 ;;
            -G) groups=$2 ;;
            *) usage ;;
        esac
        shift 2
    done
    [ "$#" -eq 1 ] || usage
    name=$1
    valid_name "$name" || die "bad user name: $name" 2
    case $comment in
        *:* | *$'\n'*) die "bad comment: $comment" 2 ;;
    esac

    has_entry "$passwd" "$name" && die "user $name already exists"
    has_entry "$group" "$name" && die "group $name already exists"
    login_shell "$shell" || die "$shell is not in /etc/shells"
    local -a extra=()
    [ -z "$groups" ] || IFS=, read -ra extra <<< "$groups"
    for group_name in "${extra[@]}"; do
        has_entry "$group" "$group_name" || die "no such group: $group_name"
    done
    home="$base/home/$name"
    [ ! -e "$home" ] || die "/home/$name already exists"

    uid=$(free_id "$passwd")
    if awk -F: -v id="$uid" '$3 == id { found = 1 } END { exit !found }' "$group"; then
        gid=$(free_id "$group")
    else
        gid=$uid
    fi

    { cat "$passwd"; echo "$name:x:$uid:$gid:$comment:/home/$name:$shell"; } | replace "$passwd"
    {
        awk -F: -v OFS=: -v user="$name" -v wanted=",$groups," '
            index(wanted, "," $1 ",") { $4 = ($4 == "" ? user : $4 "," user) }
            { print }' "$group"
        echo "$name:x:$gid:"
    } | replace "$group"
    { cat "$shadow"; echo "$name:!:$(today):0:99999:7:::"; } | replace "$shadow"

    # The skeleton keeps its files' modes; the home itself is private.
    mkdir -p -- "$base/home"
    cp -a -- "$etc/skel/." "$home/"
    chmod 700 -- "$home"
    if [ "$(id -u)" -eq 0 ]; then
        chown -R "$uid:$gid" -- "$home"
    fi
    echo "added $name (uid $uid, gid $gid)"
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
    add) add_user "$@" ;;
    *) usage ;;
esac
