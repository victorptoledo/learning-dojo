load /opt/dojo/devops

@test "the fast-burn alert must hold 2m and the slow one 15m" {
    run rule slo.rules.yml CheckoutErrorBudgetBurnFast '.for'
    assert_output "2m"
    run rule slo.rules.yml CheckoutErrorBudgetBurnSlow '.for'
    assert_output "15m"
}

@test "both alerts name the SLO and link a runbook" {
    for alert in CheckoutErrorBudgetBurnFast CheckoutErrorBudgetBurnSlow; do
        run rule slo.rules.yml "$alert" '"\(.labels.slo) \(.annotations.runbook_url)"'
        assert_output "checkout-availability https://runbooks.example.com/checkout-slo"
    done
}

@test "the alerts read the recorded ratios instead of recomputing them" {
    run rule slo.rules.yml CheckoutErrorBudgetBurnFast '.expr'
    assert_output --partial "job:slo_errors_per_request:ratio_rate1h"
    assert_output --partial "job:slo_errors_per_request:ratio_rate5m"
    run rule slo.rules.yml CheckoutErrorBudgetBurnSlow '.expr'
    assert_output --partial "job:slo_errors_per_request:ratio_rate6h"
    assert_output --partial "job:slo_errors_per_request:ratio_rate30m"
}
