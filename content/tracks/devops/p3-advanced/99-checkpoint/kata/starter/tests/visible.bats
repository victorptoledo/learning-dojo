load /opt/dojo/devops

@test "the fast-burn alert pages" {
    run rule slo.rules.yml PaymentsLatencyBudgetBurnFast '.labels.severity'
    assert_output "page"
}

@test "dev and staging share one NAT gateway" {
    terraform init -backend=false -input=false > /dev/null
    run terraform plan -input=false -var-file=prod.tfvars -var environment=dev -no-color
    assert_success
    assert_output --partial 'nat_gateway_zones = [
      + "eu-west-1a",
    ]'
}
