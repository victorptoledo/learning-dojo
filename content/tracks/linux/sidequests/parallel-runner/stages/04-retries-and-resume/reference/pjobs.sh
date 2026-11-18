#!/usr/bin/env bash
# Usage: pjobs.sh [-j N] [-t SECS] [-r N] [-o DIR [--resume]] JOBFILE - runs the commands of
# JOBFILE, at most N at a time.
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

slots=2 timeout="" timeout_text="" retries=0 outdir="" resume=false
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
        -r)
            [[ $2 =~ ^[0-9]+$ ]] || die "bad retry count: $2"
            retries=$2
            shift 2
            ;;
        -o)
            outdir=$2
            shift 2
            ;;
        --resume)
            resume=true
            shift
            ;;
        *) usage ;;
    esac
done
[ "$#" -eq 1 ] || usage
[ "$resume" = false ] || [ -n "$outdir" ] || usage
jobfile=$1
[ -f "$jobfile" ] && [ -r "$jobfile" ] || die "cannot read $jobfile"
if [ -n "$outdir" ]; then
    mkdir -p -- "$outdir" || die "cannot create $outdir"
fi
grace=$(micros "${PJOBS_GRACE:-2}")
backoff=$(micros "${PJOBS_BACKOFF:-1}")

cmds=()
while IFS= read -r line || [ -n "$line" ]; do
    [[ $line =~ ^[[:space:]]*(#|$) ]] && continue
    cmds+=("$line")
done < "$jobfile"

queue=() active=()
pid=() started=() termed=() attempt=() not_before=()
ok=0 failed=0 timed_out=0 skipped=0 stopped=0
for ((n = 1; n <= ${#cmds[@]}; n++)); do
    if [ "$resume" = true ] && [ "$(cat "$outdir/$n.status" 2> /dev/null)" = 0 ] &&
        [ "$(cat "$outdir/$n.cmd" 2> /dev/null)" = "${cmds[n - 1]}" ]; then
        echo "skip $n: ${cmds[n - 1]}"
        skipped=$((skipped + 1))
        continue
    fi
    queue+=("$n")
    not_before[n]=0
done

# Each job runs in a session of its own (setsid), so its pid is also its process group:
# kill -- -PID reaches every process the job started.
start_job() {
    local n=$1 out=/dev/null err=/dev/null
    attempt[n]=$(( ${attempt[n]:-0} + 1 ))
    if [ -n "$outdir" ]; then
        out="$outdir/$n.out" err="$outdir/$n.err"
        printf '%s\n' "${cmds[n - 1]}" > "$outdir/$n.cmd"
        rm -f -- "$outdir/$n.status"
    fi
    PJOBS_JOB=$n PJOBS_ATTEMPT=${attempt[n]} setsid bash -c "${cmds[n - 1]}" < /dev/null > "$out" 2> "$err" &
    pid[n]=$! started[n]=$(now) termed[n]=""
    active+=("$n")
}

finish_job() {
    local n=$1 status=$2 cmd result
    cmd=${cmds[n - 1]}
    if [ "$stopping" = true ]; then
        result=stopped
    elif [ -n "${termed[n]}" ]; then
        result=timeout
    else
        result=$status
    fi
    [ -z "$outdir" ] || echo "$result" > "$outdir/$n.status"
    if [ "$result" = stopped ]; then
        echo "stopped $n: $cmd"
        stopped=$((stopped + 1))
    elif [ "$result" = 0 ]; then
        echo "ok $n: $cmd"
        ok=$((ok + 1))
    elif [ "${attempt[n]}" -le "$retries" ]; then
        echo "retry $n (attempt $(( attempt[n] + 1 )) of $(( retries + 1 ))): $cmd"
        not_before[n]=$(( $(now) + backoff * (1 << (attempt[n] - 1)) ))
        queue=("$n" "${queue[@]}")
    elif [ "$result" = timeout ]; then
        echo "timeout $n (after ${timeout_text}s): $cmd"
        timed_out=$((timed_out + 1))
    else
        echo "fail $n (exit $status): $cmd"
        failed=$((failed + 1))
    fi
}

# The first queued job whose retry delay is over, removed from the queue.
next_job() {
    local i t
    t=$(now)
    for i in "${!queue[@]}"; do
        if (( not_before[queue[i]] <= t )); then
            next=${queue[i]}
            unset 'queue[i]'
            queue=("${queue[@]}")
            return 0
        fi
    done
    return 1
}

stopping=false stop_sent=false exit_status=0 not_started=0
trap 'stopping=true exit_status=143' TERM
trap 'stopping=true exit_status=130' INT

while [ "${#queue[@]}" -gt 0 ] || [ "${#active[@]}" -gt 0 ]; do
    if [ "$stopping" = true ] && [ "$stop_sent" = false ]; then
        stop_sent=true
        not_started=${#queue[@]}
        queue=()
        for n in "${active[@]}"; do
            kill -TERM -- "-${pid[n]}" 2> /dev/null
            termed[n]=$(now)
        done
    fi
    while [ "$stopping" = false ] && [ "${#active[@]}" -lt "$slots" ] && next_job; do
        start_job "$next"
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
            [ -z "${termed[n]}" ] || kill -KILL -- "-${pid[n]}" 2> /dev/null
            finish_job "$n" "$status"
        fi
    done
    active=("${still[@]}")
    [ "${#queue[@]}" -eq 0 ] && [ "${#active[@]}" -eq 0 ] && break
    sleep 0.05
done

if [ "$stopping" = true ]; then
    echo "interrupted: $stopped stopped, $not_started not started"
    exit "$exit_status"
fi
summary="$(plural "${#cmds[@]}" job): $ok ok, $failed failed"
[ "$timed_out" -eq 0 ] || summary+=", $timed_out timed out"
[ "$skipped" -eq 0 ] || summary+=", $skipped skipped"
echo "$summary"
[ $((failed + timed_out)) -eq 0 ]
