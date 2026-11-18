#!/bin/sh
# Sends 1000 stock updates to a staging server: ./scripts/load-test.sh http://staging:8080
set -eu
base=${1:?usage: load-test.sh BASE_URL}
i=0
while [ "$i" -lt 1000 ]; do
    curl --silent --output /dev/null -X PUT --data "qty=$i" "$base/items/LOAD-$((i % 50))"
    i=$((i + 1))
done
