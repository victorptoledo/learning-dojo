setup() {
    source "$BATS_TEST_DIRNAME/helpers.sh"
    stamp='[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}Z'
}

teardown() {
    stop_everything
}

# The log without its time stamps.
messages() {
    sed -E 's/^[^ ]+ //' "$SVC_STATE/$1.log"
}

@test "every line of the log is stamped with the UTC time" {
    polite_app web
    unit web "ExecStart=bash $T/web.sh"
    svc start "$T/web.service"
    wait_until 'grep -q oops "$SVC_STATE/web.log"'
    run grep -E "^$stamp hello$" "$SVC_STATE/web.log"
    assert_success
    run grep -E "^$stamp oops$" "$SVC_STATE/web.log"
    assert_success
    run grep -vE "^$stamp " "$SVC_STATE/web.log"
    assert_failure
    year=$(TZ=UTC date +%Y)
    grep -q "^$year-" "$SVC_STATE/web.log"
}

@test "the supervisor logs its events" {
    app job <<'END'
echo "working"
exit 2
END
    unit job "ExecStart=bash $T/job.sh"
    svc start "$T/job.service"
    wait_until 'grep -q "svc: exited" "$SVC_STATE/job.log"'
    pid=$(sed -nE 's/.*svc: started \(pid ([0-9]+)\)$/\1/p' "$SVC_STATE/job.log")
    run messages job
    assert_output "svc: started (pid $pid)
working
svc: exited (status 2)"
}

@test "restarts and stops are in the log" {
    app job <<'END'
echo "run"
exit 1
END
    unit job "ExecStart=bash $T/job.sh" "Restart=on-failure" "RestartSec=0.2" "StartLimitBurst=2"
    svc start "$T/job.service"
    wait_until 'grep -q "svc: start limit hit" "$SVC_STATE/job.log"'
    run messages job
    assert_line --index 0 --regexp '^svc: started \(pid [0-9]+\)$'
    assert_line --index 1 "run"
    assert_line --index 2 "svc: exited (status 1)"
    assert_line --index 3 "svc: restarting in 0.2s"
    assert_line --index 4 --regexp '^svc: started \(pid [0-9]+\)$'
    assert_line --index 7 "svc: start limit hit"
    polite_app web
    unit web "ExecStart=bash $T/web.sh"
    svc start "$T/web.service"
    wait_until '[ -s "$T/pid" ]'
    svc stop "$T/web.service"
    [ "$(messages web | tail -n 1)" = "svc: stopped" ]
}

@test "a service killed after the timeout is logged" {
    app stubborn <<'END'
echo "$$" > "$T/pid"
trap '' TERM
while true; do sleep 0.1; done
END
    unit stubborn "ExecStart=bash $T/stubborn.sh" "TimeoutStopSec=0.3"
    svc start "$T/stubborn.service"
    wait_until '[ -s "$T/pid" ]'
    svc stop "$T/stubborn.service"
    run messages stubborn
    assert_line --index 1 "svc: killed after 0.3s"
    assert_line --index 2 "svc: stopped"
}

@test "keeps a last line without a newline" {
    app job <<'END'
printf 'no newline at the end'
END
    unit job "ExecStart=bash $T/job.sh"
    svc start "$T/job.service"
    wait_until 'grep -q "svc: exited" "$SVC_STATE/job.log"'
    run messages job
    assert_line --index 1 "no newline at the end"
}

@test "logs prints the last lines" {
    app job <<'END'
for i in $(seq 1 15); do echo "line $i"; done
END
    unit job "ExecStart=bash $T/job.sh"
    run svc logs "$T/job.service"
    assert_success
    assert_output ""
    svc start "$T/job.service"
    wait_until 'grep -q "svc: exited" "$SVC_STATE/job.log"'
    run svc logs "$T/job.service"
    assert_success
    [ "${#lines[@]}" -eq 10 ]
    assert_line --index 8 --regexp "^$stamp line 15$"
    assert_line --index 9 --regexp "^$stamp svc: exited \(status 0\)$"
    run svc logs -n 2 "$T/job.service"
    assert_success
    assert_line --index 0 --regexp "^$stamp line 15$"
    [ "${#lines[@]}" -eq 2 ]
}

@test "rotates the log and keeps LogKeep old files" {
    app chatty <<'END'
for i in $(seq 1 60); do echo "message number $i"; done
END
    unit chatty "ExecStart=bash $T/chatty.sh" "LogMaxBytes=400" "LogKeep=2"
    svc start "$T/chatty.service"
    wait_until 'svc logs -n 1 "$T/chatty.service" | grep -q "svc: exited"'
    [ -e "$SVC_STATE/chatty.log.1" ]
    [ -e "$SVC_STATE/chatty.log.2" ]
    [ ! -e "$SVC_STATE/chatty.log.3" ]
    for file in "$SVC_STATE"/chatty.log*; do
        [ "$(stat -c %s "$file")" -lt 460 ]
    done
    [ "$(stat -c %s "$SVC_STATE/chatty.log.1")" -ge 400 ]
    run svc logs -n 1000 "$T/chatty.service"
    numbers=$(printf '%s\n' "${lines[@]}" | sed -nE 's/.* message number ([0-9]+)$/\1/p')
    first=$(head -n 1 <<< "$numbers")
    [ "$first" -gt 1 ]
    [ "$(tail -n 1 <<< "$numbers")" -eq 60 ]
    [ "$numbers" = "$(seq "$first" 60)" ]
}

@test "LogKeep=0 keeps no old file" {
    app chatty <<'END'
for i in $(seq 1 40); do echo "message number $i"; done
END
    unit chatty "ExecStart=bash $T/chatty.sh" "LogMaxBytes=300" "LogKeep=0"
    svc start "$T/chatty.service"
    wait_until 'grep -q "svc: exited" "$SVC_STATE/chatty.log" 2> /dev/null'
    [ ! -e "$SVC_STATE/chatty.log.1" ]
    [ "$(stat -c %s "$SVC_STATE/chatty.log")" -lt 360 ]
}
