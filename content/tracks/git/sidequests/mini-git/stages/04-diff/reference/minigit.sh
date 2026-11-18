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

# open_repo REPO: sets $repo, or stops with the usage error.
open_repo() {
    repo=$1
    [ -d "$repo" ] && git -C "$repo" rev-parse --git-dir > /dev/null 2>&1 || usage
}

# resolve REV: the id of the commit REV names.
resolve() {
    git -C "$repo" rev-parse --verify --quiet "$1^{commit}" 2> /dev/null || fail "unknown revision $1"
}

# log REPO [REV]: "ID SUBJECT" along first parents.
cmd_log() {
    [ "$#" -ge 1 ] && [ "$#" -le 2 ] || usage
    open_repo "$1"
    local commit parent subject
    commit=$(resolve "${2:-HEAD}")
    while [ -n "$commit" ]; do
        # Headers until the first empty line, then the message: its first paragraph is the
        # subject, lines joined by a space.
        { read -r parent; IFS= read -r subject; } < <(git -C "$repo" cat-file commit "$commit" | awk '
            !body && /^parent / && first == "" { first = $2 }
            !body && $0 == "" { body = 1; next }
            body && $0 == "" && subject != "" { exit }
            body && $0 != "" { subject = (subject == "" ? $0 : subject " " $0) }
            END { print first; print subject }')
        echo "$commit $subject"
        commit=$parent
    done
}

# walk TREE PREFIX: every entry of TREE that is not a tree, recursively, in tree order.
walk() {
    local mode type id name
    while IFS=$' \t' read -r mode type id name; do
        if [ "$type" = tree ]; then
            walk "$id" "$2$name/"
        else
            printf '%s %s %s\t%s\n' "$mode" "$type" "$id" "$2$name"
        fi
    done < <(git -C "$repo" cat-file -p "$1")
}

# ls-tree REPO REV: like git ls-tree -r REV.
cmd_ls-tree() {
    [ "$#" -eq 2 ] || usage
    open_repo "$1"
    local commit
    commit=$(resolve "$2")
    walk "$(git -C "$repo" rev-parse "$commit^{tree}")" ""
}

# every STATUS TREE PREFIX: a STATUS line for every file under TREE.
every() {
    local path
    while IFS=$'\t' read -r _ path; do
        printf '%s\t%s\n' "$1" "$path"
    done < <(walk "$2" "$3")
}

# kind MODE: what a tree entry is, so a change of kind is a T.
kind() {
    case "$1" in
        120000) echo link ;;
        160000) echo submodule ;;
        040000 | 40000) echo tree ;;
        *) echo file ;;
    esac
}

# compare A B PREFIX: the changes between two trees (either may be empty), skipping every
# entry whose id is the same on both sides.
compare() {
    local -A left=() right=()
    local mode type id name a b
    if [ -n "$1" ]; then
        while IFS=$' \t' read -r mode type id name; do left[$name]="$mode $id"; done \
            < <(git -C "$repo" cat-file -p "$1")
    fi
    if [ -n "$2" ]; then
        while IFS=$' \t' read -r mode type id name; do right[$name]="$mode $id"; done \
            < <(git -C "$repo" cat-file -p "$2")
    fi
    for name in "${!left[@]}" "${!right[@]}"; do
        [ -z "${seen[$3$name]:-}" ] || continue
        seen[$3$name]=1
        a=${left[$name]:-}
        b=${right[$name]:-}
        [ "$a" != "$b" ] || continue
        local ka="" kb=""
        [ -z "$a" ] || ka=$(kind "${a% *}")
        [ -z "$b" ] || kb=$(kind "${b% *}")
        if [ "$ka" = tree ] && [ "$kb" = tree ]; then
            compare "${a#* }" "${b#* }" "$3$name/"
            continue
        fi
        if [ "$ka" = tree ]; then every D "${a#* }" "$3$name/"; elif [ -n "$ka" ] && [ -z "$kb" ]; then printf 'D\t%s\n' "$3$name"; fi
        if [ "$kb" = tree ]; then every A "${b#* }" "$3$name/"; elif [ -n "$kb" ] && [ -z "$ka" ]; then printf 'A\t%s\n' "$3$name"; fi
        if [ "$ka" = tree ] && [ -n "$kb" ] && [ "$kb" != tree ]; then printf 'A\t%s\n' "$3$name"; fi
        if [ "$kb" = tree ] && [ -n "$ka" ] && [ "$ka" != tree ]; then printf 'D\t%s\n' "$3$name"; fi
        if [ -n "$ka" ] && [ -n "$kb" ] && [ "$ka" != tree ] && [ "$kb" != tree ]; then
            if [ "$ka" = "$kb" ]; then printf 'M\t%s\n' "$3$name"; else printf 'T\t%s\n' "$3$name"; fi
        fi
    done
}

# diff REPO A B: like git diff --name-status --no-renames A B.
cmd_diff() {
    [ "$#" -eq 3 ] || usage
    open_repo "$1"
    local a b
    a=$(resolve "$2")
    b=$(resolve "$3")
    declare -gA seen=()
    compare "$(git -C "$repo" rev-parse "$a^{tree}")" "$(git -C "$repo" rev-parse "$b^{tree}")" "" |
        sort -t $'\t' -k2,2
}

[ "$#" -ge 1 ] || usage
command=$1
shift
case "$command" in
    hash | snapshot | log | ls-tree | diff) ;;
    *) usage ;;
esac
tmp=$(mktemp -d)
trap 'rm -rf "$tmp"' EXIT
"cmd_$command" "$@"
