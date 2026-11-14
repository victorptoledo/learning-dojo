# Delivery: From Commit to Production

The teams that deploy most often also have the fewest failed deploys and recover fastest, because
small, automated, reversible changes are safer than big, rare, manual ones. This lesson covers the
pipeline, deploying without downtime, changing the database safely, and releasing gradually.

## Measuring delivery

The **DORA metrics** describe delivery performance:

| Metric | Question |
| --- | --- |
| Deployment frequency | how often do we deploy to production? |
| Lead time for changes | how long from commit to production? |
| Change failure rate | what share of deploys cause a failure? |
| Time to restore | how long to recover when one does? |

Speed and stability go together: improve both by making changes smaller and the pipeline faster.

## The pipeline

```text
commit → build → unit tests → static checks → package image → integration tests
       → deploy to staging → smoke tests → deploy to production (gradually) → verify
```

- **Continuous integration**: everyone merges to the main branch at least daily; every commit is
  built and tested. Short-lived branches, no long-running feature branches.
- **Build once, promote the same artifact** (the same container image) through every environment.
- **Gates** that stop a bad change: failing tests, vulnerability scans, a failed smoke test.
- Keep the pipeline **fast** (minutes): slow pipelines lead to big batches.
- **Continuous delivery**: every change that passes is releasable; **continuous deployment**: it is
  released automatically.

## Deploying without downtime

- **Rolling**: replace instances a few at a time; old and new versions run side by side for a while.
- **Blue-green**: run the new version next to the old one, switch traffic at once, switch back to roll
  back.
- **Canary**: send a small share of traffic (1%, then 10%, then all) to the new version, compare its
  error rate and latency with the old one, and stop automatically if it is worse.

All of them mean **two versions run at the same time**, so every change must be compatible with the
version before it: APIs, events and the database.

## Database migrations: expand and contract

Renaming a column in one step breaks the old version that is still running. Do it in phases:

```text
1. expand:    add the new column (nullable); deploy code that writes both columns
2. migrate:   backfill the new column for existing rows (in batches)
3. switch:    deploy code that reads the new column (still writes both)
4. contract:  stop writing the old column; later, drop it
```

Rules of thumb: additive changes are safe; removals and renames take several releases; long
migrations run in batches outside the deploy; never lock a big table in a migration at peak time; and
make migrations repeatable (Flyway, Liquibase).

## Feature flags

A **feature flag** separates **deploying** code from **releasing** a feature:

- merge unfinished work behind a flag that is off (no long branches);
- release to employees, then 5% of users, then everyone;
- turn a misbehaving feature off in seconds, without a deploy;
- run experiments (A/B tests).

Flags are debt: name an owner and a removal date, and delete them once the feature is fully on.

## Rollback and roll forward

- Every deploy must be **reversible** quickly: keep the previous image, keep migrations backward
  compatible, and practise rolling back.
- Sometimes **rolling forward** (a quick fix) is better than rolling back, for example after a data
  migration. Decide in advance which changes can be rolled back.

## Configuration and environments

- The same image everywhere; configuration and secrets from the environment.
- **Infrastructure as code** (Terraform, Kubernetes manifests, Helm) reviewed and versioned like
  application code.
- Production-like staging, but do not rely on staging alone: verify in production with canaries,
  monitoring and smoke tests.

## Summary

- DORA metrics: deploy often, short lead time, low failure rate, fast recovery; small changes help all
  four.
- CI on a fast pipeline that builds one artifact and promotes it through gated stages.
- Deploy with rolling, blue-green or canary releases; two versions always overlap.
- Change schemas with expand-migrate-switch-contract.
- Separate deploy from release with feature flags, and remove them afterwards.
- Make every deploy reversible and practise it.
