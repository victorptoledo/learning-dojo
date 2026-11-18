setup() {
    source "$BATS_TEST_DIRNAME/helpers.sh"
}

teardown() {
    kill_leftovers
}

# stop_runner: SIGTERM to the background runner, then its exit status in $status.
stop_runner() {
    kill -TERM "$runner"
    status=0
    timeout 10 tail --pid="$runner" -f /dev/null || true
    wait "$runner" || status=$?
}

@test "SIGTERM stops the running jobs and starts no new one" {
    # shellcheck disable=SC2016
    job='sleep 30 & echo $! > "$T/child.$PJOBS_JOB"; wait'
    for _ in 1 2 3 4 5; do echo "$job"; done > "$T/jobs"
    pjobs_in_background -j 2 -o "$T/logs" "$T/jobs"
    wait_for "$T/child.1" "$T/child.2"
    stop_runner
    [ "$status" -eq 143 ]
    run sort "$T/log"
    assert_output "interrupted: 2 stopped, 3 not started
stopped 1: $job
stopped 2: $job"
    [ "$(tail -n 1 "$T/log")" = "interrupted: 2 stopped, 3 not started" ]
    [ "$(cat "$T/logs/1.status")" = stopped ]
    [ "$(cat "$T/logs/2.status")" = stopped ]
    [ ! -e "$T/child.3" ]
    [ ! -e "$T/logs/3.cmd" ]
}

@test "leaves no process of any job behind" {
    # shellcheck disable=SC2016
    job='echo $$ > "$T/shell.$PJOBS_JOB"; sleep 30 & echo $! > "$T/child.$PJOBS_JOB"; wait'
    for _ in 1 2 3; do echo "$job"; done > "$T/jobs"
    pjobs_in_background -j 3 "$T/jobs"
    wait_for "$T/child.1" "$T/child.2" "$T/child.3"
    stop_runner
    [ "$status" -eq 143 ]
    sleep 0.2
    for pidfile in "$T"/shell.* "$T"/child.*; do
        if alive "$(cat "$pidfile")"; then
            echo "process $(cat "$pidfile") from $pidfile is still running" >&2
            return 1
        fi
    done
}

@test "when stopping, a job that ignores SIGTERM is killed after the grace period" {
    # shellcheck disable=SC2016
    printf '%s\n' 'trap "" TERM; sleep 30 & echo $! > "$T/child"; wait; sleep 30' > "$T/jobs"
    PJOBS_GRACE=0.5 pjobs_in_background "$T/jobs"
    wait_for "$T/child"
    start=$SECONDS
    stop_runner
    [ "$status" -eq 143 ]
    [ $((SECONDS - start)) -lt 5 ]
    [ "$(tail -n 1 "$T/log")" = "interrupted: 1 stopped, 0 not started" ]
    sleep 0.2
    ! alive "$(cat "$T/child")"
}

@test "reacts quickly even while every slot is busy" {
    printf '%s\n' 'touch "$T/started"; sleep 60' 'sleep 60' > "$T/jobs"
    pjobs_in_background -j 1 "$T/jobs"
    wait_for "$T/started"
    sleep 0.3
    start=$EPOCHREALTIME
    stop_runner
    [ "$status" -eq 143 ]
    elapsed=$(awk -v a="$start" -v b="$EPOCHREALTIME" 'BEGIN { printf "%d", (b - a) * 1000 }')
    [ "$elapsed" -lt 1500 ]
    [ "$(tail -n 1 "$T/log")" = "interrupted: 1 stopped, 1 not started" ]
}

@test "a run that is not interrupted is unchanged" {
    printf '%s\n' 'true' 'true' > "$T/jobs"
    run pjobs "$T/jobs"
    assert_success
    assert_line --index 2 "2 jobs: 2 ok, 0 failed"
}
