# Shared by the tests of every stage (sourced by their setup).
bats_load_library bats-support
bats_load_library bats-assert
bats_load_library bats-file

PJOBS="$BATS_TEST_DIRNAME/../pjobs.sh"
export T="$BATS_TEST_TMPDIR"

# Runs pjobs.sh with bats' fd 3 closed and a time limit, so a hang fails one test only.
pjobs() {
    timeout 30 bash "$PJOBS" "$@" 3>&-
}

# Starts pjobs.sh in the background, output in $T/log; its pid is in $runner.
pjobs_in_background() {
    bash "$PJOBS" "$@" > "$T/log" 2>&1 3>&- &
    runner=$!
}

# wait_for FILE...: waits up to 5 seconds until every FILE exists.
wait_for() {
    local i file missing
    for i in $(seq 100); do
        missing=0
        for file in "$@"; do
            [ -e "$file" ] || missing=1
        done
        [ "$missing" -eq 0 ] && return 0
        sleep 0.05
    done
    echo "still missing after 5 s: $*" >&2
    return 1
}

# alive PID: the process exists and is not a zombie.
alive() {
    local state
    state=$(ps -o stat= -p "$1" 2> /dev/null) || return 1
    [[ $state != Z* ]]
}

# Kills whatever a broken pjobs.sh left behind.
kill_leftovers() {
    pkill -KILL -f 'pjobs\.sh' 2> /dev/null || true
    pkill -KILL -x sleep 2> /dev/null || true
}
