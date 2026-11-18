setup() {
    source "$BATS_TEST_DIRNAME/helpers.sh"
}

teardown() {
    kill_leftovers
}

@test "runs every job and reports how each one ended" {
    printf '%s\n' 'true' 'exit 3' 'echo hidden output' > "$T/jobs"
    run --separate-stderr pjobs -j 1 "$T/jobs"
    assert_failure 1
    assert_output "ok 1: true
fail 2 (exit 3): exit 3
ok 3: echo hidden output
3 jobs: 2 ok, 1 failed"
    [ -z "$stderr" ]
}

@test "exits 0 when every job succeeds" {
    printf '%s\n' 'test 1 -eq 1' > "$T/jobs"
    run pjobs "$T/jobs"
    assert_success
    assert_output "ok 1: test 1 -eq 1
1 job: 1 ok, 0 failed"
}

@test "skips empty lines and comments when numbering jobs" {
    printf '%s\n' '# setup' '' '   # indented comment' 'true' '   ' 'false' > "$T/jobs"
    run pjobs -j 1 "$T/jobs"
    assert_failure 1
    assert_output "ok 1: true
fail 2 (exit 1): false
2 jobs: 1 ok, 1 failed"
}

@test "never runs more than N jobs at once" {
    mkdir "$T/running"
    # shellcheck disable=SC2016
    job='touch "$T/running/$PJOBS_JOB"; sleep 0.4; ls "$T/running" | wc -l >> "$T/peak"; rm "$T/running/$PJOBS_JOB"'
    for _ in 1 2 3 4 5 6 7; do echo "$job"; done > "$T/jobs"
    run pjobs -j 3 "$T/jobs"
    assert_success
    [ "$(sort -n "$T/peak" | tail -n 1)" -eq 3 ]
    rm "$T/peak"
    run pjobs "$T/jobs"
    assert_success
    [ "$(sort -n "$T/peak" | tail -n 1)" -eq 2 ]
}

@test "starts the next job as soon as a slot is free" {
    printf '%s\n' 'sleep 1.5' 'sleep 0.2' 'sleep 0.2' 'sleep 0.2' > "$T/jobs"
    run pjobs -j 2 "$T/jobs"
    assert_success
    assert_output "ok 2: sleep 0.2
ok 3: sleep 0.2
ok 4: sleep 0.2
ok 1: sleep 1.5
4 jobs: 4 ok, 0 failed"
}

@test "each job knows its number and reads nothing from stdin" {
    # shellcheck disable=SC2016
    printf '%s\n' 'echo "$PJOBS_JOB" > "$T/number.$PJOBS_JOB"' '# not a job' 'cat > "$T/stdin"' > "$T/jobs"
    run pjobs "$T/jobs" <<< "secret input"
    assert_success
    [ "$(cat "$T/number.1")" = 1 ]
    [ -e "$T/stdin" ]
    [ ! -s "$T/stdin" ]
}

@test "rejects bad arguments" {
    printf '%s\n' 'true' > "$T/jobs"
    usage="usage: pjobs.sh [-j N] [-t SECS] [-r N] [-o DIR [--resume]] JOBFILE"
    run --separate-stderr pjobs
    assert_failure 2
    [ "$stderr" = "$usage" ]
    run --separate-stderr pjobs -x "$T/jobs"
    assert_failure 2
    [ "$stderr" = "$usage" ]
    run --separate-stderr pjobs -j 0 "$T/jobs"
    assert_failure 2
    [ "$stderr" = "pjobs.sh: bad job count: 0" ]
    run --separate-stderr pjobs -j two "$T/jobs"
    assert_failure 2
    [ "$stderr" = "pjobs.sh: bad job count: two" ]
    run --separate-stderr pjobs "$T/missing"
    assert_failure 2
    [ "$stderr" = "pjobs.sh: cannot read $T/missing" ]
}
