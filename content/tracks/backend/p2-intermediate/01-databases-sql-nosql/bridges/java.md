---
from: java
status: draft
concept: "Persistence model"
from_way: "Mapping everything to ORM entities and producing hidden N+1 queries."
native_way: "Write explicit SQL or use light query builders. Relational for integrity and strict consistency; NoSQL for data without a fixed schema."
sensei_tip: "Before adding a NoSQL database, ask whether Postgres with JSONB columns and GIN indexes already solves it."
---
