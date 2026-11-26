# Checkpoint 3: Production Readiness for the Payments API

The payments API is about to take real traffic, and the readiness review found two gaps.
On-call is paged by a p99 per payment path, so a single slow request pages at night while a
slow week never does. And the network plan was drawn by hand: nothing stops a database rule
open to the internet, and nobody knows who pays for what. Close both gaps.

## The SLO (`slo.rules.yml`)

**99%** of payments requests are answered in under **300 ms** over 30 days, so the error
budget is 1% of the requests. The latency histogram is
`http_request_duration_seconds_bucket` (labels `job`, `path`, `le`) with its
`http_request_duration_seconds_count`. Only `job="payments"` counts. `path` holds payment IDs:
no rule may keep it or filter on it.

**Recording rules** (group `payments-slo-recordings`): the share of slow requests, that is
`1 -` the rate of the `le="0.3"` bucket divided by the rate of the `_count`, each
`sum by (job)`, over four windows.

| Record | Window |
| --- | --- |
| `job:slo_slow_requests:ratio_rate5m` | `5m` |
| `job:slo_slow_requests:ratio_rate30m` | `30m` |
| `job:slo_slow_requests:ratio_rate1h` | `1h` |
| `job:slo_slow_requests:ratio_rate6h` | `6h` |

**Alerts** (group `payments-slo-alerts`), each reading the recorded ratios. Remove the
draft's `PaymentsSlow`: a quantile is a cause-style signal, not the SLO.

| Alert | Fires when | `for` | `severity` | `summary` |
| --- | --- | --- | --- | --- |
| `PaymentsLatencyBudgetBurnFast` | the 1h **and** the 5m ratio are above `14.4 * 0.01` | `2m` | `page` | `payments is burning its latency budget fast` |
| `PaymentsLatencyBudgetBurnSlow` | the 6h **and** the 30m ratio are above `6 * 0.01` | `15m` | `ticket` | `payments is burning its latency budget` |

Both carry the label `slo: payments-latency` and the annotation
`runbook_url: https://runbooks.example.com/payments-latency`.

## The network plan (the Terraform root module in the kata folder)

The module creates nothing itself: it computes the plan the platform's AWS modules consume,
and its **validations are the guardrails** (policy as code) that reject a bad plan before
anyone reviews it. `versions.tf` and `prod.tfvars` are done; write the validations in
`variables.tf` and the outputs in `outputs.tf`.

| Variable | Rejected when |
| --- | --- |
| `environment` | not `dev`, `staging` or `prod` |
| `vpc_cidr` | not a `/16` inside `10.0.0.0/8`, or not written as its network address (`10.20.5.0/16` is rejected) |
| `zones` | not exactly three **different** zones |
| `tags` | `owner` or `cost-center` is missing or blank |
| `rules` (`from`, `to`, `port`) | a tier other than `internet`, `lb`, `app`, `db` (and `internet` only as `from`); anything but `lb` reachable from `internet`; `db` reachable from anything but `app` on `5432` |

| Output | Value |
| --- | --- |
| `public_subnets` | zone => the `/24` with netnum 0, 1, 2 (in the order of `zones`) |
| `private_subnets` | zone => the `/24` with netnum 10, 11, 12 |
| `nat_gateway_zones` | in `prod`, every zone (a zone outage cannot cut the others off); elsewhere only the first zone (one shared NAT costs a third) |
| `gateway_endpoints` | `["dynamodb", "s3"]`: free gateway endpoints, so that traffic never pays NAT processing |
| `ingress` | tier => its sorted `"from:port"` flows, for example `app = ["lb:8080"]` |
| `tags` | the caller's tags plus `environment` and `managed-by = "terraform"`, which the caller cannot override |

`cidrsubnet`, `merge`, `distinct`, `sort`, `lookup` and `alltrue` with a `for` expression are
all you need.

The dojo runs `promtool check rules` and hidden `promtool test rules` scenarios (a fast burn,
a slow burn, a short spike, a slow but tolerable week and the recorded ratios), `terraform fmt`,
`validate` and hidden `terraform test` runs offline, and reads both files with bats.

There are 34 tests: 2 in `tests/visible.bats`, 9 hidden bats tests, `promtool check`, 5
`promtool test` scenarios, `fmt`, `validate` and 15 `terraform test` runs.
