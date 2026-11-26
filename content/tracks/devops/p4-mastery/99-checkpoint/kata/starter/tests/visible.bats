load /opt/dojo/devops

@test "a critical service is created with the golden path's files" {
    run bash new-service.sh --name orders --team checkout --tier critical --out "$BATS_TEST_TMPDIR"
    assert_success
    run bash -c "cd '$BATS_TEST_TMPDIR/orders' && find . -type f | sort"
    assert_output "$(printf '%s\n' ./.github/workflows/ci.yml ./RUNBOOK.md ./catalog-info.yaml ./infra/main.tf ./infra/terraform.tfvars ./slo/orders.rules.yml)"
}

@test "an unknown tier exits 2" {
    run bash new-service.sh --name orders --team checkout --tier gold --out "$BATS_TEST_TMPDIR"
    assert_equal "$status" 2
}
