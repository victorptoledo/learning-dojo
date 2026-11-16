---
from: java
status: reviewed
concept: "Documents and search inside the database"
from_way: "Jackson JsonNode trees, a MongoDB or Elasticsearch next to the relational database, and JPA queries with LIKE."
native_way: "jsonb columns queried with containment and jsonpath, and full-text search with tsvector, tsquery and GIN indexes."
sensei_tip: "Hibernate 6 maps a jsonb column to a record or Map with @JdbcTypeCode(SqlTypes.JSON). Filter with @> in a native query, because a JPQL ->> comparison cannot use the GIN index."
---

| Java | PostgreSQL |
| --- | --- |
| node.path("specs").path("weight_g") | attrs #> '{specs,weight_g}' |
| JsonPath.read(json, "$.sizes[*]") | jsonb_path_query(attrs, '$.sizes[*]') |
| ObjectNode.put(...) | jsonb_set(attrs, path, value) |
| Lucene analyzers and stemmers | text search configurations ('english') |
| Elasticsearch match query | doc @@ websearch_to_tsquery('english', q) |
