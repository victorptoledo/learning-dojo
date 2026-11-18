#!/usr/bin/env bash
# Usage: svc.sh start|stop|status|logs [-n N] UNIT - a small service manager:
# runs the ExecStart of UNIT (NAME.service) under a supervisor, in the background.
set -uo pipefail
export LC_ALL=C TZ=UTC

usage() {
    echo "usage: svc.sh start|stop|restart|status|reload|logs [-n N] UNIT" >&2
    exit 2
}

# die MESSAGE [STATUS]
die() {
    echo "svc.sh: $1" >&2
    exit "${2:-2}"
}

# Seconds such as 1.5 -> microseconds, the unit of now.
micros() {
    local int=${1%%.*} frac=000000
    if [[ $1 == *.* ]]; then
        frac="${1#*.}000000"
    fi
    echo $(( 10#${int:-0} * 1000000 + 10#${frac:0:6} ))
}

now() {
    echo "${EPOCHREALTIME/./}"
}

alive() {
    [ -n "$1" ] && kill -0 "$1" 2> /dev/null
}

parse_unit() {
    local line number=0 key value section=""
    [ -f "$unit" ] && [ -r "$unit" ] || die "cannot read $unit"
    case $unit in
        *.service) ;;
        *) die "$unit: not a .service file" ;;
    esac
    name=$(basename -- "$unit" .service)
    exec_start="" timeout_stop=5 restart=no restart_sec=1 burst=5 interval=10
    max_bytes=1048576 keep=3
    while IFS= read -r line || [ -n "$line" ]; do
        number=$((number + 1))
        line=${line%$'\r'}
        [[ $line =~ ^[[:space:]]*([#\;]|$) ]] && continue
        if [[ $line =~ ^\[(.*)\]$ ]]; then
            section=${BASH_REMATCH[1]}
            case $section in
                Unit | Service) ;;
                *) die "$unit:$number: unknown section [$section]" ;;
            esac
            continue
        fi
        [[ $line == *=* ]] || die "$unit:$number: expected KEY=VALUE"
        [ -n "$section" ] || die "$unit:$number: KEY=VALUE before any [section]"
        key=${line%%=*} value=${line#*=}
        case $section:$key in
            Unit:Description) ;;
            Service:ExecStart) exec_start=$value ;;
            Service:TimeoutStopSec) seconds && timeout_stop=$value ;;
            Service:Restart)
                case $value in
                    no | on-failure | always) restart=$value ;;
                    *) bad_value ;;
                esac
                ;;
            Service:RestartSec) seconds && restart_sec=$value ;;
            Service:StartLimitBurst) count 1 && burst=$value ;;
            Service:StartLimitIntervalSec) seconds && interval=$value ;;
            Service:LogMaxBytes) count 1 && max_bytes=$value ;;
            Service:LogKeep) count 0 && keep=$value ;;
            *) die "$unit:$number: unknown key $key" ;;
        esac
    done < "$unit"
    [ -n "$exec_start" ] || die "$unit: missing ExecStart"
}

bad_value() {
    die "$unit:$number: bad value for $key: $value"
}

# The value being parsed is a number of seconds, decimals allowed.
seconds() {
    [[ $value =~ ^[0-9]+(\.[0-9]+)?$ ]] || bad_value
}

# count MIN: the value being parsed is a whole number of at least MIN.
count() {
    [[ $value =~ ^[0-9]+$ ]] && [ "$value" -ge "$1" ] || bad_value
}

files() {
    supervisor_file="$state/$name.supervisor" pid_file="$state/$name.pid"
    status_file="$state/$name.status" restarts_file="$state/$name.restarts"
    log_file="$state/$name.log"
}

supervisor_pid() {
    local pid
    pid=$(cat "$supervisor_file" 2> /dev/null)
    if alive "$pid"; then echo "$pid"; fi
}

main_pid() {
    local pid
    pid=$(cat "$pid_file" 2> /dev/null)
    if alive "$pid"; then echo "$pid"; fi
}

# ---- the supervisor: a detached process that runs the service and watches it ----

event() {
    echo "svc: $*" >&"$log"
}

set_status() {
    echo "$1" > "$status_file"
}

# Reads the lines of the service and of the supervisor, stamps them and writes them to the
# log, which it rotates past LogMaxBytes: NAME.log -> NAME.log.1 -> ... -> NAME.log.KEEP.
log_writer() {
    local line stamp size out i
    exec {out}>> "$log_file"
    size=$(stat -c %s "$log_file")
    while IFS= read -r line || [ -n "$line" ]; do
        printf -v stamp '%(%Y-%m-%dT%H:%M:%SZ)T' -1
        printf '%s %s\n' "$stamp" "$line" >&"$out"
        size=$((size + ${#stamp} + ${#line} + 2))
        if [ "$size" -ge "$max_bytes" ]; then
            exec {out}>&-
            rm -f -- "$log_file.$keep"
            for ((i = keep - 1; i >= 1; i--)); do
                [ ! -e "$log_file.$i" ] || mv -f -- "$log_file.$i" "$log_file.$((i + 1))"
            done
            if [ "$keep" -gt 0 ]; then
                mv -f -- "$log_file" "$log_file.1"
            else
                rm -f -- "$log_file"
            fi
            exec {out}>> "$log_file"
            size=0
        fi
    done
}

# Copies the service's output into the log pipe line by line, ending a last line that has
# no newline, so a supervisor event never lands on the same line.
forward_lines() {
    local line
    while IFS= read -r line || [ -n "$line" ]; do
        printf '%s
' "$line"
    done >&"$log"
}

# TERM to the service's process group, SIGKILL after TimeoutStopSec, until the group is gone.
stop_main() {
    local deadline killed=false
    deadline=$(( $(now) + $(micros "$timeout_stop") ))
    kill -TERM -- "-$main" 2> /dev/null
    while kill -0 -- "-$main" 2> /dev/null; do
        if [ "$killed" = false ] && (( $(now) >= deadline )); then
            kill -KILL -- "-$main" 2> /dev/null
            event "killed after ${timeout_stop}s"
            killed=true
        fi
        sleep 0.05
    done
    wait "$main" 2> /dev/null
}

supervise() {
    local code restarts=0 starts=() recent t s logger out forwarder
    echo "$$" > "$supervisor_file"
    echo 0 > "$restarts_file"
    exec {log}> >(log_writer)
    logger=$!
    stopping=false main=""
    # The trap only sets a flag and forwards the signal: bash runs it between commands,
    # and `wait` returns as soon as a trapped signal arrives.
    trap 'stopping=true; [ -z "$main" ] || kill -TERM -- "-$main" 2> /dev/null' TERM INT
    while true; do
        starts+=("$(now)")
        # A session of its own: the service's pid is its process group, so stop can signal
        # everything it started.
        exec {out}> >(forward_lines)
        forwarder=$!
        setsid bash -c "$exec_start" < /dev/null >&"$out" 2>&1 &
        main=$!
        exec {out}>&-
        echo "$main" > "$pid_file"
        set_status running
        event "started (pid $main)"

        wait "$main"
        code=$?
        while [ "$stopping" = false ] && alive "$main"; do
            wait "$main"
            code=$?
        done
        if [ "$stopping" = true ]; then
            stop_main
            wait "$forwarder" 2> /dev/null
            event "stopped"
            set_status stopped
            break
        fi
        kill -KILL -- "-$main" 2> /dev/null
        wait "$forwarder" 2> /dev/null
        main=""
        event "exited (status $code)"
        if ! { [ "$restart" = always ] || { [ "$restart" = on-failure ] && [ "$code" -ne 0 ]; }; }; then
            set_status "exited $code"
            break
        fi
        # Start limit: give up when StartLimitBurst starts happened within the interval.
        t=$(now)
        recent=0
        for s in "${starts[@]}"; do
            (( t - s < $(micros "$interval") )) && recent=$((recent + 1))
        done
        if [ "$recent" -ge "$burst" ]; then
            event "start limit hit"
            set_status "failed (start limit hit)"
            break
        fi
        restarts=$((restarts + 1))
        echo "$restarts" > "$restarts_file"
        set_status restarting
        event "restarting in ${restart_sec}s"
        sleep "$restart_sec" &
        wait "$!"
        if [ "$stopping" = true ]; then
            kill "$!" 2> /dev/null
            event "stopped"
            set_status stopped
            break
        fi
    done
    exec {log}>&-
    wait "$logger" 2> /dev/null
    rm -f -- "$supervisor_file"
}

# ---- the commands ----

start_service() {
    local pid i
    pid=$(supervisor_pid)
    if [ -n "$pid" ]; then
        echo "$name is already running (pid $(main_pid))"
        return 0
    fi
    rm -f -- "$pid_file"
    setsid bash "$0" __supervise "$unit" < /dev/null > /dev/null 2>&1 &
    for i in $(seq 100); do
        [ -s "$pid_file" ] && break
        sleep 0.05
    done
    [ -s "$pid_file" ] || die "$name did not start" 1
    pid=$(cat "$pid_file")
    echo "started $name (pid $pid)"
}

stop_service() {
    local pid i limit
    pid=$(supervisor_pid)
    if [ -z "$pid" ]; then
        echo "$name is not running"
        return 0
    fi
    kill -TERM "$pid"
    limit=$(( ($(micros "$timeout_stop") / 1000000 + 5) * 20 ))
    for ((i = 0; i < limit; i++)); do
        alive "$pid" || break
        sleep 0.05
    done
    alive "$pid" && die "$name did not stop" 1
    echo "stopped $name"
}

show_status() {
    local pid
    if [ -n "$(supervisor_pid)" ]; then
        pid=$(main_pid)
        if [ -n "$pid" ]; then
            echo "$name: active (running), pid $pid"
            echo "restarts: $(cat "$restarts_file" 2> /dev/null || echo 0)"
            return 0
        fi
        echo "$name: activating (auto-restart)"
        echo "restarts: $(cat "$restarts_file" 2> /dev/null || echo 0)"
        return 3
    fi
    case $(cat "$status_file" 2> /dev/null) in
        "exited "*) echo "$name: inactive ($(cat "$status_file"))" ;;
        stopped) echo "$name: inactive (stopped)" ;;
        "failed "*) echo "$name: $(cat "$status_file")" ;;
        *) echo "$name: inactive" ;;
    esac
    echo "restarts: $(cat "$restarts_file" 2> /dev/null || echo 0)"
    return 3
}

show_logs() {
    local lines=$1 i
    {
        for ((i = keep; i >= 1; i--)); do
            [ ! -e "$log_file.$i" ] || cat -- "$log_file.$i"
        done
        [ ! -e "$log_file" ] || cat -- "$log_file"
    } | tail -n "$lines"
}

command=${1:-}
[ "$#" -gt 0 ] && shift
lines=10
if [ "$command" = logs ] && [ "${1:-}" = -n ]; then
    [ "$#" -ge 2 ] && [[ $2 =~ ^[0-9]+$ ]] || usage
    lines=$2
    shift 2
fi
[ "$#" -eq 1 ] || usage
unit=$1
case $command in
    start | stop | status | logs | __supervise) ;;
    *) usage ;;
esac
parse_unit
state=${SVC_STATE:-${XDG_STATE_HOME:-$HOME/.local/state}/svc}
mkdir -p -- "$state" || die "cannot create $state"
files

case $command in
    __supervise) supervise ;;
    start) start_service ;;
    stop) stop_service ;;
    status) show_status ;;
    logs) show_logs "$lines" ;;
esac
