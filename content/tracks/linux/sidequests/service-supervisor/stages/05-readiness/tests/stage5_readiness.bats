setup() {
    source "$BATS_TEST_DIRNAME/helpers.sh"
}

teardown() {
    stop_everything
}

@test "start waits until the service is ready" {
    app slow <<'END'
echo "$$" > "$T/pid"
sleep 0.8
touch "$T/ready"
while true; do sleep 0.1; done
END
    unit slow "ExecStart=bash $T/slow.sh" "ReadyCheck=test -e $T/ready && kill -0 \$MAINPID"
    start=$EPOCHREALTIME
    run svc start "$T/slow.service"
    assert_success
    [ -e "$T/ready" ]
    assert_output "started slow (pid $(cat "$T/pid"))"
    elapsed=$(awk -v a="$start" -v b="$EPOCHREALTIME" 'BEGIN { printf "%d", (b - a) * 1000 }')
    [ "$elapsed" -ge 700 ]
}

@test "a service that never gets ready is stopped and marked failed" {
    app never <<'END'
echo "$$" > "$T/pid"
while true; do sleep 0.1; done
END
    unit never "ExecStart=bash $T/never.sh" "ReadyCheck=false" "ReadyTimeoutSec=0.5"
    start=$SECONDS
    run --separate-stderr svc start "$T/never.service"
    assert_failure 1
    [ "$stderr" = "svc.sh: never not ready after 0.5s" ]
    [ $((SECONDS - start)) -lt 5 ]
    ! alive "$(cat "$T/pid")"
    run svc status "$T/never.service"
    assert_failure 3
    assert_line --index 0 "never: failed (not ready)"
}

@test "rejects bad readiness settings" {
    unit a "ExecStart=true" "ReadyTimeoutSec=forever"
    run --separate-stderr svc start "$T/a.service"
    assert_failure 2
    [ "$stderr" = "svc.sh: $T/a.service:3: bad value for ReadyTimeoutSec: forever" ]
}
