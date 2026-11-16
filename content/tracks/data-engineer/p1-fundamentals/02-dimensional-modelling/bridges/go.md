---
from: go
status: draft
concept: "Modelling for reading, not writing"
from_way: "Structs per table and queries written for one request at a time."
native_way: "A star schema that answers aggregate questions with one join per dimension."
sensei_tip: "Keep the grain in mind when you write the loader: a struct per fact row at the declared grain, and dimension lookups that return surrogate keys."
---

| Go | Dimensional modelling |
| --- | --- |
| type OrderLine struct | one fact row at order-line grain |
| map[string]int of id to key | a surrogate key lookup |
| time.Time | a date_key into dim_date |
| int64 cents | an additive measure |
| a nested struct | a dimension to join |
