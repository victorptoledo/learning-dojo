---
from: javascript
status: draft
concept: "Encrypt, keep secrets out of code, record everything, detect threats"
from_way: "Secrets in `.env` files and a helmet middleware as the security layer."
native_way: "Secrets Manager for secrets, KMS for keys, WAF at the edge, and CloudTrail, Config and GuardDuty behind it."
sensei_tip: "Read secrets once per Lambda environment with the Parameters and Secrets Lambda extension or Powertools for TypeScript, and never bundle them into the code package."
---

| Node.js world | AWS |
| --- | --- |
| dotenv | Secrets Manager |
| helmet and rate-limit middleware | AWS WAF on CloudFront or the ALB |
| npm audit | Inspector for Lambda and ECR |
| console.log audit trail | CloudTrail |
