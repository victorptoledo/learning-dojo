#!/bin/sh
# Prints "<GREETING>, <name>!" for every argument. GREETING defaults to "hello".
set -eu
greeting=${GREETING:-hello}
for name in "$@"; do
    echo "$greeting, $name!"
done
