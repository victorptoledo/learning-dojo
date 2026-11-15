#!/bin/bash
# Checks a Docker kata. /work holds the kata folder (Dockerfile, compose.yaml, tests/),
# /facts what the dojo learned about the learner's image on the host engine.
# hadolint and `docker-compose config` become one test each; bats runs tests/*.bats.
# Prints the result in `cargo test` format.
set -u

out=$(mktemp -d)
# Work on a copy with Unix line ends, so CRLF files from Windows editors read the same.
cp -r /work "$out/kata"
find "$out/kata" -type f \( -name '*.bats' -o -name '*.txt' -o -name '*.yaml' -o -name '*.yml' \
    -o -name 'Dockerfile' -o -name '.dockerignore' -o -name '*.env' \) -exec sed -i 's/\r$//' {} +
cd "$out/kata" || exit 1

if [ ! -f Dockerfile ] && [ ! -f compose.yaml ]; then
    echo "error: no Dockerfile or compose.yaml in the kata folder"
    exit 1
fi

export FACTS=/facts
export COMPOSE_JSON="$out/compose.json"
: > "$out/lint.log"

if [ -f Dockerfile ]; then
    if hadolint --no-color --disable-ignore-pragma --config /opt/dojo/hadolint.yaml Dockerfile > "$out/one" 2>&1; then
        echo "LINT ok hadolint Dockerfile" >> "$out/lint.log"
    else
        echo "LINT fail hadolint Dockerfile" >> "$out/lint.log"
        sed 's/^/LINT msg /' "$out/one" >> "$out/lint.log"
    fi
fi

if [ -f compose.yaml ]; then
    if docker-compose --file compose.yaml config --format json > "$COMPOSE_JSON" 2> "$out/one"; then
        echo "LINT ok compose compose.yaml" >> "$out/lint.log"
    else
        echo "LINT fail compose compose.yaml" >> "$out/lint.log"
        sed 's/^/LINT msg /' "$out/one" >> "$out/lint.log"
        rm -f "$COMPOSE_JSON"
    fi
fi

bats --tap --recursive tests > "$out/bats.tap" 2>&1

awk -f /opt/dojo/tap2cargo.awk "$out/bats.tap" "$out/lint.log"
status=$?
if [ "$status" -eq 2 ]; then
    echo "error: no tests ran"
    head -40 "$out/bats.tap"
fi
exit "$status"
