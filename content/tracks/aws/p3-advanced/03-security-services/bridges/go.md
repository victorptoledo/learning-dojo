---
from: go
status: draft
concept: "Encrypt, keep secrets out of code, record everything, detect threats"
from_way: "Secrets in flags or env vars, and TLS certificates copied onto hosts."
native_way: "Secrets Manager and KMS through aws-sdk-go-v2, ACM certificates on load balancers, and the detection stack across accounts."
sensei_tip: "Use the `secretcache` package (aws-secretsmanager-caching-go) and keep the secret value only in memory; never log the struct that holds it."
---

| Go world | AWS |
| --- | --- |
| `os.Getenv("API_KEY")` | Secrets Manager |
| `crypto/aes` with a static key | KMS data keys (envelope encryption) |
| certificates on disk | ACM on the ALB or CloudFront |
| govulncheck | Inspector for images and functions |
