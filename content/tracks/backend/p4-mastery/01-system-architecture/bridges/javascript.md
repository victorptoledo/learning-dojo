---
from: javascript
status: draft
concept: "Architecture choices"
from_way: "Microservices from the start, or a NestJS monolith with modules."
native_way: "Bounded contexts, data ownership and deliberate communication styles."
sensei_tip: "A NestJS modular monolith with clear module boundaries is often the right first architecture; extract a module when its deployment or scaling needs differ."
---

| JavaScript / TypeScript | In practice |
| --- | --- |
| one service per entity | one service per capability |
| HTTP calls between everything | events where possible |
| distributed transaction | saga |
| shared models package | contracts, not shared tables |
