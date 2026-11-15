---
from: python
status: draft
concept: "Documents inside a relational table"
from_way: "json.loads into dicts and lists, and dict access like doc['order']['id']."
native_way: "jsonb with -> and ->> operators, containment and JSON path, inside SQL."
sensei_tip: "psycopg turns jsonb into dicts automatically, but querying inside the database avoids transferring whole documents to read one key."
---

| Python | SQL |
| --- | --- |
| doc['order']['id'] | payload -> 'order' ->> 'id' |
| doc.get('coupon') | payload ->> 'coupon' (NULL when missing) |
| 'coupon' in doc | payload ? 'coupon' |
| for item in doc['items']: | jsonb_array_elements(payload -> 'items') |
| json.dumps({...}) | jsonb_build_object(...) |
