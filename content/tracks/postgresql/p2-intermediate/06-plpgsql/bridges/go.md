---
from: go
status: reviewed
concept: "Code that runs next to the data"
from_way: "Plain functions in the service and explicit audit writes in each repository method."
native_way: "PL/pgSQL functions and triggers, so the rule holds for every client, not only for this binary."
sensei_tip: "Errors raised with RAISE EXCEPTION ... USING ERRCODE reach pgx as *pgconn.PgError with that Code: map them to your domain errors with errors.As."
---

| Go | PostgreSQL |
| --- | --- |
| an audit write in every repository method | one AFTER trigger on the table |
| return fmt.Errorf("order %d not found", id) | RAISE EXCEPTION 'order % not found', id USING ERRCODE = 'P0002' |
| errors.As(err, &pgErr) | the ERRCODE you raised |
| a loop with batched DELETEs and commits | a PROCEDURE with COMMIT |
| a pure function | IMMUTABLE |
