setup() {
    source "$BATS_TEST_DIRNAME/helpers.sh"
}

teardown() {
    stop_everything
}

@test "start runs the service in the background and prints its pid" {
    polite_app web
    unit web "ExecStart=bash $T/web.sh"
    start=$SECONDS
    run --separate-stderr svc start "$T/web.service"
    assert_success
    [ $((SECONDS - start)) -lt 4 ]
    wait_until '[ -s "$T/pid" ]'
    assert_output "started web (pid $(cat "$T/pid"))"
    sleep 0.3
    alive "$(cat "$T/pid")"
}

@test "status tells whether the service runs" {
    polite_app web
    unit web "ExecStart=bash $T/web.sh"
    run svc status "$T/web.service"
    assert_failure 3
    assert_line --index 0 "web: inactive"
    run svc start "$T/web.service"
    assert_success
    wait_until '[ -s "$T/pid" ]'
    run svc status "$T/web.service"
    assert_success
    assert_line --index 0 "web: active (running), pid $(cat "$T/pid")"
}

@test "stop sends SIGTERM and waits for the service" {
    polite_app web
    unit web "ExecStart=bash $T/web.sh"
    svc start "$T/web.service"
    wait_until '[ -s "$T/pid" ]'
    pid=$(cat "$T/pid")
    run svc stop "$T/web.service"
    assert_success
    assert_output "stopped web"
    ! alive "$pid"
    grep -q 'got TERM$' "$SVC_STATE/web.log"
    run svc status "$T/web.service"
    assert_failure 3
    assert_line --index 0 "web: inactive (stopped)"
    run svc stop "$T/web.service"
    assert_success
    assert_output "web is not running"
}

@test "starting a running service changes nothing" {
    polite_app web
    unit web "ExecStart=bash $T/web.sh"
    svc start "$T/web.service"
    wait_until '[ -s "$T/pid" ]'
    pid=$(cat "$T/pid")
    run svc start "$T/web.service"
    assert_success
    assert_output "web is already running (pid $pid)"
    [ "$(pgrep -cf -- "$T/web.sh")" -eq 1 ]
}

@test "the service's stdout and stderr go to its log" {
    polite_app web
    unit web "ExecStart=bash $T/web.sh"
    svc start "$T/web.service"
    wait_until 'grep -q oops "$SVC_STATE/web.log"'
    grep -q hello "$SVC_STATE/web.log"
}

@test "status reports how a service exited" {
    app job <<'END'
echo "$$" > "$T/pid"
exit 4
END
    unit job "ExecStart=bash $T/job.sh"
    run svc start "$T/job.service"
    assert_success
    assert_output --regexp '^started job \(pid [0-9]+\)$'
    wait_until '! svc status "$T/job.service" > /dev/null'
    status_line "$T/job.service"
    [ "$first_line" = "job: inactive (exited 4)" ]
}

@test "a service that ignores SIGTERM is killed after TimeoutStopSec" {
    app stubborn <<'END'
echo "$$" > "$T/pid"
trap '' TERM
while true; do sleep 0.1; done
END
    unit stubborn "ExecStart=bash $T/stubborn.sh" "TimeoutStopSec=0.5"
    svc start "$T/stubborn.service"
    wait_until '[ -s "$T/pid" ]'
    start=$SECONDS
    run svc stop "$T/stubborn.service"
    assert_success
    assert_output "stopped stubborn"
    [ $((SECONDS - start)) -lt 4 ]
    ! alive "$(cat "$T/pid")"
}

@test "stop ends every process of the service" {
    app forks <<'END'
sleep 300 &
echo $! > "$T/child"
wait
END
    unit forks "ExecStart=bash $T/forks.sh"
    svc start "$T/forks.service"
    wait_until '[ -s "$T/child" ]'
    run svc stop "$T/forks.service"
    assert_success
    sleep 0.2
    ! alive "$(cat "$T/child")"
}

@test "reads comments, a [Unit] section and CRLF line ends" {
    polite_app web
    printf '%s\r\n' "# the web app" "[Unit]" "Description=Web app" "" "[Service]" "; old style comment" "ExecStart=bash $T/web.sh" > "$T/web.service"
    run svc start "$T/web.service"
    assert_success
    wait_until '[ -s "$T/pid" ]'
}

@test "rejects broken units" {
    run --separate-stderr svc start "$T/missing.service"
    assert_failure 2
    [ "$stderr" = "svc.sh: cannot read $T/missing.service" ]
    echo "[Service]" > "$T/web.conf"
    run --separate-stderr svc start "$T/web.conf"
    assert_failure 2
    [ "$stderr" = "svc.sh: $T/web.conf: not a .service file" ]
    unit a "TimeoutStopSec=1"
    run --separate-stderr svc start "$T/a.service"
    assert_failure 2
    [ "$stderr" = "svc.sh: $T/a.service: missing ExecStart" ]
    unit b "ExecStart=true" "Colour=blue"
    run --separate-stderr svc start "$T/b.service"
    assert_failure 2
    [ "$stderr" = "svc.sh: $T/b.service:3: unknown key Colour" ]
    unit c "ExecStart=true" "just words"
    run --separate-stderr svc status "$T/c.service"
    assert_failure 2
    [ "$stderr" = "svc.sh: $T/c.service:3: expected KEY=VALUE" ]
    unit d "ExecStart=true" "TimeoutStopSec=soon"
    run --separate-stderr svc start "$T/d.service"
    assert_failure 2
    [ "$stderr" = "svc.sh: $T/d.service:3: bad value for TimeoutStopSec: soon" ]
    printf '%s\n' "[Install]" "WantedBy=multi-user.target" > "$T/e.service"
    run --separate-stderr svc start "$T/e.service"
    assert_failure 2
    [ "$stderr" = "svc.sh: $T/e.service:1: unknown section [Install]" ]
}

@test "rejects bad arguments" {
    usage="usage: svc.sh start|stop|restart|status|reload|logs [-n N] UNIT"
    run --separate-stderr svc
    assert_failure 2
    [ "$stderr" = "$usage" ]
    unit web "ExecStart=true"
    run --separate-stderr svc frobnicate "$T/web.service"
    assert_failure 2
    [ "$stderr" = "$usage" ]
    run --separate-stderr svc start "$T/web.service" extra
    assert_failure 2
    [ "$stderr" = "$usage" ]
}
