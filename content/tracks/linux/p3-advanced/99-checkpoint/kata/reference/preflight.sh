#!/usr/bin/env bash
# Usage: preflight.sh [-r ROOT] [-s SS_FILE] [-p PORTS] - checks a server before it goes live.
set -uo pipefail

usage() {
    echo "usage: preflight.sh [-r ROOT] [-s SS_FILE] [-p PORTS]" >&2
    exit 3
}

root=/
ss_file=""
ports=22,443
while getopts ':r:s:p:' opt; do
    case "$opt" in
        r) root="$OPTARG" ;;
        s) ss_file="$OPTARG" ;;
        p) ports="$OPTARG" ;;
        *) usage ;;
    esac
done
shift $((OPTIND - 1))
[ "$#" -eq 0 ] || usage

if [[ ! "$ports" =~ ^[0-9]+(,[0-9]+)*$ ]]; then
    echo "preflight.sh: invalid port list: $ports" >&2
    exit 3
fi
root="${root%/}"
for file in "$root/etc/ssh/sshd_config" "$root/etc/fstab" ${ss_file:+"$ss_file"}; do
    if [ ! -r "$file" ]; then
        echo "preflight.sh: cannot read $file" >&2
        exit 3
    fi
done

fails=0
warns=0
report() {   # report LEVEL MESSAGE
    case "$1" in
        FAIL) fails=$(( fails + 1 )) ;;
        WARN) warns=$(( warns + 1 )) ;;
    esac
    echo "$1 $2"
}

# ---- SSH: sshd keeps the FIRST value it reads for each keyword ----------------------
declare -A ssh=()
in_match=false
read_sshd() {   # read_sshd FILE: reads one file, following Include in place
    local line key value word path
    local -a files words
    while IFS= read -r line || [ -n "$line" ]; do
        [[ "$line" =~ ^[[:space:]]*(#|$) ]] && continue
        [[ "$line" =~ ^[[:space:]]*([A-Za-z]+)([[:space:]]*=[[:space:]]*|[[:space:]]+)(.*)$ ]] || continue
        key="${BASH_REMATCH[1],,}"
        value="${BASH_REMATCH[3]}"
        if [ "$key" = match ]; then
            in_match=true
        fi
        "$in_match" && continue
        case "$key" in
            include)
                read -ra words <<< "$value"
                for word in "${words[@]}"; do
                    if [[ "$word" == /* ]]; then
                        path="$root$word"
                    else
                        path="$root/etc/ssh/$word"
                    fi
                    mapfile -t files < <(compgen -G "$path" | LC_ALL=C sort)
                    for path in "${files[@]}"; do
                        [ -r "$path" ] && read_sshd "$path"
                    done
                done
                ;;
            permitrootlogin | passwordauthentication | kbdinteractiveauthentication)
                value="${value%%[[:space:]]*}"
                [ -n "${ssh[$key]+set}" ] || ssh[$key]="${value,,}"
                ;;
        esac
    done < "$1"
}
read_sshd "$root/etc/ssh/sshd_config"

check_ssh() {   # check_ssh KEY Name DEFAULT
    local value="${ssh[$1]:-$3}"
    if [ "$value" = no ]; then
        report PASS "ssh: $2 no"
    else
        report FAIL "ssh: $2 $value (want no)"
    fi
}
check_ssh permitrootlogin PermitRootLogin prohibit-password
check_ssh passwordauthentication PasswordAuthentication yes
check_ssh kbdinteractiveauthentication KbdInteractiveAuthentication yes

# ---- Firewall: an input base chain with policy drop ---------------------------------
inputs=0
drops=0
if [ -r "$root/etc/nftables.conf" ]; then
    read -r inputs drops < <(awk '
        function finish(body) {
            if (body ~ /hook[[:space:]]+input([[:space:]]|;)/) {
                inputs++
                if (body ~ /policy[[:space:]]+drop/) drops++
            }
        }
        {
            sub(/#.*/, "")
            if (!inchain && $0 ~ /(^|[[:space:]])chain[[:space:]]+[^[:space:]{]+[[:space:]]*\{/) {
                inchain = 1; start = depth; body = ""
            }
            if (inchain) body = body " " $0
            depth += gsub(/\{/, "{") - gsub(/\}/, "}")
            if (inchain && depth <= start) { finish(body); inchain = 0 }
        }
        END { print inputs + 0, drops + 0 }
    ' "$root/etc/nftables.conf")
fi
if (( drops > 0 )); then
    report PASS "firewall: input policy drop"
elif (( inputs > 0 )); then
    report FAIL "firewall: input policy is accept (want drop)"
else
    report FAIL "firewall: no input chain (want policy drop)"
fi

# ---- Ports: everything not on loopback is public -------------------------------------
if [ -n "$ss_file" ]; then
    listening=$(cat "$ss_file")
else
    listening=$(ss -Htln) || { echo "preflight.sh: cannot run ss" >&2; exit 3; }
fi
exposed=0
while IFS='|' read -r port addresses; do
    [ -n "$port" ] || continue
    report FAIL "port: $port open on $addresses"
    exposed=$(( exposed + 1 ))
done < <(awk -v allowed="$ports" '
    BEGIN { n = split(allowed, list, ","); for (i = 1; i <= n; i++) ok[list[i]] = 1 }
    $1 == "State" || NF < 4 { next }
    {
        local = $4
        port = local; sub(/.*:/, "", port)
        addr = local; sub(/:[^:]*$/, "", addr); sub(/%[^%]*$/, "", addr)
        host = addr; gsub(/[][]/, "", host)
        if (host ~ /^127\./ || host == "::1" || host ~ /^::ffff:127\./ || (port in ok)) next
        if (!(port in seen)) seen[port] = addr
        else if (index(", " seen[port] ", ", ", " addr ", ") == 0) seen[port] = seen[port] ", " addr
    }
    END { for (port in seen) print port "|" seen[port] }
' <<< "$listening" | sort -t'|' -k1,1n)
(( exposed == 0 )) && report PASS "port: only allowed ports are public"

# ---- fstab: stable names, and nofail on data disks -----------------------------------
fstab_issues=0
while IFS='|' read -r level message; do
    report "$level" "$message"
    fstab_issues=$(( fstab_issues + 1 ))
done < <(awk '
    /^[[:space:]]*(#|$)/ { next }
    $3 == "swap" { next }
    {
        spec = $1; mount = $2; options = $4
        if (spec ~ /^\/dev\/(sd|vd|xvd|hd|nvme)/) print "WARN|fstab: " mount " uses " spec " (use UUID= or LABEL=)"
        block = spec ~ /^(UUID|LABEL|PARTUUID|PARTLABEL)=/ || spec ~ /^\/dev\//
        if (block && mount !~ /^\/(boot(\/efi)?|usr|var)?$/ && ("," options ",") !~ /,nofail,/) print "WARN|fstab: " mount " has no nofail"
    }
' "$root/etc/fstab")
(( fstab_issues == 0 )) && report PASS "fstab: stable names and nofail on data disks"

echo "summary: $fails FAIL, $warns WARN"
if (( fails > 0 )); then
    exit 2
elif (( warns > 0 )); then
    exit 1
fi
exit 0
