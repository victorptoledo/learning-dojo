---
from: javascript
status: draft
concept: "Documents inside a relational table"
from_way: "JSON.parse, optional chaining (doc?.order?.id), and storing documents in MongoDB."
native_way: "jsonb in PostgreSQL: documents with SQL, transactions and joins around them."
sensei_tip: "node-postgres returns jsonb as parsed objects. Use jsonb for the flexible part and columns for what you filter and join on, instead of a separate document store."
---

| JavaScript | SQL |
| --- | --- |
| doc?.order?.id | payload -> 'order' ->> 'id' |
| doc.items.map(i => i.sku) | SELECT item ->> 'sku' FROM jsonb_array_elements(payload -> 'items') item |
| Mongo find({ 'order.id': 42 }) | WHERE payload @> '{"order": {"id": 42}}' |
| JSON.stringify(obj) | to_jsonb(row), jsonb_build_object(...) |
