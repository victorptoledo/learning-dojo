setup() {
    source "$BATS_TEST_DIRNAME/helpers.sh"
}

@test "next: a feat bumps the minor version" {
    commit "feat: first"
    release_tag v1.4.2
    commit "docs: explain coupons"
    commit "feat(cart): add coupon codes"
    commit "fix: round VAT per line"
    run --separate-stderr release next "$repo"
    assert_success
    assert_output "v1.5.0"
}

@test "next: fix and perf bump the patch version" {
    commit "feat: first"
    release_tag v1.4.2
    commit "fix(tax): round per line"
    run release next "$repo"
    assert_output "v1.4.3"
    release_tag v1.4.3
    commit "perf: cache the price list"
    run release next "$repo"
    assert_success
    assert_output "v1.4.4"
}

@test "next: a bang or a BREAKING CHANGE footer bumps the major version" {
    commit "feat: first"
    release_tag v1.4.2
    commit "fix: small"
    commit "refactor(api)!: rename the order endpoints"
    run release next "$repo"
    assert_success
    assert_output "v2.0.0"
    release_tag v2.0.0
    commit "feat: coupons\n\nCoupons replace vouchers.\n\nBREAKING CHANGE: the /vouchers endpoint is gone\n"
    run release next "$repo"
    assert_output "v3.0.0"
    release_tag v3.0.0
    commit "fix: totals\n\nBREAKING-CHANGE: totals are now in cents\n"
    run release next "$repo"
    assert_output "v4.0.0"
}

@test "next: a footer must start its line to be breaking" {
    commit "feat: first"
    release_tag v1.0.0
    commit "fix: docs links\n\nWe still say BREAKING CHANGE: in the guide, on purpose.\n"
    run release next "$repo"
    assert_success
    assert_output "v1.0.1"
}

@test "next: 0.x versions bump one place less" {
    commit "feat: first"
    release_tag v0.3.1
    commit "feat(cart)!: drop the old cart"
    run release next "$repo"
    assert_output "v0.4.0"
    release_tag v0.4.0
    commit "feat: coupons"
    run release next "$repo"
    assert_output "v0.4.1"
    release_tag v0.4.1
    commit "fix: coupons"
    run release next "$repo"
    assert_output "v0.4.2"
}

@test "next: the first release is v0.1.0" {
    commit "chore: initial commit"
    run --separate-stderr release next "$repo"
    assert_success
    assert_output "v0.1.0"
}

@test "next: subjects that are not conventional change nothing" {
    commit "feat: first"
    release_tag v1.0.0
    for subject in "Added coupons" "Feat: coupons" "feat:coupons" "feat (cart): coupons" \
        "fix(Cart): totals" "fixup! feat: first" "feature: coupons" "docs: readme" \
        "chore(deps): bump yaml"; do
        commit "$subject"
    done
    run --separate-stderr release next "$repo"
    assert_failure 1
    [ "$stderr" = "release.sh: nothing to release since v1.0.0" ]
    assert_output ""
}

@test "next: the last release is the highest vX.Y.Z tag reachable from HEAD" {
    commit "feat: first"
    release_tag v1.9.3
    commit "feat: second"
    release_tag v1.10.0
    git -C "$repo" tag latest
    git -C "$repo" tag v1.11
    git -C "$repo" tag v01.12.0
    commit "feat!: third"
    git -C "$repo" tag -a -m rc v2.0.0-rc.1
    git -C "$repo" tag version-3
    commit "fix: after the candidate"
    git -C "$repo" switch -q -c experiment
    commit "feat: only on experiment"
    release_tag v5.0.0
    git -C "$repo" switch -q main
    run release next "$repo"
    assert_success
    assert_output "v2.0.0"
}

@test "next: merge commits are not read" {
    commit "feat: first"
    release_tag v1.0.0
    git -C "$repo" switch -q -c topic
    commit "docs: guide"
    git -C "$repo" switch -q main
    commit "chore: tidy"
    git -C "$repo" merge -q --no-ff -m "feat: merged by a robot" topic
    run --separate-stderr release next "$repo"
    assert_failure 1
    [ "$stderr" = "release.sh: nothing to release since v1.0.0" ]
}

@test "next: nothing to release, no commits, and the repository left alone" {
    run --separate-stderr release next "$repo"
    assert_failure 1
    [ "$stderr" = "release.sh: no commits" ]
    commit "feat: first"
    release_tag v1.0.0
    run --separate-stderr release next "$repo"
    assert_failure 1
    [ "$stderr" = "release.sh: nothing to release since v1.0.0" ]
    commit "feat: second"
    before=$(refs)
    run release next "$repo"
    assert_success
    [ "$(refs)" = "$before" ]
}

@test "release rejects bad arguments" {
    usage="usage: release.sh next|notes|tag REPO [OPTIONS]"
    run --separate-stderr release
    assert_failure 2
    [ "$stderr" = "$usage" ]
    run --separate-stderr release publish "$repo"
    assert_failure 2
    [ "$stderr" = "$usage" ]
    run --separate-stderr release next
    assert_failure 2
    [ "$stderr" = "$usage" ]
    run --separate-stderr release next "$repo" extra
    assert_failure 2
    [ "$stderr" = "$usage" ]
    mkdir "$BATS_TEST_TMPDIR/plain"
    run --separate-stderr release next "$BATS_TEST_TMPDIR/plain"
    assert_failure 2
    [ "$stderr" = "release.sh: not a git repository: $BATS_TEST_TMPDIR/plain" ]
}
