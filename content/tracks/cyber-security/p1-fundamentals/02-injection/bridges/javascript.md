---
from: javascript
status: draft
concept: "Keep code and data apart"
from_way: "Template literals in SQL (`SELECT ... '${name}'`) and child_process.exec with a string."
native_way: "Parameterised queries (`$1` in pg, `?` in mysql2), execFile or spawn with an argument array, and path containment checks."
sensei_tip: "Tagged templates in libraries like `postgres` or Prisma's `$queryRaw` parameterise for you; plain template literals and `$queryRawUnsafe` do not."
---

| JavaScript / TypeScript | Injection-safe way |
| --- | --- |
| query(`... '${name}'`) | query("... $1", [name]) |
| exec(`ping ${host}`) | execFile("ping", ["-c", "1", host]) |
| path.join(base, name) | path.resolve(base, name) checked against base |
| { $where: input } in MongoDB | typed filters, reject operator keys |
| $queryRawUnsafe(sql) | $queryRaw tagged template |
