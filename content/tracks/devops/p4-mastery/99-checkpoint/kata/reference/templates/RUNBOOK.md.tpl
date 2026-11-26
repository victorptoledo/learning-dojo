# {{name}} runbook

- Owner: team {{team}}
- Tier: {{tier}}
- SLO: {{slo}}% of requests succeed over 30 days

## Severity

- SEV1: ErrorBudgetBurnFast pages, or users cannot use {{name}}. Open an incident now.
- SEV2: ErrorBudgetBurnSlow opens a ticket. Fix it in working hours.

## Roles

- Incident commander: coordinates and decides, and does not type the commands.
- Operations: the only person who changes production.
- Communications: posts a status update every 30 minutes until the incident is closed.

## First five minutes

1. Acknowledge the page and open an incident channel.
2. Mitigate first: roll back the most recent change (deploy, config or feature flag).
3. Only then diagnose: dashboards, then traces, then logs.

## Restore from backup

- RPO: {{rpo}}
- RTO: {{rto}}
- Restore into a new database from the cross-region copy, check it, then switch traffic. Never restore over the damaged one.

## After the incident

- Write a blameless postmortem within five working days, with owned action items.
