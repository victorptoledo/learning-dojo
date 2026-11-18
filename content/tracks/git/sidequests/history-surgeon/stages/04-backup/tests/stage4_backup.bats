setup() {
    source "$BATS_TEST_DIRNAME/helpers.sh"
    map="$repo/.git/scrub/commit-map"
    zeros=0000000000000000000000000000000000000000
}

# branches_and_tags: every branch and tag with what it points at.
branches_and_tags() {
    git -C "$repo" for-each-ref --format='%(refname) %(objectname)' refs/heads refs/tags
}

# backup_refs: the backup, written back with the names of the refs it copies.
backup_refs() {
    git -C "$repo" for-each-ref --format='%(refname) %(objectname)' refs/scrub-backup |
        sed 's#^refs/scrub-backup/#refs/#'
}

# leaky: main with a secret file in its second commit, a tag and a branch.
leaky() {
    put src/app.txt "app\n"
    commit "app"
    put .env "KEY=hunter2\n"
    put src/app.txt "app 2\n"
    commit "env"
    git -C "$repo" tag -a -m "Release 1" v1
    git -C "$repo" switch -q -c feature
    put src/feature.txt "f\n"
    commit "feature"
    git -C "$repo" switch -q main
    put .env "KEY=hunter3\n"
    commit "only env"
}

@test "backup: purge copies every branch and tag before rewriting" {
    leaky
    before=$(branches_and_tags)
    run --separate-stderr scrub purge "$repo" .env
    assert_success
    [ "$(backup_refs)" = "$before" ]
    [ "$(branches_and_tags)" != "$before" ]
}

@test "backup: the commit map covers kept, rewritten and dropped commits" {
    leaky
    app=$(id main~2)
    env=$(id main~1)
    only_env=$(id main)
    feature=$(id feature)
    run scrub purge "$repo" .env
    assert_success
    assert_output "rewrote 2 commits, dropped 1"
    assert_file_exist "$map"
    expected=$(printf '%s %s\n' "$app" "$app" "$env" "$(id main)" "$only_env" "$zeros" \
        "$feature" "$(id feature)" | LC_ALL=C sort)
    [ "$(cat "$map")" = "$expected" ]
}

@test "backup: one backup at a time" {
    leaky
    scrub purge "$repo" .env
    printf 'hunter2\n' > "$BATS_TEST_TMPDIR/words"
    before=$(state)
    run --separate-stderr scrub redact "$repo" "$BATS_TEST_TMPDIR/words"
    assert_failure 1
    [ "$stderr" = "scrub.sh: a backup exists: run undo or expire first" ]
    run --separate-stderr scrub purge "$repo" src
    assert_failure 1
    [ "$stderr" = "scrub.sh: a backup exists: run undo or expire first" ]
    [ "$(state)" = "$before" ]
}

@test "backup: redact backs up too, but not when nothing changes" {
    leaky
    printf 'nothing like this\n' > "$BATS_TEST_TMPDIR/words"
    run scrub redact "$repo" "$BATS_TEST_TMPDIR/words"
    assert_success
    [ -z "$(backup_refs)" ]
    assert_file_not_exist "$map"
    before=$(branches_and_tags)
    printf 'hunter2\n' > "$BATS_TEST_TMPDIR/words"
    run scrub redact "$repo" "$BATS_TEST_TMPDIR/words"
    assert_success
    [ "$(backup_refs)" = "$before" ]
    [ "$(wc -l < "$map")" -eq 4 ]
}

@test "undo: puts every branch and tag back and the files on disk" {
    leaky
    before=$(branches_and_tags)
    scrub purge "$repo" .env
    assert_file_not_exist "$repo/.env"
    git -C "$repo" branch after-rewrite
    run --separate-stderr scrub undo "$repo"
    assert_success
    assert_output "restored 3 refs"
    [ "$(branches_and_tags | grep -v after-rewrite)" = "$before" ]
    [ -n "$(git -C "$repo" for-each-ref refs/heads/after-rewrite)" ]
    [ -z "$(backup_refs)" ]
    assert_file_not_exist "$map"
    [ "$(cat "$repo/.env")" = "KEY=hunter3" ]
    [ -z "$(git -C "$repo" status --porcelain)" ]
    [ "$(git -C "$repo" symbolic-ref HEAD)" = refs/heads/main ]
    run scrub purge "$repo" .env
    assert_success
}

@test "undo: needs a backup and a clean tree" {
    leaky
    run --separate-stderr scrub undo "$repo"
    assert_failure 1
    [ "$stderr" = "scrub.sh: nothing to undo" ]
    scrub purge "$repo" .env
    put src/app.txt "edited\n"
    run --separate-stderr scrub undo "$repo"
    assert_failure 1
    [ "$stderr" = "scrub.sh: uncommitted changes" ]
    [ -n "$(backup_refs)" ]
}

@test "expire: the old objects are really gone" {
    leaky
    old_env=$(id main:.env)
    old_commit=$(id main)
    scrub purge "$repo" .env
    git -C "$repo" cat-file -e "$old_env"
    git -C "$repo" cat-file -e "$old_commit"
    run --separate-stderr scrub expire "$repo"
    assert_success
    assert_output "expired the backup and the reflogs"
    run git -C "$repo" cat-file -e "$old_env"
    assert_failure
    run git -C "$repo" cat-file -e "$old_commit"
    assert_failure
    [ -z "$(backup_refs)" ]
    assert_file_not_exist "$map"
    git -C "$repo" fsck --no-progress --no-dangling
    [ "$(git -C "$repo" show main:src/app.txt)" = "app 2" ]
    run scrub undo "$repo"
    assert_failure 1
    run scrub expire "$repo"
    assert_success
}

@test "expire: a redacted secret is gone from the object store" {
    leaky
    secret=$(id v1:.env)
    printf 'hunter2\nhunter3\n' > "$BATS_TEST_TMPDIR/words"
    scrub redact "$repo" "$BATS_TEST_TMPDIR/words"
    git -C "$repo" cat-file -e "$secret"
    scrub expire "$repo"
    run git -C "$repo" cat-file -e "$secret"
    assert_failure
    [ "$(git -C "$repo" show v1:.env)" = "KEY=***REMOVED***" ]
}
