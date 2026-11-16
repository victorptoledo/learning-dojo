---
from: go
status: draft
concept: "Ask what can go wrong before writing the code"
from_way: "Microservices designed for performance first, with internal calls trusted because they are inside the cluster."
native_way: "Trust boundaries drawn between services too (zero trust), with mutual TLS or signed tokens on internal flows and threats tracked per service."
sensei_tip: "Inside a cluster every service-to-service arrow crosses a boundary when a single compromised pod is part of your threat model; that is the argument for mTLS and per-service identities."
---

| Go | Threat modeling habit |
| --- | --- |
| internal network = trusted | zero trust between services |
| no identity on internal calls | mTLS or signed tokens |
| performance-only design review | STRIDE in the same review |
| panics on bad input | denial of service in the threat list |
| no abuse tests | table-driven tests for misuse |
