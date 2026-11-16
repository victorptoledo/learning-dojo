---
from: java
status: reviewed
concept: "Types that carry rules"
from_way: "Java enums, value objects such as an Email record with validation in its constructor, List<String> fields and Bean Validation annotations."
native_way: "Enums, domains, arrays and ranges in the database, with exclusion constraints that hold for every client."
sensei_tip: "A @Column with @Enumerated(EnumType.STRING) and a CHECK constraint is easier to evolve than a PostgreSQL ENUM: adding a Java constant then needs only an ALTER TABLE."
---

| Java | PostgreSQL |
| --- | --- |
| enum OrderStatus { NEW, PAID } | CREATE TYPE order_status AS ENUM (...) or CHECK |
| record Email(String value) with validation | CREATE DOMAIN email AS text CHECK (...) |
| List<String> tags | text[] |
| Instant | timestamptz |
| an Interval of two Instants | tstzrange with [) bounds |
| a synchronized "is the room free?" check | EXCLUDE USING gist (room_id WITH =, during WITH &&) |
