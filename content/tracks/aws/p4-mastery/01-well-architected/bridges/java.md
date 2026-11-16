---
from: java
status: draft
concept: "Ask the six pillars' questions on purpose, and write down the trade-offs"
from_way: "Architecture reviews by a central board checking diagrams against standards."
native_way: "Well-Architected reviews per workload, pillar by pillar, producing an owned improvement plan."
sensei_tip: "Keep architecture decision records next to the code and link them from the review; a Spring Boot service's choices (virtual threads, SnapStart, Graviton) map to concrete pillar questions."
---

| Java world | AWS |
| --- | --- |
| an architecture review board | a Well-Architected review per workload |
| non-functional requirements | the six pillars' best practices |
| ADRs in a wiki | ADRs plus the Tool's improvement plan |
| a quality gate | high risk issues tracked to done |
