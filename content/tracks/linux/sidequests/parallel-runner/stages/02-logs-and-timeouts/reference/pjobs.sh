#!/usr/bin/env bash
# Usage: pjobs.sh [-j N] [-t SECS] [-o DIR] JOBFILE - runs the commands of JOBFILE, at most N
# at a time.
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

# Seconds such as 1.5 -> microseconds, the unit of now.
micros() {
    local int=${1%%.*} frac=000000
    if [[ $1 == *.* ]]; then
        frac="${1#*.}000000"
    fi
    echo $(( 10#${int:-0} * 1000000 + 10#${frac:0:6} ))
}

now() {
    echo "${EPOCHREALTIME/./}"
}

plural() {
    if [ "$1" -eq 1 ]; then echo "1 $2"; else echo "$1 $2s"; fi
}

slots=2 timeout="" timeout_text="" outdir=""
while [ "$#" -gt 1 ]; do
    case $1 in
        -j)
            [[ $2 =~ ^[1-9][0-9]*$ ]] || die "bad job count: $2"
            slots=$2
            shift 2
            ;;
        -t)
            [[ $2 =~ ^[0-9]+(\.[0-9]+)?$ && $2 =~ [1-9] ]] || die "bad timeout: $2"
            timeout=$(micros "$2") timeout_text=$2
            shift 2
            ;;
        -o)
            outdir=$2
            shift 2
            ;;
        *) usage ;;
    esac
done
[ "$#" -eq 1 ] || usage
jobfile=$1
[ -f "$jobfile" ] && [ -r "$jobfile" ] || die "cannot read $jobfile"
if [ -n "$outdir" ]; then
    mkdir -p -- "$outdir" || die "cannot create $outdir"
fi
grace=$(micros "${PJOBS_GRACE:-2}")

cmds=()
while IFS= read -r line || [ -n "$line" ]; do
    [[ $line =~ ^[[:space:]]*(#|$) ]] && continue
    cmds+=("$line")
done < "$jobfile"

# Each job runs in a session of its own (setsid), so its pid is also its process group:
# kill -- -PID reaches every process the job started.
start_job() {
    local n=$1 out=/dev/null err=/dev/null
    if [ -n "$outdir" ]; then
        out="$outdir/$n.out" err="$outdir/$n.err"
        printf '%s\n' "${cmds[n - 1]}" > "$outdir/$n.cmd"
    fi
    PJOBS_JOB=$n setsid bash -c "${cmds[n - 1]}" < /dev/null > "$out" 2> "$err" &
    pid[n]=$! started[n]=$(now) termed[n]=""
    active+=("$n")
}

finish_job() {
    local n=$1 status=$2 cmd result=$2
    cmd=${cmds[n - 1]}
    [ -z "${termed[n]}" ] || result=timeout
    [ -z "$outdir" ] || echo "$result" > "$outdir/$n.status"
    if [ "$result" = timeout ]; then
        echo "timeout $n (after ${timeout_text}s): $cmd"
        timed_out=$((timed_out + 1))
    elif [ "$result" = 0 ]; then
        echo "ok $n: $cmd"
        ok=$((ok + 1))
    else
        echo "fail $n (exit $status): $cmd"
        failed=$((failed + 1))
    fi
}

next=1 active=() pid=() started=() termed=()
ok=0 failed=0 timed_out=0
while [ "$next" -le "${#cmds[@]}" ] || [ "${#active[@]}" -gt 0 ]; do
    while [ "${#active[@]}" -lt "$slots" ] && [ "$next" -le "${#cmds[@]}" ]; do
        start_job "$next"
        next=$((next + 1))
    done
    t=$(now)
    still=()
    for n in "${active[@]}"; do
        if kill -0 "${pid[n]}" 2> /dev/null; then
            if [ -n "$timeout" ] && [ -z "${termed[n]}" ] && (( t - started[n] >= timeout )); then
                kill -TERM -- "-${pid[n]}" 2> /dev/null
                termed[n]=$t
            elif [ -n "${termed[n]}" ] && (( t - termed[n] >= grace )); then
                kill -KILL -- "-${pid[n]}" 2> /dev/null
            fi
            still+=("$n")
        else
            wait "${pid[n]}"
            status=$?
            # What the job started may outlive it: the group goes too.
            [ -z "${termed[n]}" ] || kill -KILL -- "-${pid[n]}" 2> /dev/null
            finish_job "$n" "$status"
        fi
    done
    active=("${still[@]}")
    [ "${#active[@]}" -gt 0 ] && sleep 0.05
done

summary="$(plural "${#cmds[@]}" job): $ok ok, $failed failed"
[ "$timed_out" -eq 0 ] || summary+=", $timed_out timed out"
echo "$summary"
[ $((failed + timed_out)) -eq 0 ]
