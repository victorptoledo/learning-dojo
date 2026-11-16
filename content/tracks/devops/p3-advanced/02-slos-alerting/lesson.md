# SLIs, SLOs, Error Budgets and Burn-Rate Alerting

"Is the service reliable enough?" has no answer until you say *for whom* and *how reliable*.
Site reliability engineering answers with three terms and one idea: reliability is a feature
with a target, and the gap between the target and 100% is a **budget** you can spend.

## SLI, SLO, SLA

- An **SLI** (service level indicator) measures one aspect of the service as users experience
  it, as a ratio: **good events / valid events**.
- An **SLO** (objective) is the target for an SLI over a window: "99.9% of checkout requests
  succeed, over 30 days".
- An **SLA** (agreement) is a contract with consequences (refunds) and is looser than the SLO,
  so you notice trouble before it costs money.

| SLI kind | Good event | Measured at |
| --- | --- | --- |
| availability | a request that did not fail with a server error | the load balancer or the service |
| latency | a request served under 300 ms | a histogram bucket (`le="0.3"`) |
| freshness | data updated within 10 minutes | a pipeline's last success timestamp |
| correctness | a payment that reconciles | a business check |

Measure as close to the user as you can (the load balancer, a synthetic probe, real user
monitoring); a service that is "up" while the load balancer returns errors is down.

## Choosing targets

Higher is not better: each extra nine costs a lot, and users cannot tell 99.99% from 99.999%
behind their mobile network. Start from what users need and what the system achieves today,
and remember dependencies: a service cannot be more available than what it requires in series.

| SLO | Allowed bad time over 30 days |
| --- | --- |
| 99% | 7 h 12 min |
| 99.9% | 43 min |
| 99.95% | 21 min 36 s |
| 99.99% | 4 min 19 s |

Use a rolling window (28 or 30 days) so the budget recovers gradually.

## Error budgets

The **error budget** is `1 - SLO`: with 99.9% and 10 million requests a month, 10,000 may fail.
It turns arguments into arithmetic:

- budget left → ship features, run experiments, take planned risks;
- budget spent → an **error budget policy**, agreed in advance with the product owner, applies:
  freeze risky releases, put the team on reliability work, require a postmortem for large
  spends.

The budget also tells you when reliability work is *not* needed: being far above the SLO for
months means you could move faster.

## Alert on symptoms, by burn rate

Page a human only when **users are affected or about to be**, and when a human can act.
Causes (high CPU, a restarted Pod, a full queue) belong on dashboards or in tickets; many of
them never hurt anyone.

The **burn rate** is how fast you spend the budget: the error ratio divided by `1 - SLO`. A
burn rate of 1 spends exactly the budget in the window; 14.4 spends 2% of a 30-day budget in
one hour.

A single threshold is either too slow or too noisy. The SRE Workbook's **multiwindow,
multi-burn-rate** alerts combine a long window (is it significant?) with a short one (is it
still happening?):

| Severity | Burn rate | Long window | Short window | Budget spent when it fires |
| --- | --- | --- | --- | --- |
| page | 14.4 | 1 h | 5 min | 2% |
| page | 6 | 6 h | 30 min | 5% |
| ticket | 1 | 3 days | 6 h | 10% |

```yaml
- alert: CheckoutErrorBudgetBurnFast
  expr: |
    job:slo_errors_per_request:ratio_rate1h{job="checkout"} > (14.4 * 0.001)
      and
    job:slo_errors_per_request:ratio_rate5m{job="checkout"} > (14.4 * 0.001)
  for: 2m
  labels: { severity: page }
  annotations:
    summary: checkout is burning its error budget fast
    runbook_url: https://runbooks.example.com/checkout-slo
```

The ratios come from **recording rules**, so the alerts are cheap. **Alertmanager** groups,
deduplicates and routes alerts (page through PagerDuty or Opsgenie, tickets to a queue,
information to chat), and silences them during maintenance. Tools such as Sloth and Pyrra
generate these rules from an SLO definition.

## Humane on-call

- Every page has a **runbook**: what it means, how to check, how to mitigate, who to escalate to.
- Review alerts regularly: delete or demote any page that was not actionable.
- Track pages per shift; Google's SRE guidance aims for at most two incidents per 12-hour shift.
- Reduce **toil** (manual, repetitive, automatable work) with automation, and cap it (SRE
  teams aim to keep it under half of their time).

## Summary

- SLIs are user-facing ratios of good over valid events; SLOs set targets over a window; SLAs
  are looser contracts.
- The error budget (1 - SLO) is spent on change; a policy says what happens when it runs out.
- Alert on symptoms with burn rates over a long and a short window; send causes to dashboards.
- Every page needs a runbook and must be actionable; measure and reduce toil.

## Further reading

- *Site Reliability Engineering*, chapter 4 "Service Level Objectives": <https://sre.google/sre-book/service-level-objectives/>
- *The Site Reliability Workbook*, "Alerting on SLOs": <https://sre.google/workbook/alerting-on-slos/>
- Prometheus alerting rules: <https://prometheus.io/docs/prometheus/latest/configuration/alerting_rules/>
- Alertmanager: <https://prometheus.io/docs/alerting/latest/alertmanager/>
- *Implementing Service Level Objectives*, Alex Hidalgo (O'Reilly, 2020)
