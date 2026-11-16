load /opt/dojo/devops

paths='**.tf,.github/workflows/terraform.yml'

@test "it runs on pull requests and pushes to main that touch .tf files or the workflow" {
    run wf terraform.yml '.on.pull_request.paths // [] | sort | join(",")'
    assert_output "$paths"
    run wf terraform.yml '"\(.on.push.branches // [] | join(",")) \(.on.push.paths // [] | sort | join(","))"'
    assert_output "main $paths"
}

@test "the token is read-only by default and every action is pinned" {
    run wf terraform.yml '.permissions | tojson'
    assert_output '{"contents":"read"}'
    policy terraform.yml explicit_permissions
    policy terraform.yml pinned_actions
    policy terraform.yml timeouts
}

@test "runs are serialised in one group and never cancelled" {
    run wf terraform.yml '"\(.concurrency.group) \(.concurrency["cancel-in-progress"])"'
    assert_output "terraform false"
}

@test "plan runs only for pull requests, apply only for pushes to main" {
    run wf terraform.yml '.jobs.plan.if | gsub("\\s+"; " ")'
    assert_output --partial "github.event_name == 'pull_request'"
    run wf terraform.yml '.jobs.apply.if'
    assert_output --partial "github.event_name == 'push'"
    assert_output --partial "github.ref == 'refs/heads/main'"
}

@test "apply runs in the protected production environment" {
    run wf terraform.yml '.jobs.apply.environment | if type == "object" then .name else . end'
    assert_output "production"
}

@test "both jobs get AWS credentials through OIDC, each with its own role" {
    for job in plan apply; do
        run wf terraform.yml '.jobs[$job].permissions | tojson' --arg job "$job"
        assert_output '{"contents":"read","id-token":"write"}'
        run wf terraform.yml '[.jobs[$job].steps[] | select((.uses // "") | startswith("aws-actions/configure-aws-credentials@")) | .with["role-to-assume"]] | first // ""' --arg job "$job"
        assert_output "arn:aws:iam::123456789012:role/terraform-$job"
    done
}

@test "no static cloud keys appear anywhere" {
    run grep -nEi 'aws-access-key-id|aws-secret-access-key|secrets\.AWS' .github/workflows/terraform.yml
    assert_failure
}

@test "Terraform 1.17.2 is installed in both jobs" {
    run wf terraform.yml '[.jobs[].steps[] | select((.uses // "") | startswith("hashicorp/setup-terraform@")) | .with.terraform_version | tostring] | join(",")'
    assert_output "1.17.2,1.17.2"
}

@test "plan checks formatting, inits, validates, then saves the plan as an artifact" {
    fmt=$(step_index terraform.yml plan 'terraform fmt -check')
    init=$(step_index terraform.yml plan 'terraform init -input=false -backend-config=backend.hcl')
    validate=$(step_index terraform.yml plan 'terraform validate')
    plan=$(step_index terraform.yml plan 'terraform plan -input=false.*-var-file=prod\.tfvars.*-out=tfplan|terraform plan -input=false.*-out=tfplan.*-var-file=prod\.tfvars')
    upload=$(step_index terraform.yml plan '^actions/upload-artifact@')
    echo "fmt=$fmt init=$init validate=$validate plan=$plan upload=$upload"
    [ "$fmt" -ge 0 ] && [ "$init" -gt "$fmt" ] && [ "$validate" -gt "$init" ] && [ "$plan" -gt "$validate" ] && [ "$upload" -gt "$plan" ]
}

@test "apply inits and applies prod.tfvars without prompting" {
    init=$(step_index terraform.yml apply 'terraform init -input=false -backend-config=backend.hcl')
    apply=$(step_index terraform.yml apply 'terraform apply -input=false -auto-approve.*-var-file=prod\.tfvars|terraform apply.*-var-file=prod\.tfvars.*-auto-approve')
    echo "init=$init apply=$apply"
    [ "$init" -ge 0 ] && [ "$apply" -gt "$init" ]
}

@test "module.web is moved to module.service[\"web\"], not destroyed" {
    run bash -c "tr -d ' \t\n' < main.tf"
    assert_output --partial 'moved{from=module.webto=module.service["web"]}'
    run grep -c '^module "web"' main.tf
    assert_output "0"
}
