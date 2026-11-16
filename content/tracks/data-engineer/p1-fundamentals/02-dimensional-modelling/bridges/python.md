---
from: python
status: draft
concept: "Modelling for reading, not writing"
from_way: "One wide pandas DataFrame per report, built by merging whatever tables it needed."
native_way: "A reusable star: one fact table per process at a declared grain, shared dimensions, then any report is a join and a groupby."
sensei_tip: "A tidy DataFrame (one row per observation) is the same idea as a fact table's grain. Write the grain as a comment on every model and test it with a uniqueness check."
---

| Python | Dimensional modelling |
| --- | --- |
| one row per observation (tidy data) | the grain |
| df.merge(dim, on='customer_key') | a star join |
| df.groupby(['year', 'category']).revenue.sum() | an aggregate over dimension attributes |
| pd.date_range | a date dimension |
| Categorical columns | dimension attributes |
