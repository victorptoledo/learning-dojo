---
from: java
status: draft
concept: "Making reads cheap"
from_way: "JPA entities with @OneToMany and fetch joins, building every page from normalised tables at request time."
native_way: "Precompute what reads need: inboxes, denormalised documents and read models fed by events, so a request does one lookup instead of a dozen joins."
sensei_tip: "A Spring @TransactionalEventListener(phase = AFTER_COMMIT) is the smallest projector: it updates a read model only after the write commits. For reliability across processes, use the outbox from the Backend track."
---

| Java / Spring | Read-heavy concept |
| --- | --- |
| `@Query` with fetch joins on every request | join at read time |
| a `@Document` holding the whole page | a denormalised read model |
| `@TransactionalEventListener` | a projector after commit |
| `Slice<T>` with a keyset `WHERE` | cursor pagination |
| `LongAdder` | a striped (sharded) counter in memory |
