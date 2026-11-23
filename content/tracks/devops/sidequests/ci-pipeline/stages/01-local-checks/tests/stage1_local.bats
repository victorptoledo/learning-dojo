bats_require_minimum_version 1.5.0

setup() {
    source "$BATS_TEST_DIRNAME/helpers.sh"
}

@test "lint passes on the toolbox and counts the files it checked" {
    run ci lint
    assert_success
    assert_line --index $((${#lines[@]} - 1)) "lint: ok ($(lintable) files)"
}

@test "lint fails on a ShellCheck warning and names the file" {
    broken_script bin/broken.sh
    run ci lint
    assert_failure 1
    assert_output --partial "bin/broken.sh"
    assert_output --partial "SC2164"
    assert_line --index $((${#lines[@]} - 1)) "lint: failed (1 of $(lintable) files)"
}

@test "lint checks every file, even after a bad one" {
    broken_script bin/a-broken.sh
    broken_script bin/z-broken.sh
    run ci lint
    assert_failure 1
    assert_output --partial "bin/a-broken.sh"
    assert_output --partial "bin/z-broken.sh"
    assert_line --index $((${#lines[@]} - 1)) "lint: failed (2 of $(lintable) files)"
}

@test "lint looks into subfolders of bin and scripts, and nowhere else" {
    broken_script scripts/release/tag.sh
    broken_script docs/example.sh
    broken_script test/fixture.sh
    run ci lint
    assert_failure 1
    assert_output --partial "scripts/release/tag.sh"
    refute_output --partial "docs/example.sh"
    refute_output --partial "test/fixture.sh"
}

@test "test runs the toolbox's tests and writes a JUnit report" {
    run ci test
    assert_success
    assert_line --index $((${#lines[@]} - 1)) "test: ok"

    [ -f "$repo/reports/report.xml" ] || { echo "missing reports/report.xml"; return 1; }
    run grep -c '<testcase ' "$repo/reports/report.xml"
    assert_output "4"
}

@test "test fails when a toolbox test fails, and still writes the report" {
    put test/extra.bats '@test "a test that fails" {
    [ 1 -eq 2 ]
}'
    run ci test
    assert_failure 1
    assert_line --index $((${#lines[@]} - 1)) "test: failed"
    run grep -c '<failure' "$repo/reports/report.xml"
    assert_output "1"
}

@test "test finds tests in subfolders and empties the reports folder first" {
    put test/unit/more.bats '@test "one more" {
    true
}'
    put reports/stale.xml '<testsuites/>'
    run ci test
    assert_success
    [ ! -e "$repo/reports/stale.xml" ] || { echo "reports/stale.xml was left behind"; return 1; }
    run grep -c '<testcase ' "$repo/reports/report.xml"
    assert_output "5"
}

@test "test without any .bats file fails" {
    rm -rf "$repo/test"
    run ci test
    assert_failure 1
    assert_output --partial "test: no tests in test/"
}

@test "all lints, tests and ends with ci: ok; it is also the default" {
    run ci all
    assert_success
    assert_line --index $((${#lines[@]} - 1)) "ci: ok"
    [ -f "$repo/reports/report.xml" ]
    rm -rf "$repo/reports"
    run ci
    assert_success
    assert_line --index $((${#lines[@]} - 1)) "ci: ok"
}

@test "all stops at a lint failure before running the tests" {
    broken_script bin/broken.sh
    run ci all
    assert_failure 1
    assert_output --partial "lint: failed"
    refute_output --partial "test: ok"
    [ ! -e "$repo/reports/report.xml" ] || { echo "the tests ran after the lint failed"; return 1; }
}

@test "all fails when a test fails" {
    put test/extra.bats '@test "a test that fails" {
    false
}'
    run ci
    assert_failure 1
    refute_output --partial "ci: ok"
}

@test "an unknown command or two arguments print the usage and exit 2" {
    run --separate-stderr ci deploy
    assert_failure 2
    [ "$stderr" = "usage: ci.sh [lint|test|all]" ] || { echo "stderr: $stderr"; return 1; }
    run --separate-stderr ci lint test
    assert_failure 2
}
