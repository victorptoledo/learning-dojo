---
from: javascript
status: draft
concept: "Tables, types and keys versus classes, fields and references"
from_way: "Objects with properties, TypeScript interfaces, nested objects for relations and number for everything numeric."
native_way: "Flat rows with exact types; relations are foreign keys, not nesting."
sensei_tip: "JavaScript numbers are doubles: read numeric columns as strings (node-postgres does) or use a decimal library, never parseFloat for money."
---

| JavaScript | SQL |
| --- | --- |
| interface Order { customer: Customer } | orders.customer_id REFERENCES customers (id) |
| number | integer, bigint, numeric or double precision: you choose |
| Date | timestamptz |
| prop?: string | a nullable column |
