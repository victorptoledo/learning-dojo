---
from: go
status: draft
concept: "Capturing database changes"
from_way: "Publishing to a broker right after db.Exec succeeds."
native_way: "Read the replication stream (or Debezium topics) and apply changes by key and LSN."
sensei_tip: "pglogrepl lets a Go program read PostgreSQL logical replication directly; remember to send standby status updates so the slot can release WAL."
---

| Go | Change data capture |
| --- | --- |
| db.Exec then producer.Produce | a dual write |
| pglogrepl | reading the WAL directly |
| pglogrepl.SendStandbyStatusUpdate | confirming the LSN |
| an outbox row in the same sql.Tx | the outbox pattern |
| segmentio/kafka-go reader | consuming change events |
