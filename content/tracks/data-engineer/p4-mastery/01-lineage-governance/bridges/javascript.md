---
from: javascript
status: draft
concept: "Knowing where data comes from and who may see it"
from_way: "Tracking events sent with whatever properties seemed useful, including e-mails."
native_way: "Minimised, classified events, lineage from source to dashboard, and governed access."
sensei_tip: "Most personal data in analytics enters through tracking calls: send ids, not e-mails or names, and let the warehouse join to protected dimensions."
---

| JavaScript / TypeScript | Lineage and governance |
| --- | --- |
| analytics.track with an email | PII leaking into events: avoid |
| a typed tracking plan | a contract with classification |
| RBAC middleware | access control on the app |
| a dependency graph (madge) | the idea of lineage |
| consent flags | purpose limitation |
