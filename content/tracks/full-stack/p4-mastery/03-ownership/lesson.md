# Production Ownership: Safe Migrations, Feature Flags and Incidents

"You build it, you run it." A senior full stack engineer is trusted with production: they change
it without breaking it, they release gradually, and when it breaks anyway they lead the response
calmly and make sure it does not happen the same way twice.

## Zero-downtime schema changes

During a deploy, old and new versions of the API run at the same time, browsers hold old
JavaScript bundles, and mobile apps lag for weeks. A destructive change (rename, drop, type
change, new `NOT NULL`) must therefore become a sequence of compatible steps: **expand and
contract** (also called parallel change).

Splitting `full_name` into `first_name` and `last_name`:

1. **Expand**: add nullable `first_name` and `last_name`. Old code ignores them.
2. **Dual write**: deploy code that writes both the old and the new columns.
3. **Backfill** existing rows **in batches** (thousands of rows per transaction, with pauses),
   never one huge `UPDATE` that locks the table and floods replication.
4. **Switch reads** to the new columns, behind a flag; the API keeps returning `fullName` for old
   clients, computed from the new columns.
5. **Stop writing** the old column once nothing reads it; **measure** (logs, API analytics) that
   no client still sends or reads it.
6. **Contract**: drop the old column in a later release.

PostgreSQL details that matter: `CREATE INDEX CONCURRENTLY` instead of a locking index build; add
constraints as `NOT VALID` and `VALIDATE` them later; set a `lock_timeout` on migrations so one
waiting `ALTER TABLE` does not queue all traffic behind it.

## Feature flags and progressive delivery

A **feature flag** separates deploying code from releasing a feature:

- **release flags** hide unfinished work and turn on a feature for 1 %, 10 %, 50 % of users while
  you watch the metrics;
- **ops flags** (kill switches) turn off an expensive or failing feature without a deploy;
- **experiment flags** split traffic for A/B tests;
- **permission flags** give features to some plans or tenants.

Use a flag service or library (OpenFeature is the vendor-neutral API; LaunchDarkly, Unleash,
Flagsmith, GrowthBook are common), evaluate flags on the server where possible so the decision is
consistent, and keep the flag in the UI and the API in agreement. Flags are **debt**: give each
one an owner and an expiry, and remove release flags once the rollout is done.

**Canary releases** do the same at the deploy level: a new version gets a small share of traffic,
and an automatic check compares its error rate and latency with the old one before going further.

## When it breaks: incidents

A good response is a practised routine:

1. **Detect**: an alert on a symptom (error rate, latency SLO) or a user report.
2. **Declare** an incident and assign roles: an **incident commander** who coordinates, someone
   who communicates (status page, support, stakeholders), and responders.
3. **Mitigate first, investigate later**: roll back the last deploy, turn off the flag, scale up,
   fail over. Restoring service beats finding the root cause.
4. **Communicate** on a schedule ("next update in 30 minutes"), even when there is nothing new.
5. **Resolve** and confirm with the same signals that alerted you.
6. **Postmortem**, **blameless**: a timeline, impact, contributing factors (plural: there is rarely
   one root cause), what went well, and **action items** with owners and dates, tracked like any
   other work.

The trace ids, structured logs and RED dashboards of phase 3 are what make step 3 fast.

## Habits of a trustworthy owner

- Small, frequent, reversible changes; trunk-based development with short-lived branches.
- Runbooks for known alerts; on-call that is shared fairly and reviewed.
- Error budgets: when the SLO is at risk, reliability work comes before new features.
- Read the dashboards after every deploy you make.

## Summary

- Make destructive schema changes with expand and contract, batched backfills and measured
  removal; use concurrent indexes and lock timeouts.
- Flags separate deploy from release; roll out gradually and retire flags.
- In incidents: declare, assign roles, mitigate first, communicate, then a blameless postmortem
  with owned actions.

## Further reading

- Martin Fowler, "ParallelChange": <https://martinfowler.com/bliki/ParallelChange.html>
- PostgreSQL documentation, "CREATE INDEX" (building indexes concurrently): <https://www.postgresql.org/docs/current/sql-createindex.html>
- OpenFeature documentation: <https://openfeature.dev/docs/reference/intro>
- Pete Hodgson, "Feature Toggles (aka Feature Flags)": <https://martinfowler.com/articles/feature-toggles.html>
- Google SRE book, "Managing Incidents" and "Postmortem Culture": <https://sre.google/sre-book/managing-incidents/>
