setup() {
    bats_load_library bats-support
    bats_load_library bats-assert
    bats_load_library bats-file
}

make_queue() {
    queue="$BATS_TEST_TMPDIR/queue"
    done_dir="$BATS_TEST_TMPDIR/done"
    mkdir -p "$queue" "$done_dir"
    local i
    for i in $(seq -w 1 "$1"); do
        echo "job number $i" > "$queue/job-$i"
    done
}

@test "processes every job in name order and cleans up" {
    make_queue 3
    WORKER_DELAY=0 run bash worker.sh --once "$queue" "$done_dir"
    assert_success
    assert_output "done job-1
done job-2
done job-3"
    [ "$(cat "$done_dir/job-2")" = "JOB NUMBER 2" ]
    [ -z "$(ls -A "$queue")" ]
    [ -z "$(find "$done_dir" -name '.*')" ]
}

@test "ignores hidden files in the queue" {
    make_queue 1
    echo "keep me" > "$queue/.keep"
    WORKER_DELAY=0 run bash worker.sh --once "$queue" "$done_dir"
    assert_success
    assert_output "done job-1"
    [ -f "$queue/.keep" ]
}

@test "refuses to start when another worker is alive" {
    make_queue 1
    echo "$$" > "$queue/.worker.pid"
    WORKER_DELAY=0 run --separate-stderr bash worker.sh --once "$queue" "$done_dir"
    assert_failure 1
    [ "$stderr" = "worker.sh: already running (pid $$)" ]
    [ "$(cat "$queue/.worker.pid")" = "$$" ]
    [ -f "$queue/job-1" ]
}

@test "replaces a stale pid file" {
    make_queue 1
    echo 999999 > "$queue/.worker.pid"
    WORKER_DELAY=0 run --separate-stderr bash worker.sh --once "$queue" "$done_dir"
    assert_success
    assert_output "done job-1"
    [ "$stderr" = "worker.sh: removing stale pid file" ]
    [ ! -e "$queue/.worker.pid" ]
}

@test "writes its pid file while running" {
    make_queue 1
    WORKER_DELAY=0.2 bash worker.sh "$queue" "$done_dir" > "$BATS_TEST_TMPDIR/out" 2>&1 3>&- &
    pid=$!
    sleep 0.6
    [ "$(cat "$queue/.worker.pid")" = "$pid" ]
    kill -TERM "$pid"
    wait "$pid"
    [ ! -e "$queue/.worker.pid" ]
}

@test "stops cleanly on SIGTERM between jobs" {
    make_queue 30
    WORKER_DELAY=0.3 bash worker.sh "$queue" "$done_dir" > "$BATS_TEST_TMPDIR/out" 2>&1 3>&- &
    pid=$!
    sleep 1
    kill -TERM "$pid"
    status=0
    wait "$pid" || status=$?
    [ "$status" -eq 0 ]
    [ "$(tail -n 1 "$BATS_TEST_TMPDIR/out")" = "stopping" ]
    processed=$(find "$done_dir" -type f ! -name '.*' | wc -l)
    left=$(find "$queue" -type f ! -name '.*' | wc -l)
    [ "$processed" -gt 0 ]
    [ "$left" -gt 0 ]
    [ $(( processed + left )) -eq 30 ]
    [ -z "$(find "$done_dir" -name '.*')" ]
    [ ! -e "$queue/.worker.pid" ]
    for file in "$done_dir"/*; do
        [ ! -e "$queue/${file##*/}" ]
    done
}

@test "keeps polling without --once and picks up new jobs" {
    queue="$BATS_TEST_TMPDIR/queue"
    done_dir="$BATS_TEST_TMPDIR/done"
    mkdir -p "$queue" "$done_dir"
    WORKER_DELAY=0.2 bash worker.sh "$queue" "$done_dir" > "$BATS_TEST_TMPDIR/out" 2>&1 3>&- &
    pid=$!
    sleep 0.5
    echo "late" > "$queue/late-job"
    sleep 1
    kill -TERM "$pid"
    wait "$pid"
    [ "$(cat "$done_dir/late-job")" = "LATE" ]
    run cat "$BATS_TEST_TMPDIR/out"
    assert_output "done late-job
stopping"
}

@test "rejects bad arguments" {
    mkdir -p "$BATS_TEST_TMPDIR/q"
    run --separate-stderr bash worker.sh "$BATS_TEST_TMPDIR/q"
    assert_failure 2
    [ "$stderr" = "usage: worker.sh [--once] QUEUE DONE" ]
    run --separate-stderr bash worker.sh --once "$BATS_TEST_TMPDIR/q" "$BATS_TEST_TMPDIR/missing"
    assert_failure 2
    [ "$stderr" = "worker.sh: not a directory: $BATS_TEST_TMPDIR/missing" ]
}
