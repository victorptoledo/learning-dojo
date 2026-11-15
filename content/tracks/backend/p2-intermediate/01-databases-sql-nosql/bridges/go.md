---
from: go
status: draft
concept: "Persistence model"
from_way: "Explicit SQL in transactions with database/sql."
native_way: "The right statement and isolation level for each write."
sensei_tip: "Writing SQL by hand makes UPDATE ... WHERE seats > 0 RETURNING natural; check RowsAffected to know whether your booking won."
---

| Go | In practice |
| --- | --- |
| tx.Exec(UPDATE ... WHERE seats > 0) | atomic conditional update |
| RowsAffected() == 0 | someone else took it |
| SELECT ... FOR UPDATE | pessimistic lock |
| sql.TxOptions{Isolation: ...} | isolation level |
