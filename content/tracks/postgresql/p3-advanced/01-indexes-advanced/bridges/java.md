---
from: java
status: draft
concept: "Indexing strategy"
from_way: "Putting a plain B-Tree index on every column at random."
native_way: "Partial indexes (`WHERE active = true`) cover only part of the table, saving space and speeding up writes."
sensei_tip: "For huge, naturally ordered columns (logs, metrics), a BRIN index takes almost no space compared with a B-Tree."
---
