---
from: javascript
status: draft
concept: "Integration patterns and sagas"
from_way: "`Promise.all` over three service calls, with no plan for the one that fails after the others succeeded."
native_way: "Queues and topics (SQS, SNS, Kafka) with dead letter queues, sagas orchestrated with Temporal's TypeScript SDK or AWS Step Functions, idempotency keys, and a BFF per client in Node.js."
sensei_tip: "`Promise.all` rejects on the first failure but does not undo the calls that already succeeded. In a distributed workflow that is a half-finished saga with no compensations."
---

| JavaScript / TypeScript | Integration idea |
| --- | --- |
| SQS, SNS, KafkaJS | channels and pub-sub |
| SQS redrive policy | dead letter channel |
| Temporal TypeScript SDK, AWS Step Functions | orchestrated saga |
| events between services | choreographed saga |
| GraphQL gateway or a Node.js BFF | API composition, backend for frontend |
| `Idempotency-Key` header | idempotent receiver |
