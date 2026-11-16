# The Well-Architected Framework

The **AWS Well-Architected Framework** is AWS's distilled advice for building and running
workloads in the cloud: six **pillars**, each with design principles, best practices and
review questions. It does not tell you what to build; it gives you the questions to ask so
you notice the risks you are taking, and take them on purpose.

## The six pillars

| Pillar | The question | Design principles (a selection) |
| --- | --- | --- |
| **Operational excellence** | Can we run, change and improve it safely? | perform operations as code; make frequent, small, reversible changes; anticipate failure; learn from every operational event |
| **Security** | Are data and systems protected? | a strong identity foundation (least privilege, no long-lived keys); traceability; security at every layer; protect data in transit and at rest; keep people away from data; prepare for incidents |
| **Reliability** | Does it recover from failure and meet demand? | automatically recover from failure; test recovery procedures; scale horizontally; stop guessing capacity; manage change through automation |
| **Performance efficiency** | Does it use resources efficiently as demand changes? | use managed and serverless services; go global in minutes; experiment more often; consider mechanical sympathy |
| **Cost optimization** | Does it deliver value at the lowest price? | practise cloud financial management; adopt a consumption model; measure overall efficiency; stop spending on undifferentiated heavy lifting; analyse and attribute expenditure |
| **Sustainability** | Does it minimise environmental impact? | understand your impact; maximise utilisation; use managed services and efficient hardware (Graviton); reduce the downstream impact of your workload |

Every lesson in this track maps onto them: IAM and KMS to security, multi-AZ and DR to
reliability, CloudWatch and IaC to operational excellence, right-sizing and Savings Plans
to cost and sustainability.

## Trade-offs

The pillars pull against each other, and architecture is choosing deliberately:

| You gain | You pay |
| --- | --- |
| reliability with multi-region active-active | cost, complexity, data consistency work |
| performance with provisioned concurrency or bigger instances | cost |
| cost with Spot and smaller fleets | reliability headroom, interruption handling |
| security with private-only networking and customer managed keys | operational effort, some cost |
| speed of change with many small services | operational and observability overhead |

Security is the one pillar teams should not trade down. For the rest, write the trade-off
down (an architecture decision record) so the next team knows it was a choice.

## The review

A **Well-Architected review** is a conversation about one workload, not an audit:

1. Choose the workload and its business context (what matters: RTO, RPO, peak, budget,
   compliance).
2. Answer the pillar questions in the **AWS Well-Architected Tool** (in the console): for
   example *REL 9: How do you back up data?* or *SEC 3: How do you manage permissions for
   people and machines?* Each answer selects the best practices you follow.
3. The Tool marks **high risk issues** (HRIs) and medium risks where best practices are
   missing.
4. Build an **improvement plan**: prioritise by business risk and effort, assign owners,
   and review again in a few months. Milestones record progress.

**Lenses** add questions for specific domains: Serverless Applications, SaaS, Financial
Services, Container Build, Data Analytics, Machine Learning, Generative AI. You can write
**custom lenses** for your organization's own standards. AWS Trusted Advisor checks and
Security Hub findings feed real evidence into the answers.

## From findings to work

Findings matter only when they turn into backlog items with a measurable outcome:

| Finding | Improvement | Measure |
| --- | --- | --- |
| no alarm when the DLQ fills | DLQ depth alarm to on-call | alarm fires in a game day |
| restores never tested | quarterly timed restore of the table | restore time below the RTO |
| on-demand everywhere | Compute Savings Plan for the baseline | cost per order −20 % |
| one region, no plan | pilot light in a second region | DR exercise within RTO/RPO |

## Summary

- Six pillars: operational excellence, security, reliability, performance efficiency, cost optimization, sustainability.
- Each pillar has design principles and review questions; lenses extend them per domain.
- Pillars trade against each other; choose deliberately and write the decision down.
- A review finds high and medium risks; an improvement plan with owners and measures fixes them.

## Further reading

- AWS Well-Architected Framework: <https://docs.aws.amazon.com/wellarchitected/latest/framework/welcome.html>
- The pillars of the framework: <https://docs.aws.amazon.com/wellarchitected/latest/framework/the-pillars-of-the-framework.html>
- AWS Well-Architected Tool user guide: <https://docs.aws.amazon.com/wellarchitected/latest/userguide/intro.html>
- Serverless Applications Lens: <https://docs.aws.amazon.com/wellarchitected/latest/serverless-applications-lens/welcome.html>
- Operational excellence pillar: <https://docs.aws.amazon.com/wellarchitected/latest/operational-excellence-pillar/welcome.html>
- Sustainability pillar: <https://docs.aws.amazon.com/wellarchitected/latest/sustainability-pillar/sustainability-pillar.html>
