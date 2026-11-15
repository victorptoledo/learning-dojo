---
from: go
status: draft
concept: "Tables, types and keys versus classes, fields and references"
from_way: "A struct with typed fields and pointers or sql.Null* types for optional values."
native_way: "A table with typed columns; NULL is what your sql.NullString represents."
sensei_tip: "Pick column types first, then the Go type: numeric maps to a string or a decimal package, timestamptz to time.Time, a nullable column to a pointer."
---

| Go | SQL |
| --- | --- |
| type Order struct { CustomerID int64 } | customer_id bigint REFERENCES customers (id) |
| *string or sql.NullString | a nullable text column |
| time.Time | timestamptz |
| shopspring/decimal | numeric |
