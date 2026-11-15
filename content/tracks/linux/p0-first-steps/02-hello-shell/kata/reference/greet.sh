#!/usr/bin/env bash
# Prints a welcome message for the name given as arguments.
shout=false
if [ "${1:-}" = "--shout" ]; then
    shout=true
    shift
fi

name="$*"
if [ -z "$name" ]; then
    name="stranger"
fi

message="Welcome to the dojo, $name!"
if [ "$shout" = true ]; then
    message="${message^^}"
fi
echo "$message"
