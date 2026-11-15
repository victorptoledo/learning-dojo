---
from: go
status: draft
concept: "Specification"
from_way: "One repository function per filter combination, and SQL built with `fmt.Sprintf`."
native_way: "Function types (`type Spec func(Customer) bool`) for in-memory rules, and filter structs or query builders (squirrel) that render parameterised SQL."
sensei_tip: "Never build SQL with `fmt.Sprintf(\"... = '%s'\", v)`. A small spec-to-SQL renderer that returns the clause and `[]any` args, passed to `db.Query(clause, args...)`, is safe and testable."
---

| Go | Specification idea |
| --- | --- |
| `type Spec func(Customer) bool` | rule as a function |
| `func And(a, b Spec) Spec` | composition |
| squirrel `sq.And{sq.Eq{...}}` | specs as query data |
| `db.Query(clause, args...)` | parameterised rendering |
| `fmt.Sprintf` SQL | injection risk (avoid) |
