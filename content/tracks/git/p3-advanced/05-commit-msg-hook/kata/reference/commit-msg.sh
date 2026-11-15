#!/usr/bin/env bash
# commit-msg hook: $1 is the file holding the proposed commit message.
set -euo pipefail

fail() {
    echo "commit-msg: $1" >&2
    exit 1
}

if [ "$#" -ne 1 ]; then
    echo "usage: commit-msg.sh MESSAGE-FILE" >&2
    exit 2
fi

# Clean the message as Git does: no comment lines, no blank lines at the start.
mapfile -t lines < <(grep -v '^#' "$1" || true)
while [ "${#lines[@]}" -gt 0 ] && [ -z "${lines[0]// /}" ]; do
    lines=("${lines[@]:1}")
done
[ "${#lines[@]}" -gt 0 ] || fail "empty message"

subject="${lines[0]}"
case "$subject" in
    "Merge "* | 'Revert "'* | "fixup! "* | "squash! "* | "amend! "*) exit 0 ;;
esac

pattern='^(feat|fix|docs|style|refactor|perf|test|build|ci|chore|revert)(\([a-z0-9-]+\))?!?: [^ ]'
[[ $subject =~ $pattern ]] || fail "subject must look like 'type(scope): description'"
[ "${#subject}" -le 72 ] || fail "subject is longer than 72 characters"
if [ "${#lines[@]}" -gt 1 ] && [ -n "${lines[1]}" ]; then
    fail "leave the second line empty"
fi
