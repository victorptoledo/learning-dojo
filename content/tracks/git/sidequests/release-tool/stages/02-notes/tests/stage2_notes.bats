setup() {
    source "$BATS_TEST_DIRNAME/helpers.sh"
}

@test "notes: sections in order with scopes and short hashes" {
    commit "feat: first"
    release_tag v1.4.2
    commit "docs: explain coupons"
    commit "feat(cart): add coupon codes"
    a=$(short HEAD)
    commit "fix: round VAT per line"
    b=$(short HEAD)
    commit "perf(search): index by SKU"
    c=$(short HEAD)
    commit "feat: search by SKU"
    d=$(short HEAD)
    commit "chore: bump deps"
    run --separate-stderr release notes "$repo"
    assert_success
    assert_output "## v1.5.0 (2024-03-07)

### Features

- **cart:** add coupon codes ($a)
- search by SKU ($d)

### Bug Fixes

- round VAT per line ($b)

### Performance

- **search:** index by SKU ($c)"
}

@test "notes: breaking changes come first and stay in their own type" {
    commit "feat: first"
    release_tag v1.4.2
    commit "fix: totals"
    a=$(short HEAD)
    commit "feat(api)!: drop the v1 endpoints"
    b=$(short HEAD)
    commit "refactor: vouchers\n\nVouchers become coupons.\n\nBREAKING CHANGE: the /vouchers endpoint is gone\nRefs: #12\n"
    c=$(short HEAD)
    run release notes "$repo"
    assert_success
    assert_output "## v2.0.0 (2024-03-04)

### ⚠ BREAKING CHANGES

- **api:** drop the v1 endpoints ($b)
- the /vouchers endpoint is gone ($c)

### Features

- **api:** drop the v1 endpoints ($b)

### Bug Fixes

- totals ($a)"
}

@test "notes: only the sections with entries" {
    commit "feat: first"
    release_tag v1.0.0
    commit "fix(tax): round per line"
    a=$(short HEAD)
    commit "docs: guide"
    run release notes "$repo"
    assert_success
    assert_output "## v1.0.1 (2024-03-03)

### Bug Fixes

- **tax:** round per line ($a)"
}

@test "notes: a first release without user-facing changes" {
    commit "chore: initial commit"
    commit "docs: readme"
    run release notes "$repo"
    assert_success
    assert_output "## v0.1.0 (2024-03-02)

No user-facing changes."
    commit "feat: login"
    a=$(short HEAD)
    run release notes "$repo"
    assert_output "## v0.1.0 (2024-03-03)

### Features

- login ($a)"
}

@test "notes: nothing to release prints nothing" {
    commit "feat: first"
    release_tag v1.0.0
    commit "docs: guide"
    run --separate-stderr release notes "$repo"
    assert_failure 1
    assert_output ""
    [ "$stderr" = "release.sh: nothing to release since v1.0.0" ]
    run --separate-stderr release notes "$repo" extra
    assert_failure 2
}
