# The DevOps Role: Culture, Ownership and the Toolchain

For decades software was built by one group and run by another. Developers were rewarded for
shipping features, operations for keeping things stable, so every release was a fight: a big
batch of changes thrown "over the wall" with a runbook, deployed at night, and debugged by
people who had not written it. **DevOps** is the answer the industry converged on around
2009: the people who build a service also take part in running it, and the path from a
commit to production is automated, small-batched and measured.

## What DevOps is, and what it is not

DevOps is a **way of working**, usually summarised as CALMS:

- **Culture**: shared ownership of outcomes, blameless learning from failure;
- **Automation**: builds, tests, deployments and infrastructure are code that a machine runs;
- **Lean**: small batches, short queues, limited work in progress;
- **Measurement**: decisions based on data (delivery metrics, SLOs, cost);
- **Sharing**: knowledge, tools and on-call duty are shared across teams.

It is **not** a tool you buy, a job title alone, or a separate "DevOps team" that becomes the
new wall between developers and operations. Gene Kim's *Three Ways* say the same thing as a
sequence: make the flow from development to operations fast (**flow**), shorten and amplify
feedback from production back to developers (**feedback**), and build a culture of
experimentation and learning (**continual learning**).

## Who does the work

The job market uses several titles for overlapping work:

| Title | Focus | Typical week |
| --- | --- | --- |
| **DevOps engineer** | the delivery pipeline and the infrastructure under it | write CI/CD pipelines, Terraform and Kubernetes manifests, help teams ship |
| **Platform engineer** | an internal platform that product teams use as a product | golden paths, self-service environments, templates, a developer portal |
| **Site reliability engineer (SRE)** | reliability as an engineering problem, with SLOs and error budgets | on-call, incident response, capacity, automation that removes toil |
| **Cloud engineer** | a cloud provider's services, accounts, networks and cost | landing zones, IAM, networking, FinOps |

In a small company one person does all four. What they share is that they write code (in
YAML, HCL, bash, Python or Go) to make the path to production fast and safe, and they own
what happens when it breaks.

## The path from commit to production

This track follows a change from a laptop to users and back:

```text
 code ──► Git ──► CI ──────────► artefact ──► CD ───────► runtime ──────────► users
          PRs     build, test,    image in a   promote,    Linux, containers,
                  scan, lint      registry     release     Kubernetes, cloud
   ▲                                                                 │
   └──── metrics, logs, traces, alerts, incidents, postmortems ◄────┘
```

| Phase | Pillars | What you build |
| --- | --- | --- |
| First Steps | the role, delivery metrics | your first CI workflow |
| Fundamentals | Linux, containers, CI pipelines, configuration management | an idempotent host converger, a production CI pipeline |
| Intermediate | Kubernetes, continuous delivery and GitOps, Terraform, release strategies | a Terraform module, a canary release, a plan/apply pipeline |
| Advanced | observability, SLOs and alerting, DevSecOps, cloud networking and cost | burn-rate alerts, a hardened pipeline |
| Mastery | incident response, platform engineering, disaster recovery | written exercises on running production |

This is a **role** track: it gives you the minimum of each pillar and ties them together. For
depth, the dojo has full tracks on **Linux**, **Git & GitHub**, **Docker & Containers**,
**Kubernetes** and **AWS**; their lessons count towards the same skills.

## Skills that last longer than tools

Tools change every few years (Jenkins, then GitLab CI and GitHub Actions; Puppet, then
Ansible, then containers). What lasts:

- **Linux and networking**: every container, node and cloud VM is a Linux box behind a network;
- **reading other people's code and logs** calmly, under pressure;
- **automation as code**, reviewed and versioned like any other code;
- **thinking in systems**: queues, failure modes, feedback loops, blast radius;
- **writing**: runbooks, postmortems, design documents and clear pull requests.

## Summary

- DevOps joins building and running software: shared ownership, automation, small batches,
  measurement and sharing (CALMS; flow, feedback, learning).
- It is not a tool or a separate team that becomes a new wall.
- DevOps, platform, SRE and cloud engineers overlap; all write code to make delivery fast and
  safe and own production.
- The track follows a change from commit to users and the feedback back to developers.

## Further reading

- *The DevOps Handbook*, 2nd edition, Gene Kim, Jez Humble, Patrick Debois, John Willis and Nicole Forsgren (IT Revolution, 2021)
- Site Reliability Engineering (Google, free online): <https://sre.google/sre-book/table-of-contents/>
- DORA research programme: <https://dora.dev/>
- roadmap.sh DevOps roadmap: <https://roadmap.sh/devops>
- Martin Fowler, "DevOps Culture": <https://martinfowler.com/bliki/DevOpsCulture.html>
