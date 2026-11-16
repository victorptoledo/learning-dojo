---
from: go
status: draft
concept: "Everything in AWS is an API you call with signed credentials"
from_way: "A Go binary is copied with scp to servers you manage, with config files next to it."
native_way: "aws-sdk-go-v2 calls AWS APIs; `config.LoadDefaultConfig(ctx)` resolves region and credentials from the chain."
sensei_tip: "Load the config once at start-up and share clients; pass `config.WithRegion` explicitly, since a missing region is the most common first error."
---

| Go world | AWS |
| --- | --- |
| scp to a server | deploy to EC2, ECS or Lambda |
| flags with secrets | roles plus Secrets Manager or SSM Parameter Store |
| `config.LoadDefaultConfig(ctx)` | the default credential provider chain |
| one host | a region with three or more AZs |
