setup() {
    bats_load_library bats-support
    bats_load_library bats-assert
    bats_load_library bats-file
    export GIT_CONFIG_GLOBAL="$BATS_TEST_TMPDIR/gitconfig" GIT_CONFIG_NOSYSTEM=1
    git config --global user.name "Dojo Tester"
    git config --global user.email "tester@dojo.test"
    git config --global init.defaultBranch main
    git config --global advice.detachedHead false
    origin="$BATS_TEST_TMPDIR/origin.git"
    repo="$BATS_TEST_TMPDIR/me"
    git init -q --bare "$origin"
    git clone -q "$origin" "$repo" 2> /dev/null
    commit README.md "# Shop" "Initial commit"
    git -C "$repo" push -q origin main 2> /dev/null
    cp guard.sh "$origin/hooks/pre-receive"
    chmod +x "$origin/hooks/pre-receive"
    # Built at run time, so this file never contains a real-looking secret.
    aws_key="AKIA$(printf 'Q%.0s' {1..16})"
    key_header="-----BEGIN OPENSSH ""PRIVATE KEY-----"
}

# commit FILE TEXT SUBJECT
commit() {
    mkdir -p "$(dirname "$repo/$1")"
    printf '%s\n' "$2" > "$repo/$1"
    git -C "$repo" add "$1"
    git -C "$repo" commit -q -m "$3"
}

# big FILE BYTES SUBJECT
big() {
    mkdir -p "$(dirname "$repo/$1")"
    head -c "$2" /dev/zero | tr '\0' 'x' > "$repo/$1"
    git -C "$repo" add "$1"
    git -C "$repo" commit -q -m "$3"
}

server() {
    git -C "$origin" rev-parse -q --verify "$1" || echo none
}

# push ARGS...: pushes and keeps Git's messages (the hook's lines start with "remote: ").
push() {
    run git -C "$repo" push origin "$@"
}

@test "rejects a force-push to main and leaves the server's main alone" {
    commit a.txt a "Add a"
    git -C "$repo" push -q origin main 2> /dev/null
    before=$(server main)
    git -C "$repo" reset -q --hard HEAD~1
    commit b.txt b "Add b"
    push --force main
    assert_failure
    assert_output --partial "remote: guard: refs/heads/main: non-fast-forward push is not allowed"
    assert_output --partial "remote: guard: push rejected (1 problems)"
    assert_equal "$(server main)" "$before"
}

@test "rejects deleting main" {
    push --delete main
    assert_failure
    assert_output --partial "remote: guard: refs/heads/main: deleting main is not allowed"
    refute_output --partial "non-fast-forward"
}

@test "rejects a merge commit on main, naming it" {
    git -C "$repo" switch -q -c feature
    commit f.txt f "Add f"
    git -C "$repo" switch -q main
    commit m.txt m "Add m"
    git -C "$repo" merge -q --no-edit feature
    merge=$(git -C "$repo" rev-parse HEAD)
    push main
    assert_failure
    assert_output --partial "remote: guard: refs/heads/main: merge commit $merge is not allowed, rebase instead"
}

@test "catches a merge commit the server already has on another branch when main moves to it" {
    git -C "$repo" switch -q -c feature
    commit f.txt f "Add f"
    git -C "$repo" switch -q -c topic main
    commit t.txt t "Add t"
    git -C "$repo" merge -q --no-edit feature
    merge=$(git -C "$repo" rev-parse HEAD)
    push topic
    assert_success
    push topic:main
    assert_failure
    assert_output --partial "remote: guard: refs/heads/main: merge commit $merge is not allowed, rebase instead"
}

@test "other branches may merge, be force-pushed and be deleted" {
    git -C "$repo" switch -q -c feature
    commit f.txt f "Add f"
    push feature
    assert_success
    git -C "$repo" switch -q -c side main
    commit s.txt s "Add s"
    git -C "$repo" merge -q --no-edit feature
    push side
    assert_success
    git -C "$repo" switch -q feature
    git -C "$repo" commit -q --amend -m "Add f, reworded"
    push --force feature
    assert_success
    push --delete feature side
    assert_success
    assert_equal "$(server feature)" none
}

@test "release tags are annotated and immutable" {
    git -C "$repo" tag v1.0.0
    push v1.0.0
    assert_failure
    assert_output --partial "remote: guard: refs/tags/v1.0.0: release tags must be annotated"
    git -C "$repo" tag -d v1.0.0 > /dev/null
    git -C "$repo" tag -a v1.0.0 -m "Release v1.0.0"
    push v1.0.0
    assert_success
    commit a.txt a "Add a"
    git -C "$repo" tag -f -a v1.0.0 -m "Release v1.0.0 again" > /dev/null
    push --force v1.0.0
    assert_failure
    assert_output --partial "remote: guard: refs/tags/v1.0.0: release tags are immutable"
    push --delete v1.0.0
    assert_failure
    assert_output --partial "remote: guard: refs/tags/v1.0.0: release tags are immutable"
    assert_equal "$(server 'v1.0.0^{commit}')" "$(git -C "$repo" rev-parse HEAD~1)"
}

@test "other tags are free" {
    git -C "$repo" tag nightly
    push nightly
    assert_success
    commit a.txt a "Add a"
    git -C "$repo" tag -f nightly > /dev/null
    push --force nightly
    assert_success
    push --delete nightly
    assert_success
}

@test "rejects a file over guard.maxBlobSize with its path and size" {
    git -C "$origin" config guard.maxBlobSize 1000
    big "assets/intro video.mp4" 1001 "Add the intro video"
    push main
    assert_failure
    assert_output --partial "remote: guard: refs/heads/main: assets/intro video.mp4 is 1001 bytes (limit 1000), use Git LFS"
    git -C "$repo" reset -q --hard HEAD~1
    big assets/logo.png 1000 "Add the logo"
    push main
    assert_success
}

@test "the default limit is 1 MiB" {
    git -C "$repo" switch -q -c small
    big small.bin 1048576 "Add a file of exactly 1 MiB"
    push small
    assert_success
    git -C "$repo" switch -q -c large main
    big large.bin 1048577 "Add a file one byte over"
    push large
    assert_failure
    assert_output --partial "remote: guard: refs/heads/large: large.bin is 1048577 bytes (limit 1048576), use Git LFS"
}

@test "rejects an AWS access key and a private key on any branch" {
    git -C "$repo" switch -q -c config
    commit config/aws.env "AWS_ACCESS_KEY_ID=$aws_key" "Add AWS settings"
    commit deploy/id_ed25519 "$key_header" "Add the deploy key"
    push config
    assert_failure
    assert_output --partial "remote: guard: refs/heads/config: possible secret in config/aws.env"
    assert_output --partial "remote: guard: refs/heads/config: possible secret in deploy/id_ed25519"
    assert_output --partial "remote: guard: push rejected (2 problems)"
    assert_equal "$(server config)" none
}

@test "a secret added and removed again in the same push is still caught" {
    commit settings.env "TOKEN=$aws_key" "Add settings"
    git -C "$repo" rm -q settings.env
    git -C "$repo" commit -q -m "Remove settings"
    push main
    assert_failure
    assert_output --partial "remote: guard: refs/heads/main: possible secret in settings.env"
}

@test "finds a secret at the top of a large file" {
    { echo "key=$aws_key"; head -c 400000 /dev/zero | tr '\0' 'x'; echo; } > "$repo/dump.sql"
    git -C "$repo" add dump.sql
    git -C "$repo" commit -q -m "Add a database dump"
    push main
    assert_failure
    assert_output --partial "remote: guard: refs/heads/main: possible secret in dump.sql"
}

@test "only checks what the server does not have yet" {
    git -C "$origin" config guard.maxBlobSize 1000
    big notes.txt 900 "Add notes"
    push main
    assert_success
    git -C "$origin" config guard.maxBlobSize 100
    git -C "$repo" branch copy
    push copy
    assert_success
    git -C "$repo" tag -a v1.0.0 -m "Release v1.0.0"
    push v1.0.0
    assert_success
}

@test "the push is all or nothing and every problem is listed" {
    git -C "$origin" config guard.maxBlobSize 1000
    git -C "$repo" switch -q -c good
    commit g.txt g "Add g"
    git -C "$repo" switch -q -c bad main
    commit token.env "$aws_key" "Add token"
    big huge.bin 2000 "Add huge"
    git -C "$repo" tag light
    git -C "$repo" tag v2.0.0
    push good bad v2.0.0
    assert_failure
    assert_output --partial "remote: guard: refs/heads/bad: possible secret in token.env"
    assert_output --partial "remote: guard: refs/heads/bad: huge.bin is 2000 bytes (limit 1000), use Git LFS"
    assert_output --partial "remote: guard: refs/tags/v2.0.0: release tags must be annotated"
    assert_output --partial "remote: guard: push rejected"
    assert_equal "$(server good)" none
    assert_equal "$(server bad)" none
}

@test "--no-verify does not get past a server-side hook" {
    commit settings.env "TOKEN=$aws_key" "Add settings"
    push --no-verify main
    assert_failure
    assert_output --partial "remote: guard: refs/heads/main: possible secret in settings.env"
}
