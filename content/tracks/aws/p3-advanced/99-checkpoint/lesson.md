# Checkpoint 3: Advanced

This checkpoint closes the **Advanced** phase. It has two parts, and you need both:

1. **The quiz** (press **Q**): eight questions over the whole phase. You need 7 right.
2. **The integration kata** (press **Enter**): turn a first draft of AWS Backup into backups
   that survive an attacker with admin rights, a bad deploy and a lost region: a key only
   AWS Backup can use, a locked vault nobody can empty, a role no other account can borrow,
   a plan that meets the RPO and copies to another account and region, and the policy of
   the disaster-recovery vault in the backup account.

Do them in any order. Before you start, review:

| Lesson | Ask yourself |
| --- | --- |
| Multi-account IAM | Which policies must allow a cross-account call? What does `aws:PrincipalOrgID` give a resource policy? Why is an explicit Deny the only thing an admin cannot override? |
| Organization guardrails (kata) | How does a Deny with an exception (`ArnNotLike aws:PrincipalArn`) keep a break-glass role? |
| Security services | What does a key policy decide that IAM cannot? What does `kms:ViaService` limit? Why a customer managed key for backups? |
| Policy as code (kata) | Which mistakes would a rule catch before the template ships? |
| High availability and DR | What are RTO and RPO? What does a vault lock protect against? Why copy to another account *and* region? |
| Cost | What does cold storage cost, and what minimum does it impose? |

> [!TIP]
> Think like the attacker first. With `AdministratorAccess` in the workload account, what
> could you delete in the draft? Every explicit Deny and every condition in the kata is an
> answer to one of those moves.
