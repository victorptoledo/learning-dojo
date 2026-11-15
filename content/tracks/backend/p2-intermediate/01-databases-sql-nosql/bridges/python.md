---
from: python
status: draft
concept: "Persistence model"
from_way: "ORM calls such as obj.save(), and transactions with transaction.atomic or a session."
native_way: "Isolation levels, locks and atomic updates that prevent lost updates, plus knowing when a document store fits."
sensei_tip: "Django's F('stock') - 1 is an atomic UPDATE; obj.stock -= 1; obj.save() is a read-modify-write race. select_for_update() takes the row lock."
---

| Python | In practice |
| --- | --- |
| obj.save() after reading | a lost-update race |
| F('seats') - 1 | UPDATE ... SET seats = seats - 1 WHERE seats > 0 |
| select_for_update() | SELECT ... FOR UPDATE |
| version field check | optimistic locking |
| pymongo | a document store with its own consistency rules |
