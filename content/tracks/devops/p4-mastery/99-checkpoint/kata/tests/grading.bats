load /opt/dojo/devops

# Two services are generated once for the whole file: orders (critical, team checkout) and
# search (standard, team discovery). Each test then checks the generated files with the
# same tools the rest of the track used.
setup_file() {
    export GEN="$BATS_FILE_TMPDIR/gen"
    mkdir -p "$GEN"
    bash new-service.sh --name orders --team checkout --tier critical --out "$GEN" > /dev/null 2>&1 || true
    bash new-service.sh --name search --team discovery --tier standard --out "$GEN" > /dev/null 2>&1 || true
}

# generated SERVICE: fails when the service was not generated.
generated() {
    [ -d "$GEN/$1" ] || { echo "new-service.sh did not create $1"; return 1; }
}

@test "bad input exits 2, says why on stderr and writes nothing" {
    while IFS='|' read -r label args; do
        dir="$BATS_TEST_TMPDIR/$label"
        mkdir -p "$dir"
        # shellcheck disable=SC2086
        run --separate-stderr bash new-service.sh $args --out "$dir"
        echo "case $label: status $status, stderr: $stderr"
        [ "$status" -eq 2 ]
        [ -n "$stderr" ]
        [ -z "$(ls -A "$dir")" ]
    done <<'EOF'
uppercase|--name Orders --team checkout --tier critical
too-short|--name ab --team checkout --tier critical
trailing-dash|--name orders- --team checkout --tier critical
underscore|--name orders_api --team checkout --tier critical
bad-team|--name orders --team Checkout! --tier critical
no-team|--name orders --tier critical
unknown-tier|--name orders --team checkout --tier gold
no-tier|--name orders --team checkout
unknown-option|--name orders --team checkout --tier critical --force
EOF
}

@test "an existing service is never overwritten: exit 1 and the files stay" {
    dir="$BATS_TEST_TMPDIR/existing"
    mkdir -p "$dir/orders"
    echo "keep me" > "$dir/orders/RUNBOOK.md"
    run --separate-stderr bash new-service.sh --name orders --team checkout --tier critical --out "$dir"
    [ "$status" -eq 1 ]
    [ "$(cat "$dir/orders/RUNBOOK.md")" = "keep me" ]
    [ "$(ls -A "$dir")" = "orders" ]
    [ "$(ls -A "$dir/orders")" = "RUNBOOK.md" ]
}

@test "a failure halfway leaves no half-made service behind" {
    broken="$BATS_TEST_TMPDIR/broken"
    mkdir -p "$broken/out"
    cp -r new-service.sh templates "$broken/"
    rm "$broken/templates/RUNBOOK.md.tpl"
    run bash "$broken/new-service.sh" --name orders --team checkout --tier critical --out "$broken/out"
    assert_failure
    run ls -A "$broken/out"
    assert_output ""
}

@test "the service gets exactly the golden path's files, and nothing is left aside" {
    generated orders
    run bash -c "cd '$GEN/orders' && find . -type f | sort"
    assert_output "$(printf '%s\n' ./.github/workflows/ci.yml ./RUNBOOK.md ./catalog-info.yaml ./infra/main.tf ./infra/terraform.tfvars ./slo/orders.rules.yml)"
    run ls -A "$GEN"
    assert_output "$(printf '%s\n' orders search)"
}

@test "the same input always gives the same bytes" {
    generated orders
    again="$BATS_TEST_TMPDIR/again"
    mkdir -p "$again"
    run bash new-service.sh --name orders --team checkout --tier critical --out "$again"
    assert_success
    run diff -r "$GEN/orders" "$again/orders"
    assert_success
}

@test "every placeholder is filled" {
    generated orders
    generated search
    run bash -c "grep -rnE '(^|[^$])\{\{' '$GEN'"
    assert_failure
}

@test "the workflow passes actionlint (and ShellCheck on its scripts)" {
    generated orders
    cd "$GEN/orders"
    run actionlint -no-color -oneline .github/workflows/ci.yml
    assert_success
}

@test "the workflow has a read-only token, pinned actions, timeouts and no untrusted input in scripts" {
    generated orders
    cd "$GEN/orders"
    run wf ci.yml '.permissions | tojson'
    assert_output '{"contents":"read"}'
    policy ci.yml explicit_permissions
    policy ci.yml pinned_actions
    policy ci.yml timeouts
    policy ci.yml no_untrusted_in_run
    run wf ci.yml '[.jobs[].steps[] | select((.uses // "") | startswith("actions/checkout@")) | .with["persist-credentials"] | tostring] | unique | join(",")'
    assert_output "false"
}

@test "the workflow tests pull requests and pushes to main, and only main deploys" {
    generated orders
    cd "$GEN/orders"
    run wf ci.yml '"\(.on.push.branches | join(",")) \(.on | has("pull_request"))"'
    assert_output "main true"
    run wf ci.yml '.jobs.deploy.needs | if type == "array" then join(",") else . end'
    assert_output "test"
    run wf ci.yml '.jobs.deploy.if'
    assert_output --partial "github.event_name == 'push'"
    assert_output --partial "github.ref == 'refs/heads/main'"
    run wf ci.yml '.jobs.deploy.environment | if type == "object" then .name else . end'
    assert_output "production"
}

@test "a deploy is never cancelled halfway, while pull request runs are" {
    generated orders
    cd "$GEN/orders"
    run wf ci.yml '"\(.jobs.deploy.concurrency.group) \(.jobs.deploy.concurrency["cancel-in-progress"])"'
    assert_output "deploy-orders false"
    run wf ci.yml '.concurrency["cancel-in-progress"] | tostring | gsub("\\s+"; "")'
    assert_output "\${{github.event_name=='pull_request'}}"
}

@test "deploy assumes the service's own role through OIDC, with no stored keys" {
    generated search
    cd "$GEN/search"
    run wf ci.yml '.jobs.deploy.permissions | tojson'
    assert_output '{"contents":"read","id-token":"write"}'
    run wf ci.yml '[.jobs.deploy.steps[] | select((.uses // "") | startswith("aws-actions/configure-aws-credentials@")) | .with["role-to-assume"]] | first // ""'
    assert_output "arn:aws:iam::123456789012:role/deploy-search"
    run grep -nEi 'aws-access-key-id|aws-secret-access-key|secrets\.' .github/workflows/ci.yml
    assert_failure
}

@test "the SLO rules pass promtool check" {
    generated orders
    generated search
    run promtool check rules "$GEN/orders/slo/orders.rules.yml" "$GEN/search/slo/search.rules.yml"
    assert_success
}

@test "a critical service pages at 14.4 times a 0.1% budget (promtool test)" {
    generated orders
    cp tests/slo/orders.test.yml "$GEN/orders/slo/"
    cd "$GEN/orders/slo"
    run promtool test rules orders.test.yml
    assert_success
}

@test "a standard service tickets but does not page at 4% errors (promtool test)" {
    generated search
    cp tests/slo/search.test.yml "$GEN/search/slo/"
    cd "$GEN/search/slo"
    run promtool test rules search.test.yml
    assert_success
}

@test "the only alerts are the two burn-rate alerts" {
    generated orders
    run yaml "$GEN/orders/slo/orders.rules.yml" '[.groups[].rules[] | .alert // empty] | sort | join(",")'
    assert_output "ErrorBudgetBurnFast,ErrorBudgetBurnSlow"
}

@test "the infra module is formatted and valid" {
    generated orders
    cd "$GEN/orders/infra"
    run terraform fmt -check -no-color
    assert_success
    terraform init -backend=false -input=false > /dev/null
    run terraform validate -no-color
    assert_success
}

@test "terraform.tfvars names the service, its team and its tier" {
    generated search
    run cat "$GEN/search/infra/terraform.tfvars"
    assert_output "$(printf '%s\n' 'service = "search"' 'team    = "discovery"' 'tier    = "standard"')"
}

@test "the backup policy follows the tier (terraform test)" {
    generated orders
    mkdir -p "$GEN/orders/infra/tests"
    cp tests/infra/backup.tftest.hcl "$GEN/orders/infra/tests/"
    cd "$GEN/orders/infra"
    terraform init -backend=false -input=false > /dev/null
    run terraform test -no-color
    echo "$output"
    assert_success
    assert_output --partial "5 passed, 0 failed"
}

@test "the plan of a generated service shows its tier's RPO" {
    generated orders
    cd "$GEN/orders/infra"
    terraform init -backend=false -input=false > /dev/null
    run terraform plan -input=false -no-color
    assert_success
    assert_output --partial "rpo_minutes             = 15"
}

@test "the runbook has every section, in order" {
    generated orders
    run grep -E '^## ' "$GEN/orders/RUNBOOK.md"
    assert_output "$(printf '%s\n' '## Severity' '## Roles' '## First five minutes' '## Restore from backup' '## After the incident')"
    run grep -cE '^- (Incident commander|Operations|Communications):' "$GEN/orders/RUNBOOK.md"
    assert_output "3"
    run grep -c 'postmortem' "$GEN/orders/RUNBOOK.md"
    refute_output "0"
}

@test "the runbook says who owns the service and its SLO" {
    generated search
    run sed -n '1,5p' "$GEN/search/RUNBOOK.md"
    assert_output "$(printf '%s\n' '# search runbook' '' '- Owner: team discovery' '- Tier: standard' '- SLO: 99.5% of requests succeed over 30 days')"
}

@test "the runbook mitigates before it diagnoses" {
    generated orders
    rollback=$(grep -niE '^[0-9]+\..*roll back' "$GEN/orders/RUNBOOK.md" | cut -d: -f1 | head -1)
    diagnose=$(grep -niE '^[0-9]+\..*diagnose' "$GEN/orders/RUNBOOK.md" | cut -d: -f1 | head -1)
    echo "roll back at line ${rollback:-none}, diagnose at line ${diagnose:-none}"
    [ -n "$rollback" ] && [ -n "$diagnose" ] && [ "$rollback" -lt "$diagnose" ]
}

@test "the runbook states the tier's recovery objectives" {
    generated orders
    generated search
    run grep -E '^- (RPO|RTO): ' "$GEN/orders/RUNBOOK.md"
    assert_output "$(printf '%s\n' '- RPO: 15 minutes' '- RTO: 2 hours')"
    run grep -E '^- (RPO|RTO): ' "$GEN/search/RUNBOOK.md"
    assert_output "$(printf '%s\n' '- RPO: 24 hours' '- RTO: 8 hours')"
}

@test "the catalog entry names the owner group and the tier" {
    generated search
    run yaml "$GEN/search/catalog-info.yaml" '"\(.metadata.name) \(.spec.owner) \(.metadata.annotations["dojo.dev/tier"])"'
    assert_output "search group:discovery standard"
}
