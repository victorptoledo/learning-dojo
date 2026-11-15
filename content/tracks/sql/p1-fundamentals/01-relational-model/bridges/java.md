---
from: java
status: draft
concept: "Tables, types and keys versus classes, fields and references"
from_way: "A class with typed fields, object references between classes, and BigDecimal for money."
native_way: "A table with typed columns, foreign keys instead of references, and numeric for money."
sensei_tip: "A JPA @Entity is a view of a table, not the other way round. Design the table first (keys, types, NOT NULL), then map it."
---

| Java | SQL |
| --- | --- |
| class Order { Customer customer; } | orders.customer_id REFERENCES customers (id) |
| @Id @GeneratedValue Long id | id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY |
| BigDecimal price | price numeric(10, 2) |
| Instant createdAt | created_at timestamptz |
| LocalDate birthday | birthday date |
