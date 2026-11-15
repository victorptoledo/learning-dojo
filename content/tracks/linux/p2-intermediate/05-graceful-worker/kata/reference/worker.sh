#!/usr/bin/env bash
# Usage: worker.sh [--once] QUEUE DONE - processes the jobs in QUEUE into DONE.
set -uo pipefail

usage() {
    echo "usage: worker.sh [--once] QUEUE DONE" >&2
    exit 2
}

once=false
if [ "${1:-}" = "--once" ]; then
    once=true
    shift
fi
[ "$#" -eq 2 ] || usage
queue="$1"
done_dir="$2"
for dir in "$queue" "$done_dir"; do
    if [ ! -d "$dir" ]; then
        echo "worker.sh: not a directory: $dir" >&2
        exit 2
    fi
done
delay="${WORKER_DELAY:-1}"

pidfile="$queue/.worker.pid"
if [ -e "$pidfile" ]; then
    other=$(cat "$pidfile")
    if [ -n "$other" ] && kill -0 "$other" 2>/dev/null; then
        echo "worker.sh: already running (pid $other)" >&2
        exit 1
    fi
    echo "worker.sh: removing stale pid file" >&2
fi
echo "$$" > "$pidfile"

stop=false
trap 'stop=true' TERM INT
trap 'rm -f "$pidfile"' EXIT

process() {
    local job="$1" name
    name="${job##*/}"
    tr '[:lower:]' '[:upper:]' < "$job" > "$done_dir/.$name.tmp" &&
        mv "$done_dir/.$name.tmp" "$done_dir/$name" &&
        rm "$job" &&
        echo "done $name"
}

while true; do
    job=$(find "$queue" -maxdepth 1 -type f ! -name '.*' | LC_ALL=C sort | head -n 1)
    if [ -z "$job" ] && "$once"; then
        exit 0
    fi
    if [ -n "$job" ]; then
        process "$job"
    fi
    if "$stop"; then
        break
    fi
    sleep "$delay" & wait $!
    if "$stop"; then
        break
    fi
done
echo "stopping"
