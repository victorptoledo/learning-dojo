setup() {
    source "$BATS_TEST_DIRNAME/helpers.sh"
}

teardown() {
    kill_leftovers
}

@test "-r retries a failing job until it succeeds" {
    # shellcheck disable=SC2016
    job='n=$(cat "$T/count" 2> /dev/null || echo 0); echo $((n + 1)) > "$T/count"; [ "$n" -ge 2 ]'
    echo "$job" > "$T/jobs"
    PJOBS_BACKOFF=0 run pjobs -r 3 "$T/jobs"
    assert_success
    assert_output "retry 1 (attempt 2 of 4): $job
retry 1 (attempt 3 of 4): $job
ok 1: $job
1 job: 1 ok, 0 failed"
}

@test "gives up after the last attempt" {
    # shellcheck disable=SC2016
    job='echo "$PJOBS_ATTEMPT" >> "$T/attempts"; exit 5'
    echo "$job" > "$T/jobs"
    PJOBS_BACKOFF=0 run pjobs -r 1 -o "$T/logs" "$T/jobs"
    assert_failure 1
    assert_output "retry 1 (attempt 2 of 2): $job
fail 1 (exit 5): $job
1 job: 0 ok, 1 failed"
    [ "$(cat "$T/attempts")" = "1
2" ]
    [ "$(cat "$T/logs/1.status")" = 5 ]
}

@test "waits longer before each new attempt" {
    # shellcheck disable=SC2016
    echo 'echo "$EPOCHREALTIME" >> "$T/times"; exit 1' > "$T/jobs"
    PJOBS_BACKOFF=0.3 run pjobs -r 2 "$T/jobs"
    assert_failure 1
    [ "$(wc -l < "$T/times")" -eq 3 ]
    run awk 'NR > 1 { printf "%d\n", ($1 - prev) * 1000 } { prev = $1 }' "$T/times"
    [ "${lines[0]}" -ge 300 ]
    [ "${lines[1]}" -ge 600 ]
    [ "${lines[1]}" -lt 3000 ]
}

@test "a job waiting for its retry does not hold a slot" {
    printf '%s\n' 'exit 1' 'true' > "$T/jobs"
    PJOBS_BACKOFF=1 run pjobs -j 1 -r 1 "$T/jobs"
    assert_failure 1
    assert_output "retry 1 (attempt 2 of 2): exit 1
ok 2: true
fail 1 (exit 1): exit 1
2 jobs: 1 ok, 1 failed"
}

@test "a retry goes before the jobs that have not started" {
    printf '%s\n' 'exit 1' 'sleep 0.5' 'true' > "$T/jobs"
    PJOBS_BACKOFF=0 run pjobs -j 1 -r 1 "$T/jobs"
    assert_failure 1
    assert_output "retry 1 (attempt 2 of 2): exit 1
fail 1 (exit 1): exit 1
ok 2: sleep 0.5
ok 3: true
3 jobs: 2 ok, 1 failed"
}

@test "timeouts are retried too" {
    echo 'sleep 30' > "$T/jobs"
    PJOBS_BACKOFF=0 run pjobs -t 0.3 -r 1 "$T/jobs"
    assert_failure 1
    assert_output "retry 1 (attempt 2 of 2): sleep 30
timeout 1 (after 0.3s): sleep 30
1 job: 0 ok, 0 failed, 1 timed out"
}

@test "--resume skips the jobs that already succeeded" {
    printf '%s\n' 'echo one >> "$T/ran"' '[ -e "$T/fixed" ]' 'echo three >> "$T/ran"' > "$T/jobs"
    run pjobs -j 1 -o "$T/logs" "$T/jobs"
    assert_failure 1
    touch "$T/fixed"
    run pjobs -j 1 -o "$T/logs" --resume "$T/jobs"
    assert_success
    assert_output "skip 1: echo one >> \"\$T/ran\"
skip 3: echo three >> \"\$T/ran\"
ok 2: [ -e \"\$T/fixed\" ]
3 jobs: 1 ok, 0 failed, 2 skipped"
    [ "$(cat "$T/ran")" = "one
three" ]
}

@test "--resume runs again a job whose line changed or that was stopped" {
    printf '%s\n' 'echo a' 'echo b' > "$T/jobs"
    run pjobs -j 1 -o "$T/logs" "$T/jobs"
    assert_success
    echo stopped > "$T/logs/2.status"
    printf '%s\n' 'echo A' 'echo b' > "$T/jobs"
    run pjobs -j 1 -o "$T/logs" --resume "$T/jobs"
    assert_success
    assert_output "ok 1: echo A
ok 2: echo b
2 jobs: 2 ok, 0 failed"
}

@test "rejects bad retry options" {
    printf '%s\n' 'true' > "$T/jobs"
    run --separate-stderr pjobs -r many "$T/jobs"
    assert_failure 2
    [ "$stderr" = "pjobs.sh: bad retry count: many" ]
    run --separate-stderr pjobs --resume "$T/jobs"
    assert_failure 2
    [ "$stderr" = "usage: pjobs.sh [-j N] [-t SECS] [-r N] [-o DIR [--resume]] JOBFILE" ]
}
