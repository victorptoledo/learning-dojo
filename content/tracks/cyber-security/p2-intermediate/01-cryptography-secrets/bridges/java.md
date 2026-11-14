---
from: java
status: draft
concept: "Protecting data in transit and at rest"
from_way: "Storing passwords with MD5 or plain SHA-256 instead of a salted, slow key-derivation function."
native_way: "Argon2id, winner of the Password Hashing Competition, resists brute force on GPUs and ASICs."
sensei_tip: "Never commit secrets to Git: use pre-commit scanners (`gitleaks`) and keep secrets in a manager such as AWS Secrets Manager or HashiCorp Vault."
---
