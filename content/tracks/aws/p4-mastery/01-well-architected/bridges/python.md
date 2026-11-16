---
from: python
status: draft
concept: "Ask the six pillars' questions on purpose, and write down the trade-offs"
from_way: "\"It works on my machine\" is the main architectural criterion."
native_way: "Each workload is reviewed against six pillars; risks become backlog items with measures."
sensei_tip: "Use the Serverless Applications Lens for Lambda-heavy Python services; many of its questions (idempotency, timeouts, DLQs) are things you implemented in this track."
---

| Python world | AWS |
| --- | --- |
| a code review checklist | the pillar review questions |
| PEP 20, the Zen of Python | the design principles |
| tech debt tickets | the improvement plan |
| pytest coverage | measures such as SLOs and restore tests |
