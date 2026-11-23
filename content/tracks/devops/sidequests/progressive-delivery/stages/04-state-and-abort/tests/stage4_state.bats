bats_require_minimum_version 1.5.0

setup() {
    source "$BATS_TEST_DIRNAME/helpers.sh"
    source "$BATS_TEST_DIRNAME/fakes.sh"
    cluster
    export ROLLOUT_STATE_DIR="$BATS_TEST_TMPDIR/state"
    background=""
}

teardown() {
    rm -f "$SLEEP_GATE"
    [ -z "$background" ] || kill -9 -- "-$background" 2> /dev/null || true
}

# start_held N: starts `run` in the background, in its own process group, and waits until
# it is held in its Nth sleep (N=3: the first check of step 2 of rollout.yaml).
start_held() {
    : > "$SLEEP_GATE"
    rm -f "$SLEEP_HELD"
    SLEEP_HOLD_AT=$1 setsid bash "$repo/rollout.sh" run "$repo/rollout.yaml" \
        > "$BATS_TEST_TMPDIR/bg.out" 2> "$BATS_TEST_TMPDIR/bg.err" 3>&- &
    background=$!
    for _ in $(seq 200); do
        [ -e "$SLEEP_HELD" ] && return 0
        /usr/bin/sleep 0.05
    done
    echo "the rollout never reached sleep $1"
    cat "$BATS_TEST_TMPDIR/bg.out" "$BATS_TEST_TMPDIR/bg.err"
    return 1
}

# finish: lets the held run go on and waits for it; its exit status in $bg_status.
finish() {
    rm -f "$SLEEP_GATE"
    bg_status=0
    wait "$background" || bg_status=$?
    background=""
}

# crash: SIGKILL to the held run and every process of its group.
crash() {
    kill -9 -- "-$background"
    wait "$background" 2> /dev/null || true
    background=""
    rm -f "$SLEEP_GATE"
}

state_of() {
    jq -c '[.status, .step, .steps, .weight]' "$ROLLOUT_STATE_DIR/checkout.state"
}

@test "status before any rollout" {
    run --separate-stderr rollout status "$repo/rollout.yaml"
    assert_success
    assert_output "checkout: not started"
}

@test "a promoted rollout is saved and reported" {
    rollout run "$repo/rollout.yaml" > /dev/null
    run state_of
    assert_output '["promoted",3,3,100]'
    run --separate-stderr rollout status "$repo/rollout.yaml"
    assert_output "checkout: promoted"
    run ls -A "$ROLLOUT_STATE_DIR"
    refute_output --partial ".checkout."
}

@test "the state follows the rollout, and status sees it running" {
    start_held 3
    run state_of
    assert_output '["running",2,3,50]'
    run --separate-stderr rollout status "$repo/rollout.yaml"
    assert_output "checkout: running, step 2/3 (weight 50)"
    finish
    [ "$bg_status" -eq 0 ] || { cat "$BATS_TEST_TMPDIR/bg.out" "$BATS_TEST_TMPDIR/bg.err"; return 1; }
    run state_of
    assert_output '["promoted",3,3,100]'
}

@test "a second rollout of the same service is refused with exit 4" {
    start_held 2
    patches=$(jq -s '[.[] | select(index("patch"))] | length' "$KUBE_LOG")
    run --separate-stderr rollout run "$repo/rollout.yaml"
    assert_failure 4
    [ "$stderr" = "rollout: checkout is already running" ] || { echo "stderr: $stderr"; return 1; }
    [ "$(jq -s '[.[] | select(index("patch"))] | length' "$KUBE_LOG")" -eq "$patches" ] || { echo "the second run patched the route"; return 1; }
    finish
    [ "$bg_status" -eq 0 ]
}

@test "after a crash, status says interrupted and run resumes at that step" {
    start_held 3
    crash
    run --separate-stderr rollout status "$repo/rollout.yaml"
    assert_output "checkout: interrupted at step 2/3 (weight 50)"
    : > "$WEIGHTS_LOG"
    : > "$SLEEP_LOG"
    run --separate-stderr rollout run "$repo/rollout.yaml"
    assert_success
    assert_line --index 0 "rollout: resuming checkout at step 2/3"
    assert_line --index $((${#lines[@]} - 1)) "rollout: checkout promoted"
    run weights
    assert_output "50 50
0 100"
    run cat "$SLEEP_LOG"
    assert_output "60
60"
}

@test "the lock does not outlive a killed run" {
    start_held 1
    crash
    run --separate-stderr rollout run "$repo/rollout.yaml"
    assert_success
}

@test "a finished rollout is not run again, unless restarted" {
    rollout run "$repo/rollout.yaml" > /dev/null
    : > "$KUBE_LOG"
    run --separate-stderr rollout run "$repo/rollout.yaml"
    assert_success
    assert_output "rollout: checkout is already promoted; use run --restart to start again"
    run cat "$KUBE_LOG"
    assert_output ""
    : > "$WEIGHTS_LOG"
    run --separate-stderr rollout run --restart "$repo/rollout.yaml"
    assert_success
    run weights
    assert_output "90 10
50 50
0 100"
}

@test "a rolled back rollout says why" {
    answers 0.5
    run rollout run "$repo/rollout.yaml"
    assert_failure 3
    run state_of
    assert_output '["rolled-back",1,3,10]'
    run --separate-stderr rollout status "$repo/rollout.yaml"
    assert_output "checkout: rolled back at step 1 (error ratio 0.5 above 0.01)"
    run --separate-stderr rollout run "$repo/rollout.yaml"
    assert_success
    assert_output "rollout: checkout is already rolled back; use run --restart to start again"
}

@test "abort stops a running rollout at its next interval and rolls it back" {
    start_held 3
    run --separate-stderr rollout abort "$repo/rollout.yaml"
    assert_success
    assert_output "rollout: abort requested for checkout"
    finish
    [ "$bg_status" -eq 3 ] || { echo "run exited $bg_status"; cat "$BATS_TEST_TMPDIR/bg.out"; return 1; }
    run tail -n 1 "$BATS_TEST_TMPDIR/bg.out"
    assert_output "rollout: checkout aborted at step 2"
    run weights
    assert_line --index $((${#lines[@]} - 1)) "100 0"
    [ "$(replicas)" = 0 ]
    [ "$(wc -l < "$PROMQ_LOG")" -eq 2 ] || { echo "the run measured after the abort"; return 1; }
    run --separate-stderr rollout status "$repo/rollout.yaml"
    assert_output "checkout: aborted at step 2"
    [ ! -e "$ROLLOUT_STATE_DIR/checkout.abort" ]
}

@test "abort after a crash rolls back by itself" {
    start_held 3
    crash
    run --separate-stderr rollout abort "$repo/rollout.yaml"
    assert_success
    assert_output "rollout: checkout aborted at step 2"
    run weights
    assert_line --index $((${#lines[@]} - 1)) "100 0"
    [ "$(replicas)" = 0 ]
    run state_of
    assert_output '["aborted",2,3,50]'
}

@test "abort with nothing running fails" {
    run --separate-stderr rollout abort "$repo/rollout.yaml"
    assert_failure 1
    [ "$stderr" = "rollout: checkout is not running" ] || { echo "stderr: $stderr"; return 1; }
    rollout run "$repo/rollout.yaml" > /dev/null
    run --separate-stderr rollout abort "$repo/rollout.yaml"
    assert_failure 1
}

@test "an old abort request does not stop a new rollout" {
    mkdir -p "$ROLLOUT_STATE_DIR"
    : > "$ROLLOUT_STATE_DIR/checkout.abort"
    run --separate-stderr rollout run "$repo/rollout.yaml"
    assert_success
    assert_line --index $((${#lines[@]} - 1)) "rollout: checkout promoted"
}

@test "the state lives in .rollout by default" {
    unset ROLLOUT_STATE_DIR
    rollout run "$repo/rollout.yaml" > /dev/null
    run jq -r '.status' "$BATS_TEST_TMPDIR/.rollout/checkout.state"
    assert_output "promoted"
}
