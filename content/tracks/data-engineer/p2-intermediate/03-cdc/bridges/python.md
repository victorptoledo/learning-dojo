---
from: python
status: draft
concept: "Capturing database changes"
from_way: "A script that polls a table for rows with a newer updated_at."
native_way: "Consume Debezium change events from Kafka and apply them by key and log position."
sensei_tip: "psycopg can read a logical replication stream directly (ReplicationCursor), but in production let Debezium do it and consume its topics with a Kafka client such as confluent-kafka."
---

| Python | Change data capture |
| --- | --- |
| polling SELECT ... WHERE updated_at > | query-based CDC |
| psycopg2 LogicalReplicationConnection | reading the WAL directly |
| confluent_kafka.Consumer | reading Debezium topics |
| event['op'] in ('c', 'u', 'd', 'r') | change types |
| upsert or DELETE by key | applying a change |
