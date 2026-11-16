---
from: javascript
status: reviewed
concept: "Documents and search inside the database"
from_way: "Plain JavaScript objects, JSON.parse, MongoDB queries, and filtering arrays with includes() for search."
native_way: "jsonb documents queried with containment and jsonpath, and full-text search with tsvector, tsquery and GIN indexes."
sensei_tip: "node-postgres parses jsonb into objects for you. Pass filters as one JSON parameter: WHERE attrs @> $1::jsonb with JSON.stringify(filter), and the GIN index does the rest."
---

| JavaScript / TypeScript | PostgreSQL |
| --- | --- |
| obj.specs.weight_g | attrs #>> '{specs,weight_g}' |
| { ...obj, color: 'red' } | jsonb concatenation with '{"color": "red"}' |
| 'waterproof' in obj | attrs ? 'waterproof' |
| db.products.find({ brand: 'Kestrel' }) | attrs @> '{"brand": "Kestrel"}' |
| text.includes('login') | doc @@ websearch_to_tsquery('english', 'login') |
