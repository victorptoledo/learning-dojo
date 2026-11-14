---
from: java
status: draft
concept: "The dual-write problem"
from_way: "Saving to the database and then publishing to the broker in the same request; if the broker fails, the two drift apart."
native_way: "Transactional outbox: save the entity and the event in the same ACID transaction; a background process reads the outbox and publishes with retries."
sensei_tip: "CQRS doesn't require separate databases: the same Postgres can hold normal tables for writes and views or denormalised tables for fast reads."
---
