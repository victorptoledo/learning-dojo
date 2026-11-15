---
from: go
status: draft
concept: "Rules the data itself enforces"
from_way: "Validation in handlers or with go-playground/validator struct tags."
native_way: "Constraints in the schema, the only rules no code path can skip."
sensei_tip: "With pgx, errors.As(err, &pgErr) and check pgErr.Code == \"23505\" (pgerrcode.UniqueViolation) to turn a constraint into a clean API error."
---

| Go | SQL |
| --- | --- |
| validate:"required" | NOT NULL |
| validate:"gt=0" | CHECK (seats > 0) |
| a SELECT before INSERT to check duplicates | UNIQUE, and handle 23505 |
| pgErr.ConstraintName | the name you gave the constraint |
| pgerrcode.ForeignKeyViolation | SQLSTATE 23503 |
