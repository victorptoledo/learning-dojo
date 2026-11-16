---
from: python
status: draft
concept: "Keeping history of changing entities"
from_way: "Overwriting rows in a DataFrame or table when the source changes."
native_way: "Close the current version and append a new one for tracked attributes; overwrite only corrections."
sensei_tip: "In pandas, a merge on the natural key plus a comparison of tracked columns (with NaN handled: a.ne(b) is True for NaN vs NaN) gives the changed rows; in SQL, IS DISTINCT FROM does it right."
---

| Python | Slowly changing dimensions |
| --- | --- |
| df.update(new) | type 1 |
| pd.concat([closed, new_versions]) | type 2 |
| pd.merge_asof by date | a point-in-time join |
| a != b (None) | careful: compare NULLs on purpose |
| dbt snapshot | type 2 from a mutable table |
