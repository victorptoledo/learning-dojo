---
from: python
status: draft
concept: "Concurrent changes that stay correct"
from_way: "threading.Lock in one process, and the ORM's session.commit or with conn.transaction()."
native_way: "Transactions and row locks shared by every process that uses the database."
sensei_tip: "A threading.Lock means nothing to another gunicorn worker. With psycopg, use 'with conn.transaction():' and an atomic UPDATE or SELECT ... FOR UPDATE."
---

| Python | SQL |
| --- | --- |
| with conn.transaction(): | BEGIN ... COMMIT |
| with lock: | SELECT ... FOR UPDATE |
| select_for_update(skip_locked=True) in Django | FOR UPDATE SKIP LOCKED |
| F('balance') - 80 in Django | SET balance = balance - 80 |
| except SerializationFailure: retry | retry the transaction on 40001 |
