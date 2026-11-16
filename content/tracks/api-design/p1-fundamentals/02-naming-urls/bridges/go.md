---
from: go
status: draft
concept: "Naming and representation conventions"
from_way: "Exported Go field names (`CustomerID`) appearing in JSON, `float64` prices, `time.Time` in local time."
native_way: "Explicit `json:\"customerId\"` tags, `int64` cents, `time.Time` in UTC (marshalled as RFC 3339), string types with constants for enums."
sensei_tip: "`time.Time` marshals as RFC 3339 with nanoseconds; truncate or format explicitly if your contract promises second precision."
---

| Go | Convention |
| --- | --- |
| `json:"customerId"` | deliberate field name |
| `time.Now().UTC()` | RFC 3339 timestamp |
| `int64` cents | money |
| `type Status string` + constants | named enum values |
| `json:",omitempty"` | absent versus empty: decide |
