---
from: java
status: draft
concept: "Capturing database changes"
from_way: "JPA entity listeners or application code that publishes an event after saving."
native_way: "Read changes from the database log with Debezium, or publish domain events through an outbox table."
sensei_tip: "Debezium is written in Java and can run embedded in your application (the Debezium Engine) or as Kafka Connect connectors; Spring Modulith's event publication registry is an outbox."
---

| Java | Change data capture |
| --- | --- |
| @PostUpdate listener + publish | a dual write: avoid |
| outbox table in the same @Transactional | the outbox pattern |
| Debezium Engine | embedded log-based CDC |
| Kafka Connect + Debezium | CDC as a service |
| JDBC polling every minute | query-based CDC |
