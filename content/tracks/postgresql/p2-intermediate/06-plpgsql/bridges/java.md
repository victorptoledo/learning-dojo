---
from: java
status: reviewed
concept: "Code that runs next to the data"
from_way: "Service methods, JPA entity listeners (@PrePersist, @PreUpdate) and Hibernate Envers for auditing."
native_way: "PL/pgSQL functions and procedures, and triggers that fire for every client, including psql and other services."
sensei_tip: "@PreUpdate and Envers only see changes made through Hibernate. A trigger also catches the bulk UPDATE in a migration and the fix someone typed in psql."
---

| Java | PostgreSQL |
| --- | --- |
| @PreUpdate setting updatedAt | BEFORE UPDATE trigger setting NEW.updated_at |
| Hibernate Envers audit tables | an AFTER trigger writing to_jsonb(OLD) and to_jsonb(NEW) |
| throw new IllegalArgumentException(...) | RAISE EXCEPTION ... USING ERRCODE = '22023' |
| try { } catch (DuplicateKeyException e) | EXCEPTION WHEN unique_violation THEN |
| @Transactional batch with flush and clear | a PROCEDURE that COMMITs between batches |
