#!/bin/bash
# Checks a Linux kata in /work: every script must parse (bash -n), the visible and grading
# tests (staged in /work/tests) run with bats, and shellcheck must find no warnings.
# Prints the result in `cargo test` format.
set -u

out=/target
rm -rf "${out:?}"/*

# Everything runs on a copy, so a script that deletes or rewrites files cannot harm /work.
# Windows editors may save CRLF line ends, which bash reads as part of each command.
cp -r /work "$out/kata"
find "$out/kata" -type f \( -name '*.sh' -o -name '*.bats' -o -name '*.txt' -o -name '*.csv' \) -exec sed -i 's/\r$//' {} +
cd "$out/kata" || exit 1

mapfile -t scripts < <(find . -name '*.sh' -not -path './tests/*' | sed 's#^\./##' | sort)
if [ "${#scripts[@]}" -eq 0 ]; then
    echo "error: no .sh scripts found in the kata folder"
    exit 1
fi

# A syntax error is the shell's "does not compile".
broken=0
for script in "${scripts[@]}"; do
    if ! bash -n "$script" 2> "$out/syntax.log"; then
        echo "error: $script has a syntax error"
        cat "$out/syntax.log"
        broken=1
    fi
done
[ "$broken" -eq 0 ] || exit 1

bats --tap --recursive tests > "$out/bats.tap" 2>&1

# shellcheck findings become one test per script, so style issues show next to the tests.
: > "$out/shellcheck.log"
for script in "${scripts[@]}"; do
    name=$script
    if shellcheck --severity=warning --format=gcc "$name" > "$out/sc.one" 2>&1; then
        echo "SC ok $name" >> "$out/shellcheck.log"
    else
        echo "SC fail $name" >> "$out/shellcheck.log"
        sed 's/^/SC msg /' "$out/sc.one" >> "$out/shellcheck.log"
    fi
done

awk -f /opt/dojo/tap2cargo.awk "$out/bats.tap" "$out/shellcheck.log"
status=$?
if [ "$status" -eq 2 ]; then
    echo "error: no tests ran"
    head -40 "$out/bats.tap"
fi
exit "$status"
