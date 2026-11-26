# Checkpoint 3: Advanced

This checkpoint closes the **Advanced** phase. It has two parts, and you need both:

1. **The quiz** (press **Q**): eight questions over the whole phase. You need 7 right.
2. **The integration kata** (press **Enter**): replace a payments API's p99 alert with a
   latency SLO and multiwindow burn-rate alerts, and write the guardrails and outputs of its
   network plan in Terraform: subnets per zone, NAT gateways that trade cost for resilience,
   flows the database cannot escape, and the tags that tell who pays.

Do them in any order. Before you start, review:

| Lesson | Ask yourself |
| --- | --- |
| Observability | Metric, log or trace for this question? Why a histogram and not an average? Which labels explode cardinality? |
| SLIs, SLOs and error budgets | What is the good event and the valid event? How big is the budget? What does a burn rate of 14.4 mean? |
| SLO alerts (kata) | Why two windows per alert? Which one makes the alert fire, which one makes it stop? |
| DevSecOps | Where does each check sit in the pipeline? What is policy as code, and when does it run? |
| Secure the pipeline (kata) | Who controls this value? What can this step do with its token? |
| Cloud networking and cost | What lives in a public subnet? Where does NAT money go? What does a gateway endpoint save? |

> [!TIP]
> If a quiz question surprises you, open the lesson it comes from again before retrying. The
> explanation after each answer says which idea it tests.
