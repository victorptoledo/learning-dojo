#!/usr/bin/env bash
# Usage: converge.sh [--check] ROOT - brings the files under ROOT to the state in desired.txt.
# Every resource is checked first and changed only when it differs, so a second run changes
# nothing. With --check nothing is written and the exit status says whether anything drifted.
set -euo pipefail

here=$(cd "$(dirname "$0")" && pwd)
check=0
if [ "${1:-}" = "--check" ]; then
    check=1
    shift
fi
if [ "$#" -ne 1 ]; then
    echo "usage: converge.sh [--check] ROOT" >&2
    exit 2
fi
root=${1%/}
if [ ! -d "$root" ]; then
    echo "converge.sh: $root is not a directory" >&2
    exit 1
fi

changed=0
unchanged=0

# report DIFFERS WHAT: one line per resource, counted for the summary.
report() {
    if [ "$1" -eq 1 ]; then
        changed=$((changed + 1))
        if [ "$check" -eq 1 ]; then
            echo "would change: $2"
        else
            echo "changed: $2"
        fi
    else
        unchanged=$((unchanged + 1))
        echo "ok: $2"
    fi
}

# render TEMPLATE: the template with every ${KEY} from vars.env replaced.
render() {
    local content key value
    content=$(cat "$here/$1")
    while IFS='=' read -r key value; do
        [ -n "$key" ] || continue
        content=${content//"\${$key}"/$value}
    done < "$here/vars.env"
    printf '%s\n' "$content"
}

ensure_dir() {
    local path=$root$1 mode=$2 differs=0
    if [ ! -d "$path" ] || [ "$(stat -c %a "$path")" != "${mode#0}" ]; then
        differs=1
        if [ "$check" -eq 0 ]; then
            mkdir -p "$path"
            chmod "$mode" "$path"
        fi
    fi
    report "$differs" "dir $1"
}

ensure_file() {
    local path=$root$1 template=$2 mode=$3 differs=0 wanted
    wanted=$(render "$template")
    if [ ! -f "$path" ] || [ "$(cat "$path")" != "$wanted" ] || [ "$(stat -c %a "$path")" != "${mode#0}" ]; then
        differs=1
        if [ "$check" -eq 0 ]; then
            mkdir -p "$(dirname "$path")"
            printf '%s\n' "$wanted" > "$path"
            chmod "$mode" "$path"
        fi
    fi
    report "$differs" "file $1"
}

ensure_line() {
    local path=$root$1 line=$2 differs=0
    if [ ! -f "$path" ] || ! grep -qxF -- "$line" "$path"; then
        differs=1
        if [ "$check" -eq 0 ]; then
            mkdir -p "$(dirname "$path")"
            printf '%s\n' "$line" >> "$path"
        fi
    fi
    report "$differs" "line $1"
}

ensure_absent() {
    local path=$root$1 differs=0
    if [ -e "$path" ]; then
        differs=1
        if [ "$check" -eq 0 ]; then
            rm -rf -- "$path"
        fi
    fi
    report "$differs" "absent $1"
}

number=0
while read -r kind path rest || [ -n "${kind:-}" ]; do
    number=$((number + 1))
    case ${kind:-} in
        '' | '#'*) continue ;;
        dir) ensure_dir "$path" "$rest" ;;
        file) ensure_file "$path" "${rest% *}" "${rest##* }" ;;
        line) ensure_line "$path" "$rest" ;;
        absent) ensure_absent "$path" ;;
        *)
            echo "converge.sh: line $number: unknown resource '$kind'" >&2
            exit 1
            ;;
    esac
done < "$here/desired.txt"

if [ "$check" -eq 1 ]; then
    echo "summary: $changed to change, $unchanged ok"
    [ "$changed" -eq 0 ] || exit 3
else
    echo "summary: $changed changed, $unchanged ok"
fi
