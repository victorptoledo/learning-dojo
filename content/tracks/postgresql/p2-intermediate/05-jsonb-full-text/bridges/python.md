---
from: python
status: reviewed
concept: "Documents and search inside the database"
from_way: "dicts and json.loads, Django's JSONField lookups, and icontains filters for search."
native_way: "jsonb with containment, key and jsonpath operators, and full-text search with tsvector and GIN indexes."
sensei_tip: "Django's JSONField __contains lookup compiles to @> and can use a GIN index; __key='x' compiles to -> comparisons and cannot. django.contrib.postgres.search gives you SearchVector, SearchQuery and SearchRank."
---

| Python | PostgreSQL |
| --- | --- |
| doc["specs"]["weight_g"] | attrs -> 'specs' -> 'weight_g' |
| {**doc, "color": "red"} | attrs or-merged with '{"color": "red"}' (the jsonb concatenation operator) |
| "waterproof" in doc | attrs ? 'waterproof' |
| Product.objects.filter(attrs__contains={...}) | attrs @> '{...}' |
| SearchVector('title', weight='A') | setweight(to_tsvector('english', title), 'A') |
