---
from: python
status: draft
concept: "Rules the data itself enforces"
from_way: "Pydantic or form validators, and checks in view code before saving."
native_way: "Constraints in the table definition, enforced for every client and every race."
sensei_tip: "Validate input in Python for good messages, but rely on the constraint. psycopg raises errors.UniqueViolation and errors.ForeignKeyViolation you can catch."
---

| Python | SQL |
| --- | --- |
| Field(gt=0) | CHECK (seats > 0) |
| Optional vs required field | nullable vs NOT NULL |
| if User.objects.filter(email=e).exists(): ... | UNIQUE (email), and catch UniqueViolation |
| ForeignKey(on_delete=models.CASCADE) | REFERENCES ... ON DELETE CASCADE |
| psycopg.errors.UniqueViolation | SQLSTATE 23505 |
