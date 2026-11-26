#!/usr/bin/env bash
# Usage: install as hooks/pre-receive of a bare repository. Reads "OLD NEW REF" lines on stdin
# and rejects the whole push, listing every broken rule, when one of the lines breaks a rule.
set -euo pipefail

zero_re='^0+$'
secret_re='AKIA[0-9A-Z]{16}|-----BEGIN [A-Z ]*PRIVATE KEY-----'
limit=$(git config --int guard.maxBlobSize 2> /dev/null || echo 1048576)
problems=()

reject() {
    problems+=("guard: $1: $2")
}

while read -r old new ref; do
    if [[ $new =~ $zero_re ]]; then
        case "$ref" in
            refs/heads/main) reject "$ref" "deleting main is not allowed" ;;
            refs/tags/v*) reject "$ref" "release tags are immutable" ;;
        esac
        continue
    fi

    case "$ref" in
        refs/heads/main)
            if [[ $old =~ $zero_re ]]; then
                added=("$new" --not --all)
            else
                if ! git merge-base --is-ancestor "$old" "$new"; then
                    reject "$ref" "non-fast-forward push is not allowed"
                fi
                # Commits the server already has on another branch still join main here.
                added=("$old..$new")
            fi
            while read -r merge; do
                reject "$ref" "merge commit $merge is not allowed, rebase instead"
            done < <(git rev-list --min-parents=2 "${added[@]}")
            ;;
        refs/tags/v*)
            if ! [[ $old =~ $zero_re ]]; then
                reject "$ref" "release tags are immutable"
            elif [ "$(git cat-file -t "$new")" != tag ]; then
                reject "$ref" "release tags must be annotated"
            fi
            ;;
    esac

    # Only objects the server does not have yet: whatever it has was checked when it came in.
    # Every new blob counts, not just the tip's files: a secret added and then deleted in
    # the same push is still in history.
    while read -r type oid size path; do
        [ "$type" = blob ] || continue
        if [ "$size" -gt "$limit" ]; then
            reject "$ref" "$path is $size bytes (limit $limit), use Git LFS"
        # grep -c reads the whole blob: grep -q would stop early, and under pipefail the
        # SIGPIPE of git cat-file would turn a match into a failure.
        elif [ "$(git cat-file blob "$oid" | grep -cE -e "$secret_re" || true)" -gt 0 ]; then
            reject "$ref" "possible secret in $path"
        fi
    done < <(git rev-list --objects "$new" --not --all \
        | git cat-file --batch-check='%(objecttype) %(objectname) %(objectsize) %(rest)')
done

if [ "${#problems[@]}" -gt 0 ]; then
    printf '%s\n' "${problems[@]}" >&2
    echo "guard: push rejected (${#problems[@]} problems)" >&2
    exit 1
fi
