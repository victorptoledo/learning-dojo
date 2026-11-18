setup() {
    source "$BATS_TEST_DIRNAME/helpers.sh"
}

teardown() {
    stop_everything
}

# A service that reads its "configuration" at start and again on SIGHUP.
reloadable_app() {
    app "$1" <<'END'
echo "$$" > "$T/pid"
load() { echo "config is $(cat "$T/config")"; }
load
trap load HUP
trap 'exit 0' TERM
while true; do sleep 0.1; done
END
}

@test "reload runs ExecReload with MAINPID and keeps the process" {
    echo one > "$T/config"
    reloadable_app web
    unit web "ExecStart=bash $T/web.sh" 'ExecReload=kill -HUP $MAINPID'
    svc start "$T/web.service"
    wait_until 'grep -q "config is one" "$SVC_STATE/web.log"'
    pid=$(cat "$T/pid")
    echo two > "$T/config"
    run svc reload "$T/web.service"
    assert_success
    assert_output "reloaded web"
    wait_until 'grep -q "config is two" "$SVC_STATE/web.log"'
    run svc status "$T/web.service"
    assert_line --index 0 "web: active (running), pid $pid"
}

@test "reload explains why it cannot run" {
    reloadable_app web
    unit web "ExecStart=bash $T/web.sh"
    svc start "$T/web.service"
    run --separate-stderr svc reload "$T/web.service"
    assert_failure 1
    [ "$stderr" = "svc.sh: web has no ExecReload" ]
    unit job "ExecStart=bash $T/web.sh" 'ExecReload=exit 7'
    run --separate-stderr svc reload "$T/job.service"
    assert_failure 1
    [ "$stderr" = "svc.sh: job is not running" ]
    svc start "$T/job.service"
    run --separate-stderr svc reload "$T/job.service"
    assert_failure 1
    [ "$stderr" = "svc.sh: reload of job failed (status 7)" ]
}

@test "restart gives the service a new process" {
    echo one > "$T/config"
    reloadable_app web
    unit web "ExecStart=bash $T/web.sh"
    run svc restart "$T/web.service"
    assert_success
    assert_output --regexp '^started web \(pid [0-9]+\)$'
    wait_until '[ -s "$T/pid" ]'
    old=$(cat "$T/pid")
    rm "$T/pid"
    run svc restart "$T/web.service"
    assert_success
    assert_line --index 0 "stopped web"
    wait_until '[ -s "$T/pid" ]'
    new=$(cat "$T/pid")
    assert_line --index 1 "started web (pid $new)"
    [ "$new" != "$old" ]
    ! alive "$old"
}
