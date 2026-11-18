setup() {
    source "$BATS_TEST_DIRNAME/helpers.sh"
    d="$BATS_TEST_TMPDIR/photos"
    mkdir -p "$d"
}

# index_and_head: what snapshot must leave alone.
index_and_head() {
    sha1sum "$repo/.git/index" 2> /dev/null || echo "no index"
    git -C "$repo" symbolic-ref HEAD
    (cd "$repo" && find . -path ./.git -prune -o -type f -print | sort | xargs -r sha1sum)
}

@test "snapshot: a new branch from a folder" {
    mkdir -p "$d/2024/july" "$d/raw"
    printf 'jpeg\0\1' > "$d/2024/july/beach.jpg"
    printf 'list\n' > "$d/index.txt"
    printf '#!/bin/sh\n' > "$d/raw/convert.sh"
    chmod +x "$d/raw/convert.sh"
    ln -s 2024/july/beach.jpg "$d/latest"
    run --separate-stderr minigit snapshot "$repo" "$d" backup "backup of photos"
    assert_success
    no_porcelain
    [ "$output" = "$(git -C "$repo" rev-parse refs/heads/backup)" ]
    [ "$(git -C "$repo" rev-parse "$output^{tree}")" = "$(git_tree "$d")" ]
    [ "$(git -C "$repo" log -1 --format='%B' backup)" = "backup of photos" ]
    [ "$(git -C "$repo" log -1 --format='%an <%ae>|%cn' backup)" = "Dojo Tester <tester@dojo.test>|Dojo Tester" ]
    [ "$(git -C "$repo" rev-list --count backup)" -eq 1 ]
    git -C "$repo" fsck --no-progress --strict
}

@test "snapshot: the next commit has the old tip as parent" {
    printf 'one\n' > "$d/a.txt"
    first=$(minigit snapshot "$repo" "$d" backup "one")
    printf 'two\n' >> "$d/a.txt"
    mkdir -p "$d/new"
    printf 'n\n' > "$d/new/file"
    run minigit snapshot "$repo" "$d" backup "two"
    assert_success
    [ "$(git -C "$repo" rev-parse backup)" = "$output" ]
    [ "$(git -C "$repo" rev-parse backup~1)" = "$first" ]
    run git -C "$repo" diff --name-status backup~1 backup
    assert_output "$(printf 'M\ta.txt\nA\tnew/file')"
    no_porcelain
}

@test "snapshot: author and dates come from the environment" {
    printf 'x\n' > "$d/x"
    export GIT_AUTHOR_NAME="Backup Bot" GIT_AUTHOR_EMAIL=bot@backup.example
    export GIT_AUTHOR_DATE=2024-07-01T10:00:00Z GIT_COMMITTER_DATE=2024-07-01T10:00:00Z
    run minigit snapshot "$repo" "$d" backup "nightly"
    assert_success
    [ "$(git -C "$repo" log -1 --format='%an <%ae> %at %ct' backup)" = "Backup Bot <bot@backup.example> 1719828000 1719828000" ]
}

@test "snapshot: the index, the working tree and HEAD are left alone" {
    put README.md "# Repo\n"
    commit "first"
    put wip.txt "draft\n"
    git -C "$repo" add wip.txt
    put README.md "# Repo, edited\n"
    before=$(index_and_head)
    head=$(git -C "$repo" rev-parse HEAD)
    run minigit snapshot "$repo" "$repo" main "snapshot of the working tree"
    assert_success
    [ "$(index_and_head)" = "$before" ]
    [ "$(git -C "$repo" rev-parse main~1)" = "$head" ]
    [ "$(git -C "$repo" show main:README.md)" = "# Repo, edited" ]
    [ "$(git -C "$repo" show main:wip.txt)" = "draft" ]
    no_porcelain
}

@test "snapshot: nothing to commit" {
    run --separate-stderr minigit snapshot "$repo" "$d" backup "empty"
    assert_failure 1
    [ "$stderr" = "minigit.sh: nothing to commit" ]
    run git -C "$repo" rev-parse --verify --quiet refs/heads/backup
    assert_failure
    printf 'same\n' > "$d/f"
    minigit snapshot "$repo" "$d" backup "one"
    tip=$(git -C "$repo" rev-parse backup)
    mkdir -p "$d/empty-folder"
    run --separate-stderr minigit snapshot "$repo" "$d" backup "again"
    assert_failure 1
    [ "$stderr" = "minigit.sh: nothing to commit" ]
    [ "$(git -C "$repo" rev-parse backup)" = "$tip" ]
}

@test "snapshot: moves the branch with its old value" {
    printf 'a\n' > "$d/a"
    one=$(minigit snapshot "$repo" "$d" backup "one")
    printf 'b\n' > "$d/a"
    two=$(minigit snapshot "$repo" "$d" backup "two")
    [ "$(grep -c 'update-ref' "$calls")" -eq 2 ]
    grep -E "update-ref .*refs/heads/backup $one( | 0{40})\$" "$calls"
    grep -E "update-ref .*refs/heads/backup $two $one\$" "$calls"
}

@test "snapshot: rejects bad arguments" {
    printf 'a\n' > "$d/a"
    mkdir -p "$BATS_TEST_TMPDIR/plain"
    for args in "snapshot $repo $d backup" "snapshot $repo $d/a backup msg" \
        "snapshot $BATS_TEST_TMPDIR/plain $d backup msg"; do
        # shellcheck disable=SC2086
        run --separate-stderr minigit $args
        assert_failure 2
        [ "$stderr" = "usage: minigit.sh COMMAND ARGS" ]
    done
}
