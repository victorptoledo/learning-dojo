---
from: java
status: draft
concept: "Everything in AWS is an API you call with signed credentials"
from_way: "A Java team deploys a WAR to servers the ops team racked, patched and firewalled."
native_way: "You call AWS APIs (through the console, the CLI, CloudFormation or the AWS SDK for Java 2.x) and own everything above the service line."
sensei_tip: "Build clients once with the default credential provider chain (`S3Client.create()`); never pass keys to the builder, so the same jar works on a laptop with SSO and in AWS with a role."
---

| Java world | AWS |
| --- | --- |
| a datacentre and its ops team | AWS: facilities, hardware, hypervisor |
| `application.properties` with a DB password | IAM roles plus Secrets Manager |
| `DefaultCredentialsProvider` | the default credential provider chain |
| a Maven profile per environment | one AWS account (and region) per environment |
