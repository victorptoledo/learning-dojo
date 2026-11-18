# Shared by the tests of every stage (sourced by their setup).
bats_load_library bats-support
bats_load_library bats-assert
bats_load_library bats-file

SVC="$BATS_TEST_DIRNAME/../svc.sh"
T="$BATS_TEST_TMPDIR"
export T
export SVC_STATE="$T/state"

# Runs svc.sh with bats' fd 3 closed and a time limit: a supervisor that does not detach
# fails one test instead of hanging the whole check.
svc() {
    timeout 20 bash "$SVC" "$@" 3>&-
}

# unit NAME LINE...: writes $T/NAME.service, a [Service] section with the given lines.
unit() {
    local name=$1
    shift
    { echo "[Service]"; printf '%s\n' "$@"; } > "$T/$name.service"
}

# app NAME: $T/NAME.sh gets the script read from stdin.
app() {
    cat > "$T/$1.sh"
}

# A service that writes its pid, says hello on stdout and stderr, and stops on SIGTERM.
polite_app() {
    app "$1" <<'END'
echo "$$" > "$T/pid"
echo "hello"
echo "oops" >&2
trap 'echo "got TERM"; exit 0' TERM
while true; do sleep 0.1; done
END
}

# wait_until COMMAND: runs it every 0.05 s, for up to 5 s, until it succeeds.
wait_until() {
    local i
    for i in $(seq 100); do
        eval "$1" && return 0
        sleep 0.05
    done
    echo "still false after 5 s: $1" >&2
    return 1
}

# alive PID: the process exists and is not a zombie.
alive() {
    local state
    state=$(ps -o stat= -p "$1" 2> /dev/null) || return 1
    [[ $state != Z* ]]
}

# The first line of status, as $first_line.
status_line() {
    first_line=$(svc status "$1" | head -n 1)
}

stop_everything() {
    local file
    for file in "$T"/*.service; do
        [ -e "$file" ] && timeout 10 bash "$SVC" stop "$file" > /dev/null 2>&1 3>&-
    done
    pkill -KILL -f -- "$T" 2> /dev/null || true
}
