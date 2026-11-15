#!/usr/bin/env bash
# Usage: health.sh [-p PROC] [-d DF_FILE] - prints memory, load and disk health.
set -uo pipefail

usage() {
    echo "usage: health.sh [-p PROC] [-d DF_FILE]" >&2
    exit 2
}

proc=/proc
df_file=""
while getopts ':p:d:' opt; do
    case "$opt" in
        p) proc="$OPTARG" ;;
        d) df_file="$OPTARG" ;;
        *) usage ;;
    esac
done
shift $((OPTIND - 1))
[ "$#" -eq 0 ] || usage

for file in "$proc/meminfo" "$proc/loadavg" "$proc/cpuinfo" ${df_file:+"$df_file"}; do
    if [ ! -r "$file" ]; then
        echo "health.sh: cannot read $file" >&2
        exit 2
    fi
done

warnings=0
# Prints " WARN" and counts it when VALUE >= THRESHOLD (decimals allowed).
mark() {
    if awk -v v="$1" -v t="$2" 'BEGIN { exit !(v >= t) }'; then
        warnings=$(( warnings + 1 ))
        printf ' WARN'
    fi
}

read -r used used_mib total_mib < <(awk '
    /^MemTotal:/ { total = $2 }
    /^MemAvailable:/ { available = $2 }
    END { printf "%d %d %d\n", int((total - available) * 100 / total), int((total - available) / 1024), int(total / 1024) }
' "$proc/meminfo")
printf 'memory: %s%% used (%s of %s MiB)' "$used" "$used_mib" "$total_mib"
mark "$used" 90
printf '\n'

read -r load _ < "$proc/loadavg"
cpus=$(grep -c '^processor' "$proc/cpuinfo")
per_cpu=$(awk -v l="$load" -v c="$cpus" 'BEGIN { printf "%.2f", l / c }')
printf 'load: %s on %s cpus (%s per cpu)' "$load" "$cpus" "$per_cpu"
mark "$per_cpu" 1
printf '\n'

if [ -n "$df_file" ]; then
    df_output=$(cat "$df_file")
else
    df_output=$(df -P)
fi
while read -r mount percent; do
    printf 'disk %s: %s%% used' "$mount" "$percent"
    mark "$percent" 90
    printf '\n'
done < <(awk 'NR > 1 && $1 !~ /^(tmpfs|devtmpfs|udev)$/ { sub(/%/, "", $5); print $6, $5 }' <<< "$df_output")

if [ "$warnings" -eq 0 ]; then
    echo "status: OK"
    exit 0
fi
if [ "$warnings" -eq 1 ]; then
    echo "status: WARN (1 check)"
else
    echo "status: WARN ($warnings checks)"
fi
exit 1
