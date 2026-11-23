#!/usr/bin/env bash
# Usage: ci.sh [lint|test|all] - the checks CI runs, the same way on a laptop.
set -uo pipefail

root=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
cd "$root" || exit 1

usage() {
    echo "usage: ci.sh [lint|test|all]" >&2
    exit 2
}

# Every .sh file under bin/ and scripts/, through ShellCheck one by one, so one bad file
# does not hide the others.
lint() {
    local files=() file failed=0 dirs=()
    for file in bin scripts; do
        [ -d "$file" ] && dirs+=("$file")
    done
    if [ "${#dirs[@]}" -gt 0 ]; then
        mapfile -t files < <(find "${dirs[@]}" -type f -name '*.sh' | LC_ALL=C sort)
    fi
    for file in "${files[@]}"; do
        shellcheck --severity=warning --format=gcc "$file" || failed=$((failed + 1))
    done
    if [ "$failed" -gt 0 ]; then
        echo "lint: failed ($failed of ${#files[@]} files)"
        return 1
    fi
    echo "lint: ok (${#files[@]} files)"
}

# The bats tests under test/, with a JUnit report in a fresh reports/ folder.
unit_tests() {
    if [ -z "$(find test -type f -name '*.bats' 2> /dev/null | head -1)" ]; then
        echo "test: no tests in test/"
        return 1
    fi
    rm -rf reports
    mkdir reports
    if bats --recursive --report-formatter junit --output reports test; then
        echo "test: ok"
    else
        echo "test: failed"
        return 1
    fi
}

[ "$#" -le 1 ] || usage
case "${1:-all}" in
    lint) lint ;;
    test) unit_tests ;;
    all)
        lint || exit 1
        unit_tests || exit 1
        echo "ci: ok"
        ;;
    *) usage ;;
esac
