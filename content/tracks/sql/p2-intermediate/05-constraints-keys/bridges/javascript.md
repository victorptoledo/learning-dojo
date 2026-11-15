---
from: javascript
status: draft
concept: "Rules the data itself enforces"
from_way: "Zod or Joi schemas, and checks in route handlers before calling the ORM."
native_way: "Constraints declared once in the schema, enforced by the database."
sensei_tip: "Zod protects the API, constraints protect the data. Map Prisma's P2002 (unique) and P2003 (foreign key) errors, or pg's error.code 23505, to HTTP 409 and 422."
---

| JavaScript | SQL |
| --- | --- |
| z.number().positive() | CHECK (seats > 0) |
| z.string() (required) | NOT NULL |
| @unique in schema.prisma | UNIQUE |
| @relation(onDelete: Cascade) | ON DELETE CASCADE |
| err.code === '23505' | unique_violation |
