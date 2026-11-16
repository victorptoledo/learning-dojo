---
from: python
status: reviewed
concept: "Readers that never block writers"
from_way: "Rebinding a name to a new immutable tuple while other code keeps a reference to the old one, and reference counting freeing it later."
native_way: "MVCC: every update writes a new row version, each transaction reads the versions its snapshot allows, and VACUUM collects the garbage."
sensei_tip: "Django and SQLAlchemy sessions that stay inside a transaction while a view calls an API are idle in transaction: they pin old row versions for the whole database."
---

| Python | PostgreSQL |
| --- | --- |
| new = old._replace(balance=80) | UPDATE writes a new tuple |
| a variable still pointing to the old tuple | a snapshot that sees the old version |
| the garbage collector | VACUUM |
| with conn.transaction(): and a slow HTTP call inside | idle in transaction, holding back cleanup |
| psycopg autocommit=True for read-only scripts | no long snapshot |
