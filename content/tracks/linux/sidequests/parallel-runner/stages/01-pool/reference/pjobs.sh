#!/usr/bin/env bash
# Usage: pjobs.sh [-j N] JOBFILE - runs the commands of JOBFILE, at most N at a time.
set -uo pipefail
export LC_ALL=C

usage() {
    echo "usage: pjobs.sh [-j N] [-t SECS] [-r N] [-o DIR [--resume]] JOBFILE" >&2
    exit 2
}

die() {
    echo "pjobs.sh: $*" >&2
    exit 2
}

plural() {
    if [ "$1" -eq 1 ]; then echo "1 $2"; else echo "$1 $2s"; fi
}

slots=2
while [ "$#" -gt 1 ]; do
    case $1 in
        -j)
            [[ $2 =~ ^[1-9][0-9]*$ ]] || die "bad job count: $2"
            slots=$2
            shift 2
            ;;
        *) usage ;;
    esac
done
[ "$#" -eq 1 ] || usage
jobfile=$1
[ -f "$jobfile" ] && [ -r "$jobfile" ] || die "cannot read $jobfile"

cmds=()
while IFS= read -r line || [ -n "$line" ]; do
    [[ $line =~ ^[[:space:]]*(#|$) ]] && continue
    cmds+=("$line")
done < "$jobfile"

next=1 active=() pid=() ok=0 failed=0
while [ "$next" -le "${#cmds[@]}" ] || [ "${#active[@]}" -gt 0 ]; do
    while [ "${#active[@]}" -lt "$slots" ] && [ "$next" -le "${#cmds[@]}" ]; do
        PJOBS_JOB=$next bash -c "${cmds[next - 1]}" < /dev/null > /dev/null 2>&1 &
        pid[next]=$!
        active+=("$next")
        next=$((next + 1))
    done
    still=()
    for n in "${active[@]}"; do
        if kill -0 "${pid[n]}" 2> /dev/null; then
            still+=("$n")
            continue
        fi
        wait "${pid[n]}"
        status=$?
        if [ "$status" -eq 0 ]; then
            echo "ok $n: ${cmds[n - 1]}"
            ok=$((ok + 1))
        else
            echo "fail $n (exit $status): ${cmds[n - 1]}"
            failed=$((failed + 1))
        fi
    done
    active=("${still[@]}")
    [ "${#active[@]}" -gt 0 ] && sleep 0.05
done

echo "$(plural "${#cmds[@]}" job): $ok ok, $failed failed"
[ "$failed" -eq 0 ]
