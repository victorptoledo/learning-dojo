---
from: go
status: draft
concept: "Security in ordinary code"
from_way: "Decoding JSON straight into the database struct."
native_way: "Dedicated request structs, ownership checks and limits."
sensei_tip: "json.Decode into the model struct sets any exported field present in the body; decode into a request struct with only the editable fields, and use DisallowUnknownFields."
---

| Go | In practice |
| --- | --- |
| Decode(&user) | Decode(&updateUserRequest) |
| WHERE id = $1 | WHERE id = $1 AND owner_id = $2 |
| x/time/rate | rate limits |
| returning the model | a response struct |
