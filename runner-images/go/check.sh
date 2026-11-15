#!/bin/bash
# Checks a Go kata in /work: the package must compile, the visible and grading tests (staged
# in /work/tests) run under the race detector, and go vet and gofmt check the code.
# Prints the result in `cargo test` format (see report/main.go).
set -u

out=/target
rm -rf "${out:?}"/*

# Everything runs on a copy, so code that writes or deletes files cannot harm /work.
cp -r /work "$out/kata"
cd "$out/kata" || exit 1

if [ ! -f go.mod ]; then
    echo "error: no go.mod in the kata folder"
    exit 1
fi

# Go tests live next to the code they test, so the grading tests join the package folder.
if [ -d tests ]; then
    find tests -maxdepth 1 -name '*_test.go' -exec cp {} . \;
    rm -rf tests
fi

if ! go build ./... > "$out/build.log" 2>&1; then
    echo "error: the code does not compile"
    grep -v '^#' "$out/build.log"
    exit 1
fi

if ! go test -c -race -vet=off -o "$out/kata.test" . > "$out/test-build.log" 2>&1; then
    echo "error: the tests do not compile (did a function or type get renamed?)"
    grep -v '^#' "$out/test-build.log"
    exit 1
fi

exec /opt/dojo/dojo-report "$out/kata.test"
