setup() {
    source "$BATS_TEST_DIRNAME/helpers.sh"
}

# contents TAG: the message of an annotated tag.
contents() {
    git -C "$repo" tag -l --format='%(contents)' "$1"
}

@test "tag: an annotated tag on HEAD holding the notes" {
    commit "feat: first"
    release_tag v1.4.2
    commit "feat(cart): add coupon codes"
    commit "fix: round VAT per line"
    run release notes "$repo"
    notes=$output
    head=$(git -C "$repo" rev-parse HEAD)
    run --separate-stderr release tag "$repo"
    assert_success
    assert_output "tagged v1.5.0"
    [ "$(git -C "$repo" cat-file -t v1.5.0)" = tag ]
    [ "$(git -C "$repo" rev-parse 'v1.5.0^{commit}')" = "$head" ]
    [ "$(git -C "$repo" rev-parse HEAD)" = "$head" ]
    [ "$(contents v1.5.0)" = "$notes" ]
    [ "$(git -C "$repo" tag -l --format='%(taggername)' v1.5.0)" = "Release Bot" ]
    run --separate-stderr release next "$repo"
    assert_failure 1
    [ "$stderr" = "release.sh: nothing to release since v1.5.0" ]
}

@test "tag: the first release" {
    commit "chore: initial commit"
    run release tag "$repo"
    assert_success
    assert_output "tagged v0.1.0"
    [ "$(contents v0.1.0)" = "## v0.1.0 (2024-03-01)

No user-facing changes." ]
}

@test "tag: refuses uncommitted changes but not untracked files" {
    commit "feat: first"
    echo "draft" > "$repo/notes.txt"
    run --separate-stderr release tag "$repo"
    assert_success
    assert_output "tagged v0.1.0"
    commit "fix: one"
    echo "edited" >> "$repo/changes/1.txt"
    run --separate-stderr release tag "$repo"
    assert_failure 1
    [ "$stderr" = "release.sh: uncommitted changes" ]
    git -C "$repo" checkout -q -- changes
    git -C "$repo" add notes.txt
    run --separate-stderr release tag "$repo"
    assert_failure 1
    [ "$stderr" = "release.sh: uncommitted changes" ]
    [ "$(git -C "$repo" tag -l)" = "v0.1.0" ]
}

@test "tag: refuses a detached HEAD" {
    commit "feat: first"
    git -C "$repo" switch -q --detach HEAD
    run --separate-stderr release tag "$repo"
    assert_failure 1
    [ "$stderr" = "release.sh: HEAD is detached" ]
    [ -z "$(git -C "$repo" tag -l)" ]
}

@test "tag: refuses nothing to release and a tag that already exists" {
    commit "feat: first"
    release_tag v1.0.0
    commit "docs: guide"
    run --separate-stderr release tag "$repo"
    assert_failure 1
    [ "$stderr" = "release.sh: nothing to release since v1.0.0" ]
    git -C "$repo" switch -q -c other
    commit "fix: elsewhere"
    release_tag v1.1.0
    git -C "$repo" switch -q main
    commit "feat: here"
    run --separate-stderr release tag "$repo"
    assert_failure 1
    [ "$stderr" = "release.sh: tag v1.1.0 already exists" ]
    [ "$(git -C "$repo" rev-parse 'v1.1.0^{commit}')" = "$(git -C "$repo" rev-parse other)" ]
}

@test "tag: release candidates count up per version and identifier" {
    commit "feat: first"
    release_tag v1.0.0
    commit "feat!: new checkout"
    run release tag "$repo" --pre rc
    assert_success
    assert_output "tagged v2.0.0-rc.1"
    [ "$(git -C "$repo" cat-file -t v2.0.0-rc.1)" = tag ]
    [ "$(contents v2.0.0-rc.1 | head -n 1)" = "## v2.0.0-rc.1 (2024-03-02)" ]
    run release next "$repo"
    assert_output "v2.0.0"
    commit "fix: checkout totals"
    run release tag "$repo" --pre rc
    assert_output "tagged v2.0.0-rc.2"
    git -C "$repo" tag -a -m rc v2.0.0-rc.9 'v1.0.0^{commit}'
    commit "fix: more"
    run release tag "$repo" --pre rc
    assert_output "tagged v2.0.0-rc.10"
    run release tag "$repo" --pre beta
    assert_output "tagged v2.0.0-beta.1"
    run release tag "$repo"
    assert_output "tagged v2.0.0"
    commit "fix: after"
    run release tag "$repo" --pre rc
    assert_output "tagged v2.0.1-rc.1"
}

@test "tag: rejects bad options" {
    commit "feat: first"
    for args in "--pre" "--pre RC" "--pre rc1" "--pre r-c" "--draft" "extra"; do
        # shellcheck disable=SC2086
        run --separate-stderr release tag "$repo" $args
        assert_failure 2
        [ "$stderr" = "usage: release.sh next|notes|tag REPO [OPTIONS]" ]
    done
    [ -z "$(git -C "$repo" tag -l)" ]
}
