# Helpers for Docker kata tests: `load /opt/dojo/facts` at the top of a .bats file.
#
# The dojo builds the learner's image on the host engine and leaves what it learned in
# $FACTS (/facts): image.json (docker image inspect), history.jsonl (docker history),
# files.txt (every path in the image), commands/<name>.{out,err,status} (one per line of
# tests/commands.txt), container.json and logs.txt (the image started once, no network).
# When the container is still running, the tests share its network: localhost is the
# learner's container. The tests run in a copy of the kata folder.

bats_load_library bats-support
bats_load_library bats-assert

FACTS=${FACTS:-/facts}

# Fails the test with a readable message when the image was not built.
require_image() {
    [ -s "$FACTS/image.json" ] || fail "the image was not built (is there a Dockerfile?)"
}

# jq over the image config: image_jq '.Config.User'
image_jq() {
    require_image || return 1
    jq -r ".[0] | $1" "$FACTS/image.json"
}

# The user the image runs as, "" for root by default.
image_user() { image_jq '.Config.User // ""'; }

# Size of the image in MB (10^6 bytes), rounded down.
image_size_mb() { image_jq '(.Size / 1000000) | floor'; }

# Number of file system layers.
image_layers() { image_jq '.RootFS.Layers | length'; }

# Value of an environment variable set in the image, empty when unset.
image_env() {
    image_jq ".Config.Env // [] | map(select(startswith(\"$1=\"))) | .[0] // \"\" | sub(\"^$1=\"; \"\")"
}

# Exposed ports, one per line, like 8080/tcp.
image_ports() { image_jq '.Config.ExposedPorts // {} | keys[]'; }

# Label value, empty when unset.
image_label() { image_jq ".Config.Labels // {} | .[\"$1\"] // \"\""; }

# Health check command as one line, empty when the image has none.
image_healthcheck() { image_jq '.Config.Healthcheck.Test // [] | join(" ")'; }

# Every path in the image, one per line (directories end with /).
image_files() {
    require_image || return 1
    cat "$FACTS/files.txt"
}

# True when the image has the file or directory.
image_has_path() {
    local path=${1%/}
    image_files | grep -qxF -e "$path" -e "$path/"
}

# True when any path in the image starts with the prefix.
image_has_prefix() { image_files | grep -q "^$1"; }

# The command of every layer step, oldest first.
image_history() {
    require_image || return 1
    jq -r '.CreatedBy' "$FACTS/history.jsonl" | tac
}

# Output, error output and exit status of a line of tests/commands.txt.
cmd_out() { tr -d '\r' < "$FACTS/commands/$1.out"; }
cmd_err() { tr -d '\r' < "$FACTS/commands/$1.err"; }
cmd_status() { cat "$FACTS/commands/$1.status"; }

# jq over the started container: container_jq '.State.Status'
container_jq() {
    require_image || return 1
    jq -r ".[0] | $1" "$FACTS/container.json"
}

container_status() { container_jq '.State.Status'; }
container_exit_code() { container_jq '.State.ExitCode'; }
container_health() { container_jq '.State.Health.Status // "none"'; }
container_logs() { tr -d '\r' < "$FACTS/logs.txt"; }

# GET http://localhost:PORT/PATH from inside the learner's container network.
# Retries for about 10 s while the server starts. Prints the body.
http_get() {
    local url="http://localhost:$1$2" tries=0
    until curl --silent --show-error --fail --max-time 2 "$url"; do
        tries=$((tries + 1))
        if [ "$tries" -ge 20 ]; then
            echo "nothing answered on $url" >&2
            return 1
        fi
        sleep 0.5
    done
}

# HTTP status code of GET http://localhost:PORT/PATH, 000 when nothing answers.
http_status() {
    curl --silent --output /dev/null --write-out '%{http_code}' --max-time 2 "http://localhost:$1$2" || true
}

# jq over the normalised compose file (docker-compose config --format json).
compose_jq() {
    [ -s "$COMPOSE_JSON" ] || { fail "compose.yaml is not valid (see the compose::compose_yaml test)"; return 1; }
    jq -r "$1" "$COMPOSE_JSON"
}
