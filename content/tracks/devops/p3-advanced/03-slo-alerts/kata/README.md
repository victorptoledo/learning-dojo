# SLO Alerts: Multiwindow, Multi-Burn-Rate Rules

The checkout service has an SLO: **99.9%** of its requests succeed (are not 5xx) over 30
days. Its error budget is therefore 0.1% of the requests. Write the Prometheus rules in
`slo.rules.yml` that page a human only when the budget is in real danger.

The counter is `http_requests_total` with the labels `job` and `code`. Only `job="checkout"`
counts.

**Recording rules** (group `checkout-slo-recordings`): the share of checkout requests that
failed, `sum by (job)` of the 5xx rate divided by `sum by (job)` of the total rate, over four
windows. Keep the `job` label.

| Record | Window |
| --- | --- |
| `job:slo_errors_per_request:ratio_rate5m` | `5m` |
| `job:slo_errors_per_request:ratio_rate30m` | `30m` |
| `job:slo_errors_per_request:ratio_rate1h` | `1h` |
| `job:slo_errors_per_request:ratio_rate6h` | `6h` |

**Alerts** (group `checkout-slo-alerts`), each reading the recorded ratios. The **burn rate**
is the error ratio divided by the budget (0.001): a burn rate of 1 spends the budget in
exactly 30 days.

| Alert | Fires when | `for` | `severity` | `summary` |
| --- | --- | --- | --- | --- |
| `CheckoutErrorBudgetBurnFast` | the 1h **and** the 5m ratio are above `14.4 * 0.001` | `2m` | `page` | `checkout is burning its error budget fast` |
| `CheckoutErrorBudgetBurnSlow` | the 6h **and** the 30m ratio are above `6 * 0.001` | `15m` | `ticket` | `checkout is burning its error budget` |

Both alerts also carry the label `slo: checkout-availability` and the annotation
`runbook_url: https://runbooks.example.com/checkout-slo`.

Why two windows each: the long window proves the burn is significant (14.4 for one hour is 2%
of the month's budget), the short one proves it is **still happening**, so the alert stops
soon after a fix and a one-minute spike after a clean hour does not page anyone.

The dojo checks the file with `promtool check rules` and runs hidden `promtool test rules`
files that feed series in and expect alerts (or none): a 2% error rate, a 0.8% error rate,
a healthy service, a short spike, and the recorded values. Run them yourself with
`promtool test rules tests/*.test.yml` if you have Prometheus.

There are 10 tests: 1 in `tests/visible.bats`, 3 hidden bats tests, `promtool check` and
5 `promtool test` files.
