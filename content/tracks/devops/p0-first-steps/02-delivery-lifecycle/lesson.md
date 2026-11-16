# The Delivery Lifecycle and DORA Metrics

Every change, from a one-line fix to a new service, travels the same path: someone writes
it, it is reviewed and integrated, built into an artefact, tested, released to an
environment, and finally runs for users who generate signals about it. DevOps is mostly about
making that path short, automatic and safe. This lesson names the stages and the four
numbers that tell you how well your path works.

## Integration, delivery, deployment

Three terms that are often mixed up:

| Practice | What is automatic | What stays manual |
| --- | --- | --- |
| **Continuous integration (CI)** | every change is merged to the main branch at least daily, built and tested automatically | the release |
| **Continuous delivery (CD)** | every change that passes the pipeline *could* go to production: it is packaged, deployed to test environments, verified | pressing the button for production |
| **Continuous deployment** | every change that passes the pipeline *does* go to production | nothing |

CI is about **integration**: long-lived branches that merge rarely are where bugs and
conflicts hide. Continuous delivery is about being **always releasable**. Continuous
deployment is a choice on top of that, common for web services and rare where releases need
an external approval.

## Small batches

Lean manufacturing learned it first, software learned it again: smaller batches flow faster
and fail softer.

- A small change is **easy to review**: reviewers find bugs in 50 lines, rubber-stamp 2,000.
- When it breaks, the cause is **obvious** and the rollback is **cheap**.
- It reaches users **sooner**, so feedback arrives while the author still remembers the code.
- Fewer changes wait in queues (waiting for review, for a release train, for a freeze), and
  waiting is most of the lead time in slow organisations.

Big releases feel safer because they happen rarely, but each one carries more risk, so teams
release even less often: a downward spiral. The way out is to release so often that releasing
becomes boring.

## The four DORA metrics

The DevOps Research and Assessment programme (DORA, now at Google) surveyed tens of thousands
of teams and found four metrics that predict delivery performance, and, importantly, that
**speed and stability go together**: the best teams are both faster and more stable.

| Metric | Question | Throughput or stability |
| --- | --- | --- |
| **Deployment frequency** | how often do we deploy to production? | throughput |
| **Lead time for changes** | how long from a commit to it running in production? | throughput |
| **Change failure rate** | what share of deployments cause a failure that needs a fix (rollback, hotfix, patch)? | stability |
| **Time to restore service** (failed deployment recovery time) | how long does it take to recover when a change fails? | stability |

Rough bands from the State of DevOps reports: elite teams deploy on demand (several times a
day), with lead times under a day, change failure rates around 5% or less and recovery in
under an hour; low performers deploy monthly or less, with lead times of months. Newer
reports add a fifth signal, **reliability** (meeting your availability and performance
targets), and **rework rate** (unplanned deployments to fix problems).

### Computing them

From a month of data:

```text
deployments:      12      failed deployments:  2 (restored in 40 min and 3 h)
merged commits → production: median 20 h
─────────────────────────────────────────────────────────────────────────────
deployment frequency  3 per week
lead time             20 hours (median)
change failure rate   2 / 12 = 17%
time to restore       median of 40 min and 3 h ≈ 1 h 50 min
```

Use medians for times: one stuck change should not hide that most flow quickly.

### Using them well

- Measure **teams and systems**, never individuals. Ranking people by metrics destroys the
  trust that blameless learning needs.
- Look at the four **together**: deploying more often while the failure rate climbs is not
  progress.
- Goodhart's law: when a measure becomes a target, it stops being a good measure. Splitting a
  deploy into ten tiny ones to raise the frequency teaches you nothing.
- They describe outcomes; to improve them you change capabilities: trunk-based development,
  test automation, loosely coupled architecture, deployment automation, monitoring.

## Value stream mapping

To find where lead time goes, draw the **value stream**: every step from "idea accepted" to
"running in production", with the time each takes to work on and the time it waits. In most
organisations the waits (for review, for a test environment, for a change advisory board)
dwarf the work. Automation removes work; flow improvements remove waits.

## Summary

- CI integrates and tests every change; continuous delivery keeps main releasable; continuous
  deployment releases every passing change.
- Small batches are faster to review, ship and fix, and less risky.
- DORA's four metrics: deployment frequency, lead time, change failure rate, time to restore;
  speed and stability improve together.
- Measure teams, read the metrics together, and improve capabilities rather than the numbers.

## Further reading

- *Accelerate*, Nicole Forsgren, Jez Humble and Gene Kim (IT Revolution, 2018)
- DORA's four keys: <https://dora.dev/guides/dora-metrics-four-keys/>
- *Continuous Delivery*, Jez Humble and David Farley (Addison-Wesley, 2010)
- Martin Fowler, "Continuous Integration": <https://martinfowler.com/articles/continuousIntegration.html>
- *Value Stream Mapping*, Karen Martin and Mike Osterling (McGraw-Hill, 2013)
