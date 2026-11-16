---
from: python
status: draft
concept: "Accounts are the isolation boundary; guardrails set the maximum"
from_way: "Separate Django settings files point dev and prod at different resources in one account."
native_way: "Separate accounts per environment, SCP guardrails, and cross-account roles assumed with STS."
sensei_tip: "Use `boto3.Session(profile_name=\"orders-prod\")` with SSO profiles locally; in code, call `sts.assume_role` once and build a session from its credentials, or let a profile with `role_arn` and `source_profile` do it."
---

| Python world | AWS |
| --- | --- |
| `settings_prod.py` | a production account |
| `sts.assume_role(...)` | cross-account access |
| a superuser that can do anything | a role capped by a permissions boundary |
| an `ALLOWED_HOSTS` list | an SCP region allow-list |
