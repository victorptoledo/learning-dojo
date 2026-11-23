bats_require_minimum_version 1.5.0

setup() {
    source "$BATS_TEST_DIRNAME/helpers.sh"
    source "$BATS_TEST_DIRNAME/fakes.sh"
    source "$BATS_TEST_DIRNAME/runner.sh"
    cluster 1
    kubeconfig="apiVersion: v1 # the test cluster"
}

# tf_apply ENV: renders environments/ENV.tfvars in the copy, as CI does.
tf_apply() {
    (cd "$repo" && timeout 120 terraform init -input=false > "$BATS_TEST_TMPDIR/init.log" 2>&1 \
        && timeout 120 terraform apply -input=false -auto-approve -no-color "-var-file=environments/$1.tfvars" 3>&-) \
        > "$BATS_TEST_TMPDIR/apply.log" 2>&1 || { cat "$BATS_TEST_TMPDIR/init.log" "$BATS_TEST_TMPDIR/apply.log"; return 1; }
}

deploy() {
    local job=$1
    shift
    KUBE_EXPECT_CONFIG=$kubeconfig run_job deploy.yml "$job" \
        "inputs.version=1.9.0" "secrets.KUBECONFIG=$(printf '%s' "$kubeconfig" | base64 -w0)" "$@"
}

@test "the module declares its variables and outputs" {
    run tf_variables .
    for variable in environment failure_limit interval max_error_ratio namespace output_dir query service steps; do
        assert_line "$variable"
    done
    run tf_outputs .
    assert_output "file
first_weight
steps"
}

@test "staging gets a short rollout with a looser limit" {
    tf_apply staging
    run --separate-stderr rollout plan "$repo/rollouts/staging/checkout.yaml"
    assert_success
    assert_output "checkout: 2 steps, analysis every 30s (max error ratio 0.02, 1 failures roll back)
step 1/2: weight 50, pause 120s, 4 checks
step 2/2: weight 100"
}

@test "production goes slowly, with a strict limit" {
    tf_apply production
    run --separate-stderr rollout plan "$repo/rollouts/production/checkout.yaml"
    assert_success
    assert_output "checkout: 4 steps, analysis every 60s (max error ratio 0.01, 2 failures roll back)
step 1/4: weight 10, pause 300s, 5 checks
step 2/4: weight 25, pause 300s, 5 checks
step 3/4: weight 50, pause 600s, 10 checks
step 4/4: weight 100"
}

@test "the rendered file drives the controller from start to end" {
    tf_apply staging
    run --separate-stderr rollout run "$repo/rollouts/staging/checkout.yaml"
    assert_success
    run weights
    assert_output "50 50
0 100"
    run grep -c 'service="checkout-canary"' "$PROMQ_LOG"
    assert_output "4"
}

@test "deploy is started by hand with the version to release" {
    run wf deploy.yml '.on | keys | join(",")'
    assert_output "workflow_dispatch"
    run wf deploy.yml '.on.workflow_dispatch.inputs | to_entries | map("\(.key):\(.value.required):\(.value.type)") | join(",")'
    assert_output "version:true:string"
}

@test "one deploy at a time, never cancelled by the next, with a read-only token" {
    run wf deploy.yml '.permissions | tojson'
    assert_output '{"contents":"read"}'
    run wf deploy.yml '"\(.concurrency.group | test("\\$\\{\\{") | not) \(.concurrency["cancel-in-progress"])"'
    assert_output "true false"
    policy deploy.yml pinned_actions
    policy deploy.yml timeouts
    policy deploy.yml no_untrusted_in_run
}

@test "staging first, then production, each in its environment" {
    run wf deploy.yml '.jobs | keys | sort | join(",")'
    assert_output "production,staging"
    run wf deploy.yml '[.jobs.production.needs] | flatten | join(",")'
    assert_output "staging"
    for job in staging production; do
        run wf deploy.yml '.jobs[$job].environment | if type == "object" then .name else . end' --arg job "$job"
        assert_output "$job"
    done
}

@test "both jobs use Terraform 1.17.2 and render their own environment" {
    run wf deploy.yml '[.jobs[].steps[] | select((.uses // "") | startswith("hashicorp/setup-terraform@")) | .with.terraform_version | tostring] | join(",")'
    assert_output "1.17.2,1.17.2"
    for job in staging production; do
        run step_index deploy.yml "$job" "terraform apply.*-var-file=environments/$job\\.tfvars"
        [ "$output" -ge 0 ] || { echo "job $job does not apply environments/$job.tfvars"; return 1; }
    done
}

@test "each job connects, renders, sets the image, then rolls out, in that order" {
    for job in staging production; do
        connect=$(step_index deploy.yml "$job" 'GITHUB_ENV')
        render=$(step_index deploy.yml "$job" 'terraform apply')
        image=$(step_index deploy.yml "$job" 'kubectl .*set image')
        roll=$(step_index deploy.yml "$job" "rollout\\.sh run .*rollouts/$job/checkout\\.yaml")
        echo "$job: connect=$connect render=$render image=$image roll=$roll"
        [ "$connect" -ge 0 ] && [ "$render" -gt "$connect" ] && [ "$image" -gt "$render" ] && [ "$roll" -gt "$image" ]
    done
}

@test "a cancelled deploy aborts its rollout" {
    for job in staging production; do
        run wf deploy.yml '[.jobs[$job].steps[] | select((.run // "") | test("rollout\\.sh abort"))] | first // {} | "\(.if // "" | gsub("[[:space:]]"; "")) \(.run // "" | test("rollouts/" + $job + "/checkout\\.yaml"))"' --arg job "$job"
        assert_output --regexp '^(\$\{\{)?cancelled\(\)(\}\})? true$'
    done
}

@test "the staging job releases the version given to staging" {
    run deploy staging
    assert_success
    run jq -r '.spec.template.spec.containers[] | select(.name == "app") | .image' "$KUBE_STATE/deployment-$CANARY.json"
    assert_output "ghcr.io/acme/checkout:1.9.0"
    run weights
    assert_output "50 50
0 100"
    [ -f "$repo/rollouts/staging/checkout.yaml" ]
}

@test "a bad canary fails the production job and is rolled back" {
    answers 0.2
    run deploy production
    assert_failure 3
    run weights
    assert_output "90 10
100 0"
    [ "$(replicas)" = 0 ]
}

@test "the abort step rolls back a rollout whose runner was killed" {
    tf_apply staging
    mkdir -p "$repo/.rollout"
    jq -n '{status: "running", step: 1, steps: 2, weight: 50, reason: ""}' > "$repo/.rollout/checkout.state"
    KUBE_EXPECT_CONFIG=$kubeconfig run_step deploy.yml staging 'GITHUB_ENV' \
        "secrets.KUBECONFIG=$(printf '%s' "$kubeconfig" | base64 -w0)" > /dev/null
    KUBE_EXPECT_CONFIG=$kubeconfig run run_step deploy.yml staging 'rollout\.sh abort'
    assert_success
    assert_output --partial "rollout: checkout aborted at step 1"
    run weights
    assert_output "100 0"
    [ "$(replicas)" = 0 ]
}
