load /opt/dojo/devops

records='job:slo_slow_requests:ratio_rate1h job:slo_slow_requests:ratio_rate30m job:slo_slow_requests:ratio_rate5m job:slo_slow_requests:ratio_rate6h'

@test "the four recording rules are in the group payments-slo-recordings" {
    run yaml slo.rules.yml '[.groups[] | select(.name == "payments-slo-recordings") | .rules[].record] | sort | join(" ")'
    assert_output "$records"
}

@test "the two alerts are in the group payments-slo-alerts, and nothing else alerts" {
    run yaml slo.rules.yml '[.groups[] | select(.name == "payments-slo-alerts") | .rules[].alert] | sort | join(",")'
    assert_output "PaymentsLatencyBudgetBurnFast,PaymentsLatencyBudgetBurnSlow"
    run yaml slo.rules.yml '[.groups[].rules[] | select(.alert)] | length'
    assert_output "2"
}

@test "the SLI counts requests in the le=\"0.3\" bucket, never a quantile" {
    run grep -c 'histogram_quantile' slo.rules.yml
    assert_output "0"
    for record in $records; do
        run rule slo.rules.yml "$record" '.expr | gsub("\\s+"; "")'
        assert_output --partial 'le="0.3"'
        assert_output --partial 'http_request_duration_seconds_count{job="payments"}'
    done
}

@test "no rule keeps or filters on the high-cardinality path label" {
    run bash -c "grep -v '^[[:space:]]*#' slo.rules.yml | grep -n path"
    assert_failure
}

@test "the alerts read the recorded ratios instead of recomputing them" {
    run rule slo.rules.yml PaymentsLatencyBudgetBurnFast '.expr'
    assert_output --partial "job:slo_slow_requests:ratio_rate1h"
    assert_output --partial "job:slo_slow_requests:ratio_rate5m"
    refute_output --partial "rate(http_request"
    run rule slo.rules.yml PaymentsLatencyBudgetBurnSlow '.expr'
    assert_output --partial "job:slo_slow_requests:ratio_rate6h"
    assert_output --partial "job:slo_slow_requests:ratio_rate30m"
    refute_output --partial "rate(http_request"
}

@test "the fast-burn alert holds 2m, the slow one 15m, and only the fast one pages" {
    run rule slo.rules.yml PaymentsLatencyBudgetBurnFast '"\(.for) \(.labels.severity)"'
    assert_output "2m page"
    run rule slo.rules.yml PaymentsLatencyBudgetBurnSlow '"\(.for) \(.labels.severity)"'
    assert_output "15m ticket"
}

@test "both alerts name the SLO and link its runbook" {
    for alert in PaymentsLatencyBudgetBurnFast PaymentsLatencyBudgetBurnSlow; do
        run rule slo.rules.yml "$alert" '"\(.labels.slo) \(.annotations.runbook_url)"'
        assert_output "payments-latency https://runbooks.example.com/payments-latency"
    done
}

@test "the module declares its inputs and outputs and creates nothing by itself" {
    run tf_variables .
    assert_output "$(printf '%s\n' environment rules tags vpc_cidr zones)"
    run tf_outputs .
    assert_output "$(printf '%s\n' gateway_endpoints ingress nat_gateway_zones private_subnets public_subnets tags)"
    run grep -lE '^(resource|provider|data|module) ' ./*.tf
    assert_failure
}

@test "prod.tfvars plans with one NAT gateway per zone" {
    terraform init -backend=false -input=false > /dev/null
    run terraform plan -input=false -var-file=prod.tfvars -no-color
    assert_success
    assert_output --partial '"eu-west-1c",'
}
