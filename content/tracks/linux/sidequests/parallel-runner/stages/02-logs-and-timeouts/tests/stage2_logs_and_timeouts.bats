setup() {
    source "$BATS_TEST_DIRNAME/helpers.sh"
}

teardown() {
    kill_leftovers
}

@test "-o keeps each job's output, command and exit status" {
    printf '%s\n' 'echo to-out; echo to-err >&2' 'exit 4' > "$T/jobs"
    run pjobs -j 1 -o "$T/logs/run-1" "$T/jobs"
    assert_failure 1
    assert_output "ok 1: echo to-out; echo to-err >&2
fail 2 (exit 4): exit 4
2 jobs: 1 ok, 1 failed"
    [ "$(cat "$T/logs/run-1/1.out")" = "to-out" ]
    [ "$(cat "$T/logs/run-1/1.err")" = "to-err" ]
    [ "$(cat "$T/logs/run-1/1.cmd")" = "echo to-out; echo to-err >&2" ]
    [ "$(cat "$T/logs/run-1/1.status")" = 0 ]
    [ "$(cat "$T/logs/run-1/2.status")" = 4 ]
    [ ! -s "$T/logs/run-1/2.out" ]
}

@test "-t stops a job that runs too long" {
    printf '%s\n' 'sleep 30' 'true' > "$T/jobs"
    start=$SECONDS
    run pjobs -j 1 -t 0.5 -o "$T/logs" "$T/jobs"
    assert_failure 1
    assert_output "timeout 1 (after 0.5s): sleep 30
ok 2: true
2 jobs: 1 ok, 0 failed, 1 timed out"
    [ "$(cat "$T/logs/1.status")" = timeout ]
    [ $((SECONDS - start)) -lt 6 ]
}

@test "a job that ends in time is not a timeout" {
    printf '%s\n' 'sleep 0.2' > "$T/jobs"
    run pjobs -t 3 "$T/jobs"
    assert_success
    assert_output "ok 1: sleep 0.2
1 job: 1 ok, 0 failed"
}

@test "a timeout kills every process the job started" {
    # shellcheck disable=SC2016
    printf '%s\n' 'sleep 30 & echo $! > "$T/child"; wait' > "$T/jobs"
    run pjobs -t 0.5 "$T/jobs"
    assert_failure 1
    assert_line --index 0 --partial "timeout 1 (after 0.5s): "
    child=$(cat "$T/child")
    sleep 0.2
    if alive "$child"; then
        echo "the job's child $child is still running" >&2
        return 1
    fi
}

@test "a job that ignores SIGTERM is killed after the grace period" {
    # shellcheck disable=SC2016
    printf '%s\n' 'trap "" TERM; sleep 30 & echo $! > "$T/child"; wait; sleep 30' > "$T/jobs"
    start=$SECONDS
    PJOBS_GRACE=0.5 run pjobs -t 0.3 -o "$T/logs" "$T/jobs"
    assert_failure 1
    [ "$(cat "$T/logs/1.status")" = timeout ]
    [ $((SECONDS - start)) -lt 8 ]
    sleep 0.2
    ! alive "$(cat "$T/child")"
}

@test "timeouts and the pool work together" {
    printf '%s\n' 'sleep 30' 'sleep 30' 'sleep 0.1' 'sleep 0.1' > "$T/jobs"
    run pjobs -j 2 -t 0.6 "$T/jobs"
    assert_failure 1
    assert_line --index 4 "4 jobs: 2 ok, 0 failed, 2 timed out"
}

@test "rejects a bad timeout" {
    printf '%s\n' 'true' > "$T/jobs"
    run --separate-stderr pjobs -t soon "$T/jobs"
    assert_failure 2
    [ "$stderr" = "pjobs.sh: bad timeout: soon" ]
    run --separate-stderr pjobs -t 0 "$T/jobs"
    assert_failure 2
    [ "$stderr" = "pjobs.sh: bad timeout: 0" ]
}
