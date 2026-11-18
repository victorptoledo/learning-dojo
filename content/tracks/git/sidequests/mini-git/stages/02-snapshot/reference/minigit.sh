#!/usr/bin/env bash
# Usage: minigit.sh hash|snapshot|log|ls-tree|diff|unreachable ARGS - Git's work on plumbing.
set -euo pipefail
export LC_ALL=C
EMPTY_TREE=4b825dc642cb6eb9a060e54bf8d69288fbee4904

usage() {
    echo "usage: minigit.sh COMMAND ARGS" >&2
    exit 2
}

fail() {
    echo "minigit.sh: $*" >&2
    exit 1
}

# object_id TYPE FILE: the SHA-1 of "TYPE SIZE\0" followed by the file's bytes.
object_id() {
    local size
    size=$(stat -c %s "$2")
    { printf '%s %d\0' "$1" "$size"; cat "$2"; } | sha1sum | cut -c 1-40
}

# hex_bytes HEX: writes the bytes that HEX spells.
hex_bytes() {
    # shellcheck disable=SC2059
    printf "$(sed 's/../\\x&/g' <<< "$1")"
}

# tree_id DIR: the id of the tree of DIR, or nothing when it holds no files at all.
tree_id() {
    local dir=$1 entries name path mode id
    entries=$(mktemp -p "$tmp")
    # Folders sort as "name/": build "key<TAB>name" lines and sort them by key.
    while IFS=$'\t' read -r _ name; do
        path="$dir/$name"
        if [ -L "$path" ]; then
            readlink "$path" | tr -d '\n' > "$tmp/link"
            mode=120000
            id=$(object_id blob "$tmp/link")
        elif [ -d "$path" ]; then
            id=$(tree_id "$path")
            [ -n "$id" ] || continue
            mode=40000
        elif [ -f "$path" ]; then
            mode=100644
            [ -x "$path" ] && mode=100755
            id=$(object_id blob "$path")
        else
            continue
        fi
        printf '%s %s\0' "$mode" "$name" >> "$entries"
        hex_bytes "$id" >> "$entries"
    done < <(find "$dir" -mindepth 1 -maxdepth 1 ! -name .git \
        \( -type d -printf '%f/\t%f\n' -o -printf '%f\t%f\n' \) | sort -t $'\t' -k1,1)
    if [ -s "$entries" ]; then object_id tree "$entries"; fi
}

cmd_hash() {
    [ "$#" -eq 2 ] || usage
    case "$1" in
        blob) [ -f "$2" ] || usage; object_id blob "$2" ;;
        tree)
            [ -d "$2" ] || usage
            local id
            id=$(tree_id "$2")
            echo "${id:-$EMPTY_TREE}"
            ;;
        *) usage ;;
    esac
}

# write_tree DIR: writes the blobs and trees of DIR into $repo and prints the tree's id, or
# nothing when DIR holds no files at all.
write_tree() {
    local dir=$1 entries name path mode type id
    entries=$(mktemp -p "$tmp")
    while IFS= read -r -d '' name; do
        path="$dir/$name"
        type=blob
        if [ -L "$path" ]; then
            mode=120000
            id=$(readlink "$path" | tr -d '\n' | git -C "$repo" hash-object -w --stdin)
        elif [ -d "$path" ]; then
            id=$(write_tree "$path")
            [ -n "$id" ] || continue
            mode=040000
            type=tree
        elif [ -f "$path" ]; then
            mode=100644
            [ -x "$path" ] && mode=100755
            id=$(git -C "$repo" hash-object -w -- "$path")
        else
            continue
        fi
        printf '%s %s %s\t%s\0' "$mode" "$type" "$id" "$name" >> "$entries"
    done < <(find "$dir" -mindepth 1 -maxdepth 1 ! -name .git -printf '%f\0')
    # mktree sorts the entries itself, folders as "name/".
    if [ -s "$entries" ]; then git -C "$repo" mktree -z < "$entries"; fi
}

# snapshot REPO DIR BRANCH MESSAGE: commits DIR as the next commit of BRANCH, without the index.
cmd_snapshot() {
    [ "$#" -eq 4 ] || usage
    repo=$1
    local dir=$2 ref="refs/heads/$3" message=$4 tree parent commit
    [ -d "$dir" ] || usage
    git -C "$repo" rev-parse --git-dir > /dev/null 2>&1 || usage
    tree=$(write_tree "$dir")
    [ -n "$tree" ] || fail "nothing to commit"
    parent=$(git -C "$repo" rev-parse --verify --quiet "$ref^{commit}" || true)
    if [ -n "$parent" ]; then
        [ "$(git -C "$repo" rev-parse "$parent^{tree}")" != "$tree" ] || fail "nothing to commit"
        commit=$(git -C "$repo" commit-tree "$tree" -p "$parent" -m "$message")
    else
        commit=$(git -C "$repo" commit-tree "$tree" -m "$message")
    fi
    # The old value makes the update fail if the branch moved since we read it.
    git -C "$repo" update-ref "$ref" "$commit" "$parent"
    echo "$commit"
}

[ "$#" -ge 1 ] || usage
command=$1
shift
case "$command" in
    hash | snapshot) ;;
    *) usage ;;
esac
tmp=$(mktemp -d)
trap 'rm -rf "$tmp"' EXIT
"cmd_$command" "$@"
