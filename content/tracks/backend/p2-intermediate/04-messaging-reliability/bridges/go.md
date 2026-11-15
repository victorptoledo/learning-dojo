---
from: go
status: draft
concept: "Reliable events"
from_way: "Publishing with a broker client after the database write, handled by hand."
native_way: "Outbox, at-least-once delivery and idempotent handlers."
sensei_tip: "Write the event row and the business row in one sql.Tx; a separate goroutine or process relays outbox rows to the broker."
---

| Go | In practice |
| --- | --- |
| tx.Commit(); producer.Produce() | dual write |
| outbox rows in the same tx | outbox |
| INSERT processed id ON CONFLICT DO NOTHING | idempotent consumer |
| retry with backoff | then a DLQ |
