---
from: javascript
status: draft
concept: "Storing each fact once"
from_way: "Nested JSON documents and denormalised Redux state (normalizr exists for a reason)."
native_way: "Normalised tables, joined when needed."
sensei_tip: "Normalising Redux state by id (entities and ids) is the same idea as normal forms: one place per fact, references by key."
---

| JavaScript | SQL |
| --- | --- |
| { order: { customer: { name } } } | orders.customer_id → customers.name |
| tags: ['a', 'b'] inside the row | a junction or tags table |
| normalizr entities by id | tables keyed by primary key |
| a copied total in a cache | deliberate denormalisation |
