setup() {
    source "$BATS_TEST_DIRNAME/helpers.sh"
}

# history: main has v1.2.0, then a feature released as v1.3.0; release/1.2 starts at v1.2.0.
history() {
    commit "feat: first"
    release_tag v1.2.0
    commit "feat(cart): coupons"
    release_tag v1.3.0
    git -C "$repo" branch release/1.2 v1.2.0
}

@test "branches: a release branch makes patch releases of its own line" {
    history
    git -C "$repo" switch -q release/1.2
    commit "fix(tax): round per line"
    a=$(short HEAD)
    run --separate-stderr release next "$repo"
    assert_success
    assert_output "v1.2.1"
    run release notes "$repo"
    assert_output "## v1.2.1 (2024-03-03)

### Bug Fixes

- **tax:** round per line ($a)"
    run release tag "$repo"
    assert_output "tagged v1.2.1"
    commit "docs: guide"
    commit "perf: cache"
    run release next "$repo"
    assert_output "v1.2.2"
}

@test "branches: main ignores the patch tags of release branches" {
    history
    git -C "$repo" switch -q release/1.2
    commit "fix: backport"
    release_tag v1.2.1
    git -C "$repo" switch -q main
    commit "fix: on main"
    run release next "$repo"
    assert_success
    assert_output "v1.3.1"
}

@test "branches: a release branch refuses features and breaking changes" {
    history
    git -C "$repo" switch -q release/1.2
    commit "fix: one"
    commit "feat(cart): coupons on 1.2"
    commit "fix!: change the totals"
    run --separate-stderr release next "$repo"
    assert_failure 1
    [ "$stderr" = "release.sh: release/1.2 only takes fixes: feat(cart): coupons on 1.2" ]
    run --separate-stderr release tag "$repo"
    assert_failure 1
    [ "$stderr" = "release.sh: release/1.2 only takes fixes: feat(cart): coupons on 1.2" ]
    [ "$(git -C "$repo" tag -l | wc -l)" -eq 2 ]
    git -C "$repo" switch -q -c release/1.3 v1.3.0
    commit "fix: x\n\nBREAKING CHANGE: y\n"
    run --separate-stderr release next "$repo"
    assert_failure 1
    [ "$stderr" = "release.sh: release/1.3 only takes fixes: fix: x" ]
}

@test "branches: a release branch needs a tag of its line" {
    history
    git -C "$repo" switch -q -c release/2.0
    commit "fix: early"
    run --separate-stderr release next "$repo"
    assert_failure 1
    [ "$stderr" = "release.sh: release/2.0 has no v2.0.Z tag" ]
    git -C "$repo" switch -q -c release/next
    run release next "$repo"
    assert_success
    assert_output "v1.3.1"
}

@test "branches: release candidates on a release branch" {
    history
    git -C "$repo" switch -q release/1.2
    commit "fix: one"
    run release tag "$repo" --pre rc
    assert_success
    assert_output "tagged v1.2.1-rc.1"
}

@test "branches: --changelog commits CHANGELOG.md and tags that commit" {
    commit "feat: first"
    release_tag v1.4.2
    commit "feat(cart): add coupon codes"
    run release notes "$repo"
    notes=$output
    before=$(git -C "$repo" rev-parse HEAD)
    run --separate-stderr release tag "$repo" --changelog
    assert_success
    assert_output "committed CHANGELOG.md
tagged v1.5.0"
    [ "$(git -C "$repo" rev-parse HEAD^)" = "$before" ]
    [ "$(git -C "$repo" log -1 --format=%s)" = "chore(release): v1.5.0" ]
    [ "$(git -C "$repo" diff --name-only HEAD^ HEAD)" = "CHANGELOG.md" ]
    [ "$(git -C "$repo" rev-parse 'v1.5.0^{commit}')" = "$(git -C "$repo" rev-parse HEAD)" ]
    [ "$(git -C "$repo" tag -l --format='%(contents)' v1.5.0)" = "$notes" ]
    [ "$(cat "$repo/CHANGELOG.md")" = "# Changelog

$notes" ]
    [ -z "$(git -C "$repo" status --porcelain)" ]
    run --separate-stderr release next "$repo"
    assert_failure 1
    [ "$stderr" = "release.sh: nothing to release since v1.5.0" ]
}

@test "branches: newer entries go on top of the CHANGELOG" {
    commit "feat: first"
    release_tag v1.0.0
    commit "fix: one"
    run release tag "$repo" --changelog
    first=$(git -C "$repo" tag -l --format='%(contents)' v1.0.1)
    commit "feat: two"
    run release tag "$repo" --pre rc --changelog
    assert_success
    assert_output "committed CHANGELOG.md
tagged v1.1.0-rc.1"
    second=$(git -C "$repo" tag -l --format='%(contents)' v1.1.0-rc.1)
    [ "$(cat "$repo/CHANGELOG.md")" = "# Changelog

$second

$first" ]
}

@test "branches: a CHANGELOG without its title gets one" {
    commit "feat: first"
    release_tag v1.0.0
    printf 'Old notes, written by hand.\n' > "$BATS_TEST_TMPDIR/old"
    cp "$BATS_TEST_TMPDIR/old" "$repo/CHANGELOG.md"
    git -C "$repo" add CHANGELOG.md
    commit "fix: one"
    run release tag "$repo" --changelog
    assert_success
    notes=$(git -C "$repo" tag -l --format='%(contents)' v1.0.1)
    [ "$(cat "$repo/CHANGELOG.md")" = "# Changelog

$notes

Old notes, written by hand." ]
}

@test "branches: --changelog keeps the refusals" {
    commit "feat: first"
    release_tag v1.0.0
    commit "docs: guide"
    before=$(git -C "$repo" rev-parse HEAD)
    run --separate-stderr release tag "$repo" --changelog
    assert_failure 1
    [ "$(git -C "$repo" rev-parse HEAD)" = "$before" ]
    assert_file_not_exist "$repo/CHANGELOG.md"
    commit "fix: one"
    echo "edit" >> "$repo/changes/1.txt"
    run --separate-stderr release tag "$repo" --changelog
    assert_failure 1
    [ "$stderr" = "release.sh: uncommitted changes" ]
    assert_file_not_exist "$repo/CHANGELOG.md"
}
