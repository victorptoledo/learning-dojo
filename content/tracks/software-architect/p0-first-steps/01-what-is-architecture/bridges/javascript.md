---
from: javascript
status: draft
concept: "Software architecture"
from_way: "Architecture means the folder template of a framework (Express routes, a Next.js app directory) and the npm packages chosen."
native_way: "Architecture as the decisions that are expensive to change (module and package boundaries, data ownership, sync calls or events, what runs where), driven by quality attributes such as latency, scalability and modifiability."
sensei_tip: "Choosing between server rendering, a single-page app and edge functions is an architectural decision: it fixes performance, cost and operations for years. Write down why you chose it."
---

| JavaScript / TypeScript world | Architectural question behind it |
| --- | --- |
| one Node.js service or many | deployment units, availability, team independence |
| a monorepo with workspaces, `dependency-cruiser` rules | module structure and allowed dependencies |
| SSR, SPA, edge functions | where code runs: performance, cost, operations |
| queues and event buses (BullMQ, Kafka, SNS) | synchronous versus event-driven runtime structure |
| one database schema shared by every service | data ownership and coupling |
| k6 load tests | checking performance scenarios |
