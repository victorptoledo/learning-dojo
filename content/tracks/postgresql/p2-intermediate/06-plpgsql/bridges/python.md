---
from: python
status: reviewed
concept: "Code that runs next to the data"
from_way: "Model methods, Django signals (pre_save, post_save) and libraries such as django-simple-history."
native_way: "PL/pgSQL functions and procedures, and triggers that fire for every client and every statement."
sensei_tip: "Django signals do not fire for queryset.update() or bulk_create(); a trigger does. If an audit trail must be complete, it belongs in the database."
---

| Python | PostgreSQL |
| --- | --- |
| pre_save signal | BEFORE INSERT OR UPDATE row trigger |
| post_save signal writing a history row | AFTER trigger with to_jsonb(NEW) |
| raise ValueError(...) | RAISE EXCEPTION ... USING ERRCODE |
| try / except IntegrityError | BEGIN ... EXCEPTION WHEN unique_violation |
| a management command looping in batches | CREATE PROCEDURE ... COMMIT; CALL it |
| functools.cache on a pure function | IMMUTABLE |
