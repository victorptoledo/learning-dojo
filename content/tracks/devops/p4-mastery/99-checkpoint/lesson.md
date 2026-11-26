# Checkpoint 4: Final Exam

This checkpoint closes the **Mastery** phase and the whole track. It has two parts, and you
need both:

1. **The quiz** (press **Q**): eight questions over the whole track, weighted towards incident
   response, platform engineering and disaster recovery. You need 7 right.
2. **The integration kata** (press **Enter**): build the **golden path** of a platform team, a
   script that creates a new service with a hardened CI/CD workflow, SLO burn-rate alerts
   scaled to its tier, a backup policy that meets its RPO and RTO, a runbook that mitigates
   before it diagnoses, and a catalog entry. The tests check what your script generates with
   the tools of every phase: actionlint, promtool and Terraform.

Do them in any order. Before you start, review:

| Lesson | Ask yourself |
| --- | --- |
| CI/CD pipelines and Checkpoint 1 | Which token does each job need? Why pin to a SHA? Which runs may be cancelled? |
| Continuous delivery and GitOps, Checkpoint 2 | Why OIDC instead of stored keys? Who approves a production deploy? |
| SLOs and burn-rate alerting, Checkpoint 3 | How does the budget change the thresholds? Why page on one alert and ticket on the other? |
| Incident response | Who is the incident commander, and what do they not do? Why mitigate before you diagnose? |
| Platform engineering | What makes a golden path attractive rather than forced? What must it never do to a team's files? |
| Disaster recovery | What do RPO and RTO each limit? Why must backups be immutable and in another region? |

> [!TIP]
> If a quiz question surprises you, open the lesson it comes from again before retrying. The
> explanation after each answer says which idea it tests.
