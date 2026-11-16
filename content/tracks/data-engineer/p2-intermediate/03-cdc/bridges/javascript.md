---
from: javascript
status: draft
concept: "Capturing database changes"
from_way: "Calling a webhook or emitting an event after each successful write."
native_way: "Capture changes from the database log, or write events to an outbox in the same transaction."
sensei_tip: "Firestore and MongoDB change streams are CDC exposed as an API: the same rules apply (resume tokens are your log position, and events can be replayed)."
---

| JavaScript / TypeScript | Change data capture |
| --- | --- |
| emit after save | a dual write |
| MongoDB changeStream with resumeAfter | log-based CDC with a position |
| prisma.$transaction([order, outbox]) | the outbox pattern |
| kafkajs consumer | reading change events |
| op: 'd' with after: null | a delete event |
