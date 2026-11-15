setup() {
    bats_load_library bats-support
    bats_load_library bats-assert
    bats_load_library bats-file
}

@test "processes one job with --once" {
    mkdir -p "$BATS_TEST_TMPDIR/queue" "$BATS_TEST_TMPDIR/done"
    echo "hello" > "$BATS_TEST_TMPDIR/queue/job1"
    WORKER_DELAY=0 run bash worker.sh --once "$BATS_TEST_TMPDIR/queue" "$BATS_TEST_TMPDIR/done"
    assert_success
    assert_output "done job1"
    [ "$(cat "$BATS_TEST_TMPDIR/done/job1")" = "HELLO" ]
}
