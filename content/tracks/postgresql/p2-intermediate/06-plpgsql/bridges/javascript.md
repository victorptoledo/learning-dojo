---
from: javascript
status: reviewed
concept: "Code that runs next to the data"
from_way: "ORM hooks such as Sequelize beforeUpdate or Prisma middleware and extensions, run inside the Node process."
native_way: "PL/pgSQL functions and triggers that run inside the database for every client."
sensei_tip: "Prisma's @updatedAt is set by the client; a raw SQL UPDATE skips it. A BEFORE UPDATE trigger keeps the column right for everyone."
---

| JavaScript / TypeScript | PostgreSQL |
| --- | --- |
| a beforeUpdate hook | BEFORE UPDATE row trigger |
| an afterCreate hook writing an audit record | AFTER INSERT trigger |
| throw new Error(...) | RAISE EXCEPTION |
| try / catch on a unique violation (code 23505) | EXCEPTION WHEN unique_violation |
| a pure function | IMMUTABLE |
