load /opt/dojo/devops

@test "test runs on ubuntu, macos and an experimental ARM runner" {
    run wf ci.yml '.jobs.test.strategy.matrix.os | join(",")'
    assert_output "ubuntu-24.04,macos-15"
    run wf ci.yml '.jobs.test.strategy.matrix.include | tojson'
    assert_output '[{"os":"ubuntu-24.04-arm","experimental":true}]'
    run wf ci.yml '.jobs.test["runs-on"] | gsub("[[:space:]]"; "")'
    assert_output '${{matrix.os}}'
}

@test "one failing runner does not cancel the others" {
    run wf ci.yml '.jobs.test.strategy["fail-fast"] | tostring'
    assert_output "false"
}

@test "only the experimental runner may fail without failing the workflow" {
    run wf ci.yml '.jobs.test["continue-on-error"] | tostring'
    assert_output --partial "matrix.experimental"
    run wf ci.yml '.jobs.lint | has("continue-on-error")'
    assert_output "false"
}

@test "the cache key of both jobs depends on the CPU too" {
    for job in lint test; do
        run wf ci.yml '[.jobs[$job].steps[] | select((.uses // "") | startswith("actions/cache@")) | .with] | first // {} | "\(.key | gsub("[[:space:]]"; ""))|\(.["restore-keys"] // "" | gsub("[[:space:]]"; ""))"' --arg job "$job"
        assert_output "toolbox-\${{runner.os}}-\${{runner.arch}}-\${{hashFiles('tools.lock')}}|toolbox-\${{runner.os}}-\${{runner.arch}}-"
    done
}

@test "each runner uploads its JUnit report after the tests, unless cancelled" {
    test=$(step_index ci.yml test 'scripts/ci\.sh test')
    upload=$(step_index ci.yml test '^actions/upload-artifact@')
    echo "test=$test upload=$upload"
    [ "$test" -ge 0 ] && [ "$upload" -gt "$test" ]
    run wf ci.yml '.jobs.test.steps[$i | tonumber] | "\(.with.name | gsub("[[:space:]]"; ""))|\(.with.path | sub("/$"; ""))|\(.if // "" | gsub("[[:space:]]"; ""))"' --arg i "$upload"
    assert_output --regexp '^junit-\$\{\{matrix\.os\}\}\|reports\|(\$\{\{)?!cancelled\(\)(\}\})?$'
}

@test "reports are kept for at most 14 days" {
    run wf ci.yml '[.jobs.test.steps[] | select((.uses // "") | startswith("actions/upload-artifact@")) | .with["retention-days"]] | first | . != null and . <= 14'
    assert_output "true"
}

@test "ci-ok needs lint and test and always runs" {
    run wf ci.yml '[.jobs["ci-ok"].needs] | flatten | sort | join(",")'
    assert_output "lint,test"
    run wf ci.yml '.jobs["ci-ok"].if // "" | gsub("[[:space:]]"; "")'
    assert_output --regexp '^(\$\{\{)?always\(\)(\}\})?$'
    run wf ci.yml '.jobs["ci-ok"]["runs-on"]'
    assert_output "ubuntu-24.04"
    policy ci.yml timeouts
}

# The run script of the ci-ok step that reads RESULTS.
gate() {
    wf ci.yml '[.jobs["ci-ok"].steps[] | select(.env.RESULTS)] | first | .run // ""'
}

@test "ci-ok reads the results of every job it needs" {
    run wf ci.yml '[.jobs["ci-ok"].steps[] | select(.env.RESULTS)] | first | .env.RESULTS | gsub("[[:space:]]"; "")'
    assert_output "\${{join(needs.*.result,'')}}"
}

@test "ci-ok passes when every job succeeded" {
    script=$(gate)
    [ -n "$script" ] || { echo "todo: no ci-ok step with env RESULTS"; return 1; }
    RESULTS="success success" run timeout 10 bash -c "$script"
    assert_success
}

@test "ci-ok fails on a failure, a cancellation or a skipped job" {
    script=$(gate)
    [ -n "$script" ] || { echo "todo: no ci-ok step with env RESULTS"; return 1; }
    for results in "success failure" "failure success" "cancelled success" "success skipped" "failure failure"; do
        RESULTS="$results" run timeout 10 bash -c "$script"
        [ "$status" -ne 0 ] || { echo "ci-ok passed with RESULTS='$results'"; return 1; }
    done
}
