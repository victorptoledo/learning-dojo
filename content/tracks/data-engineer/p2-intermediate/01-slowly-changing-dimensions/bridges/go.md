---
from: go
status: draft
concept: "Keeping history of changing entities"
from_way: "UPDATE statements that overwrite the row when a field changes."
native_way: "Close and insert versions in one transaction, comparing tracked fields NULL-safely."
sensei_tip: "Compare sql.NullString values with both Valid and String, or let SQL do it with IS DISTINCT FROM: a plain != on the strings misses NULL to value changes."
---

| Go | Slowly changing dimensions |
| --- | --- |
| UPDATE ... SET | type 1 |
| UPDATE to close + INSERT, one tx | type 2 |
| sql.NullString | a nullable tracked attribute |
| time.Date(9999, 12, 31, ...) | the open-ended valid_to |
| sort.Search on validFrom | a point-in-time lookup |
