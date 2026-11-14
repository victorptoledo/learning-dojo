# SRE: SLIs, SLOs and Incidents

"Is the service healthy?" needs a number, not a feeling. Site Reliability Engineering (SRE) gives
the backend team a way to define reliability from the user's point of view, decide how much is
enough, alert only when users are hurting, and get better after every incident.

## SLIs, SLOs and SLAs

- An **SLI** (service level indicator) measures something users feel, as a ratio of good events to
  all events:
  - **availability**: successful requests / all requests (5xx are failures; 4xx usually are not);
  - **latency**: requests faster than 300 ms / all requests;
  - **freshness**, **correctness**, **durability** for pipelines and storage.
- An **SLO** (objective) is the target for an SLI over a window: "99.9% of checkout requests succeed
  over 30 days", "99% of searches answer within 300 ms".
- An **SLA** (agreement) is a contract with a penalty, set looser than the SLO.

Measure SLIs where users are: at the load balancer or the API gateway, not only inside the app.

## Error budgets

An SLO of 99.9% allows **0.1% failures**: in 30 days, about **43 minutes** of total downtime, or the
equivalent in failed requests. That allowance is the **error budget**.

| SLO | Allowed downtime per 30 days |
| --- | --- |
| 99% | 7.2 hours |
| 99.9% | 43 minutes |
| 99.99% | 4.3 minutes |

The budget turns reliability into a decision rule: while budget remains, ship features and take
risks; when it is spent, slow down and invest in reliability. And 100% is the wrong target: every
nine costs much more than the last, and users cannot tell the difference beyond what their own
network gives them.

## What to watch: the golden signals

For every service, four signals cover most problems:

1. **Latency**: how long requests take, as percentiles (p50, p95, p99), not averages. Track
   failed requests' latency separately.
2. **Traffic**: requests per second.
3. **Errors**: the rate of failed requests.
4. **Saturation**: how full the scarce resource is: CPU, memory, connection pool, queue depth, disk.

For resources, **USE** (utilisation, saturation, errors); for request-driven services, **RED** (rate,
errors, duration).

## Alerting

- **Alert on symptoms, not causes**: "checkout errors are burning the budget" pages someone; "CPU at
  80%" alone does not, because it may not hurt anyone.
- **Burn-rate alerts**: page when the error budget is being consumed fast enough to run out soon
  (for example 2% of the monthly budget in one hour), and open a ticket for slow burns.
- Every page must be **actionable** and link to a runbook. An alert nobody acts on is noise, and
  noisy alerts get ignored, including the real ones.
- Dashboards are for investigating; alerts are for waking people.

## Incidents

When users are affected:

1. **Declare** it early, in a known channel, with a severity.
2. **Assign roles**: an incident commander who coordinates, people who investigate, someone who
   communicates with stakeholders.
3. **Mitigate first, debug later**: roll back the deploy, turn off the feature flag, fail over,
   add capacity. Understanding the root cause can wait until users are fine.
4. **Communicate** regularly: what is affected, what is being done, when the next update comes.
5. **Resolve**, then follow up.

## Blameless postmortems

After an incident, write down what happened, how it was detected, how it was fixed, the timeline, the
impact, and **what will change** (action items with owners). Blameless means looking for the
conditions that made the mistake easy (no tests for that path, a confusing dashboard, a risky manual
step) instead of the person who made it: people who fear blame hide information, and the system does
not improve.

## Practices that prevent incidents

- Deploy small changes often, with fast rollback.
- Load-test and set capacity before the peak, not during it.
- Run **game days**: break things on purpose (kill an instance, slow a dependency) and check that
  alerts fire and the system degrades gracefully.
- Track **toil** (manual, repetitive operational work) and automate it away.

## Summary

- SLIs measure what users feel as good/total ratios; SLOs set targets; SLAs are contracts.
- The error budget (1 − SLO) decides between shipping and stabilising; 100% is the wrong target.
- Watch latency percentiles, traffic, errors and saturation.
- Alert on symptoms with burn rates; every page actionable, with a runbook.
- In incidents mitigate first, communicate, then run a blameless postmortem with owned actions.
