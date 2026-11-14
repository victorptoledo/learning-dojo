---
from: java
status: draft
concept: "Analytical storage on disk"
from_way: "Exporting and reading gigabyte-sized CSV dumps."
native_way: "Parquet stores data by column with min/max statistics in its metadata, so readers skip irrelevant data."
sensei_tip: "An analytical query like `SELECT AVG(salary)` on Parquet reads a single column, often far faster than on CSV."
---
