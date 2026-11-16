---
from: python
status: draft
concept: "Encrypt, keep secrets out of code, record everything, detect threats"
from_way: "`cryptography.Fernet` keys in an env var and secrets in `.env` files."
native_way: "KMS envelope encryption, Secrets Manager secrets read through the role, and a detection stack across accounts."
sensei_tip: "Use `aws_secretsmanager_caching` (or Powertools' parameters utility) to cache secrets in Lambda, so you don't call Secrets Manager on every invocation."
---

| Python world | AWS |
| --- | --- |
| `Fernet(key)` | KMS `GenerateDataKey` and envelope encryption |
| python-dotenv | Secrets Manager or Parameter Store |
| bandit in CI | cfn-guard and Config rules for infrastructure |
| fail2ban | GuardDuty plus WAF rate-based rules |
