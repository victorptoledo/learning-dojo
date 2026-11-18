setup() {
    source "$BATS_TEST_DIRNAME/helpers.sh"
}

teardown() {
    stop_everything
}

# crashing_app NAME STATUS SECONDS: counts its runs in $T/runs, exits STATUS after SECONDS.
crashing_app() {
    app "$1" <<END
echo "\$\$" >> "\$T/runs"
sleep $3
exit $2
END
}

runs() {
    if [ -e "$T/runs" ]; then wc -l < "$T/runs"; else echo 0; fi
}

@test "Restart=on-failure restarts a service that crashes" {
    crashing_app worker 3 0.3
    unit worker "ExecStart=bash $T/worker.sh" "Restart=on-failure" "RestartSec=0.2"
    run svc start "$T/worker.service"
    assert_success
    wait_until '[ "$(runs)" -ge 3 ]'
    [ "$(sort -u "$T/runs" | wc -l)" -ge 3 ]
    run svc status "$T/worker.service"
    assert_line --index 1 --regexp '^restarts: [2-9]$'
}

@test "Restart=on-failure leaves a clean exit alone" {
    crashing_app worker 0 0.1
    unit worker "ExecStart=bash $T/worker.sh" "Restart=on-failure" "RestartSec=0.1"
    svc start "$T/worker.service"
    wait_until '! svc status "$T/worker.service" > /dev/null'
    sleep 0.6
    [ "$(runs)" -eq 1 ]
    run svc status "$T/worker.service"
    assert_failure 3
    assert_output "worker: inactive (exited 0)
restarts: 0"
}

@test "Restart=always restarts a clean exit too" {
    crashing_app worker 0 0.1
    unit worker "ExecStart=bash $T/worker.sh" "Restart=always" "RestartSec=0.1"
    svc start "$T/worker.service"
    wait_until '[ "$(runs)" -ge 3 ]'
}

@test "without Restart a crash is final" {
    crashing_app worker 3 0.1
    unit worker "ExecStart=bash $T/worker.sh"
    svc start "$T/worker.service"
    wait_until '! svc status "$T/worker.service" > /dev/null'
    sleep 1.3
    [ "$(runs)" -eq 1 ]
    run svc status "$T/worker.service"
    assert_output "worker: inactive (exited 3)
restarts: 0"
}

@test "gives up when the start limit is hit" {
    crashing_app worker 1 0
    unit worker "ExecStart=bash $T/worker.sh" "Restart=always" "RestartSec=0.1" "StartLimitBurst=3" "StartLimitIntervalSec=10"
    svc start "$T/worker.service"
    wait_until '[ "$(svc status "$T/worker.service" | head -n 1)" = "worker: failed (start limit hit)" ]'
    sleep 0.5
    [ "$(runs)" -eq 3 ]
    run svc status "$T/worker.service"
    assert_failure 3
    assert_output "worker: failed (start limit hit)
restarts: 2"
    run svc start "$T/worker.service"
    assert_success
    wait_until '[ "$(runs)" -ge 4 ]'
}

@test "status shows the wait before a restart" {
    crashing_app worker 1 0.1
    unit worker "ExecStart=bash $T/worker.sh" "Restart=on-failure" "RestartSec=3"
    svc start "$T/worker.service"
    wait_until '[ "$(svc status "$T/worker.service" | head -n 1)" = "worker: activating (auto-restart)" ]'
    run svc status "$T/worker.service"
    assert_failure 3
    assert_line --index 1 "restarts: 1"
}

@test "stop during the wait before a restart is immediate and final" {
    crashing_app worker 1 0.1
    unit worker "ExecStart=bash $T/worker.sh" "Restart=on-failure" "RestartSec=30"
    svc start "$T/worker.service"
    wait_until '[ "$(svc status "$T/worker.service" | head -n 1)" = "worker: activating (auto-restart)" ]'
    start=$SECONDS
    run svc stop "$T/worker.service"
    assert_success
    assert_output "stopped worker"
    [ $((SECONDS - start)) -lt 3 ]
    run svc status "$T/worker.service"
    assert_line --index 0 "worker: inactive (stopped)"
    [ "$(runs)" -eq 1 ]
}

@test "a stopped service is not restarted" {
    polite_app web
    unit web "ExecStart=bash $T/web.sh" "Restart=always" "RestartSec=0.1"
    svc start "$T/web.service"
    wait_until '[ -s "$T/pid" ]'
    run svc stop "$T/web.service"
    assert_success
    sleep 0.5
    [ "$(pgrep -cf -- "$T/web.sh")" -eq 0 ]
    run svc status "$T/web.service"
    assert_output "web: inactive (stopped)
restarts: 0"
}

@test "rejects bad restart settings" {
    unit a "ExecStart=true" "Restart=sometimes"
    run --separate-stderr svc start "$T/a.service"
    assert_failure 2
    [ "$stderr" = "svc.sh: $T/a.service:3: bad value for Restart: sometimes" ]
    unit b "ExecStart=true" "StartLimitBurst=0"
    run --separate-stderr svc start "$T/b.service"
    assert_failure 2
    [ "$stderr" = "svc.sh: $T/b.service:3: bad value for StartLimitBurst: 0" ]
    unit c "ExecStart=true" "RestartSec=1m"
    run --separate-stderr svc start "$T/c.service"
    assert_failure 2
    [ "$stderr" = "svc.sh: $T/c.service:3: bad value for RestartSec: 1m" ]
}
