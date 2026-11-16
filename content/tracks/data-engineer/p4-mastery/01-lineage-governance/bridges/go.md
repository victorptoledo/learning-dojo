---
from: go
status: draft
concept: "Knowing where data comes from and who may see it"
from_way: "Services with database credentials that can read everything."
native_way: "Per-pipeline service accounts with least privilege, lineage events, and classified, masked columns."
sensei_tip: "Give each pipeline its own credentials with only the grants it needs, the same way you would scope an API token: a leaked key then reaches one dataset, not the warehouse."
---

| Go | Lineage and governance |
| --- | --- |
| one DB user per service | least privilege per pipeline |
| OpenTelemetry traces | the idea behind run-level lineage |
| a `pii:"true"` struct tag | classification in code |
| crypto/aes with per-user keys | crypto-shredding |
| audit logs | who queried what |
