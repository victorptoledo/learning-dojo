load /opt/dojo/devops

@test "the fast-burn alert pages" {
    run rule slo.rules.yml CheckoutErrorBudgetBurnFast '.labels.severity'
    assert_output "page"
}
