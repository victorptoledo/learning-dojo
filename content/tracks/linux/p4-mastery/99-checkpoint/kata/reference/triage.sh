#!/usr/bin/env bash
# Usage: triage.sh [-r ROOT] - the first minutes of an incident, read from /proc, /sys and the logs.
set -uo pipefail
shopt -s nullglob
export LC_ALL=C

usage() {
    echo "usage: triage.sh [-r ROOT]" >&2
    exit 3
}

root=/
while getopts ':r:' opt; do
    case "$opt" in
        r) root="$OPTARG" ;;
        *) usage ;;
    esac
done
shift $((OPTIND - 1))
[ "$#" -eq 0 ] || usage
root="${root%/}"

crits=0
warns=0
report() {   # report LEVEL MESSAGE
    case "$1" in
        CRIT) crits=$(( crits + 1 )) ;;
        WARN) warns=$(( warns + 1 )) ;;
    esac
    echo "$1 $2"
}
ge() {   # ge A B: true when the decimal A >= B
    awk -v a="$1" -v b="$2" 'BEGIN { exit !(a >= b) }'
}

# ---- Pressure stall information: the share of time tasks waited -------------------------
psi="$root/proc/pressure"
if [ -d "$psi" ]; then
    for resource in cpu memory io; do
        [ -r "$psi/$resource" ] || continue
        read -r some full < <(awk '
            $1 == "some" || $1 == "full" {
                for (i = 2; i <= NF; i++) if ($i ~ /^avg10=/) value[$1] = substr($i, 7)
            }
            END { print ("some" in value ? value["some"] : "0.00"), ("full" in value ? value["full"] : "0.00") }
        ' "$psi/$resource")
        if [ "$resource" = cpu ]; then
            level=OK
            ge "$some" 10 && level=WARN
            report "$level" "pressure cpu: some $some"
        else
            level=OK
            ge "$some" 10 && level=WARN
            ge "$full" 10 && level=CRIT
            report "$level" "pressure $resource: some $some full $full"
        fi
    done
else
    report WARN "pressure: not available (boot with psi=1)"
fi

# ---- OOM kills in the kernel log: the victims, not the process that invoked it ------------
kills=""
if [ -r "$root/var/log/kern.log" ]; then
    kills=$(sed -nE 's/.*[Oo]ut of memory: Killed process [0-9]+ \(([^)]*)\).*/\1/p' "$root/var/log/kern.log" |
        awk '{ count[$0]++ } END { for (name in count) printf "%d\t%s\n", count[name], name }' |
        sort -t $'\t' -k1,1nr -k2,2)
fi
if [ -n "$kills" ]; then
    total=$(awk -F '\t' '{ sum += $1 } END { print sum }' <<< "$kills")
    list=$(awk -F '\t' '{ printf "%s%s x%d", (NR > 1 ? ", " : ""), $2, $1 }' <<< "$kills")
    report CRIT "oom: $total killed: $list"
else
    report OK "oom: no kills"
fi

# ---- cgroups of the services: memory limit, OOM kills inside, CPU throttling -------------
cgroup_issues=0
for dir in "$root"/sys/fs/cgroup/system.slice/*.service; do
    [ -d "$dir" ] || continue
    name=${dir##*/}
    if [ -r "$dir/memory.current" ] && [ -r "$dir/memory.max" ]; then
        read -r current < "$dir/memory.current"
        read -r max < "$dir/memory.max"
        if [[ "$max" =~ ^[0-9]+$ ]] && (( max > 0 )); then
            percent=$(( current * 100 / max ))
            if (( percent >= 90 )); then
                report WARN "cgroup $name: memory $(( current / 1048576 )) of $(( max / 1048576 )) MiB ($percent%)"
                cgroup_issues=$(( cgroup_issues + 1 ))
            fi
        fi
    fi
    if [ -r "$dir/memory.events" ]; then
        oom_kills=$(awk '$1 == "oom_kill" { print $2 }' "$dir/memory.events")
        if (( ${oom_kills:-0} > 0 )); then
            report CRIT "cgroup $name: oom kills $oom_kills"
            cgroup_issues=$(( cgroup_issues + 1 ))
        fi
    fi
    if [ -r "$dir/cpu.stat" ]; then
        read -r periods throttled < <(awk '
            $1 == "nr_periods" { p = $2 } $1 == "nr_throttled" { t = $2 }
            END { print p + 0, t + 0 }
        ' "$dir/cpu.stat")
        if (( periods > 0 )); then
            percent=$(( throttled * 100 / periods ))
            if (( percent >= 25 )); then
                report WARN "cgroup $name: throttled in $percent% of periods"
                cgroup_issues=$(( cgroup_issues + 1 ))
            fi
        fi
    fi
done
(( cgroup_issues == 0 )) && report OK "cgroups: no service near its limits"

# ---- File descriptors: near the soft limit, or holding deleted files ---------------------
fd_issues=0
mapfile -t pids < <(for dir in "$root"/proc/*/; do
    dir=${dir%/}
    [[ "${dir##*/}" =~ ^[0-9]+$ ]] && echo "${dir##*/}"
done | sort -n)
for pid in "${pids[@]}"; do
    proc="$root/proc/$pid"
    [ -r "$proc/fd" ] && [ -x "$proc/fd" ] || continue
    comm="?"
    [ -r "$proc/comm" ] && read -r comm < "$proc/comm"
    fds=("$proc"/fd/*)
    soft=""
    [ -r "$proc/limits" ] && soft=$(awk '/^Max open files/ { print $4 }' "$proc/limits")
    if [[ "$soft" =~ ^[0-9]+$ ]] && (( soft > 0 )); then
        percent=$(( ${#fds[@]} * 100 / soft ))
        if (( percent >= 80 )); then
            report WARN "fd $pid $comm: ${#fds[@]} of $soft open files ($percent%)"
            fd_issues=$(( fd_issues + 1 ))
        fi
    fi
    while IFS= read -r path; do
        [ -n "$path" ] || continue
        report WARN "fd $pid $comm: deleted file still open: $path"
        fd_issues=$(( fd_issues + 1 ))
    done < <(for fd in "${fds[@]}"; do
        target=$(readlink "$fd") || continue
        [[ "$target" == *" (deleted)" ]] && printf '%s\n' "${target% (deleted)}"
    done | sort -u)
done
(( fd_issues == 0 )) && report OK "fds: no process near its limit or holding deleted files"

# ---- Kernel and pending reboot -----------------------------------------------------------
if [ -r "$root/proc/sys/kernel/osrelease" ]; then
    read -r running < "$root/proc/sys/kernel/osrelease"
    newest=$(for image in "$root"/boot/vmlinuz-*; do echo "${image##*/vmlinuz-}"; done | sort -V | tail -n 1)
    if [ -n "$newest" ] && [ "$newest" != "$running" ] &&
        [ "$(printf '%s\n' "$running" "$newest" | sort -V | tail -n 1)" = "$newest" ]; then
        report WARN "kernel: running $running, newest installed $newest"
    else
        report OK "kernel: running $running"
    fi
fi
if [ -e "$root/run/reboot-required" ]; then
    packages=""
    [ -r "$root/run/reboot-required.pkgs" ] &&
        packages=$(sort -u "$root/run/reboot-required.pkgs" | awk 'NF { printf "%s%s", (n++ ? ", " : ""), $0 }')
    if [ -n "$packages" ]; then
        report WARN "reboot: required by $packages"
    else
        report WARN "reboot: required"
    fi
else
    report OK "reboot: not required"
fi

if (( crits > 0 )); then
    echo "status: CRIT ($crits crit, $warns warn)"
    exit 2
elif (( warns > 0 )); then
    echo "status: WARN ($warns warn)"
    exit 1
fi
echo "status: OK"
exit 0
