---
from: java
status: draft
concept: "Encrypt, keep secrets out of code, record everything, detect threats"
from_way: "Keystores on disk, passwords in properties files and security reviews once a year."
native_way: "KMS keys, Secrets Manager, CloudTrail, Config, GuardDuty and Security Hub give continuous protection and evidence."
sensei_tip: "Use the AWS Encryption SDK for Java for client-side envelope encryption, and the Secrets Manager JDBC driver or caching client so the app picks up rotated passwords."
---

| Java world | AWS |
| --- | --- |
| a JKS keystore | a KMS customer managed key |
| `application.properties` password | Secrets Manager with rotation |
| audit logs in the app | CloudTrail for every AWS API call |
| OWASP dependency-check | Inspector scanning ECR images and Lambda |
