---
from: java
status: draft
concept: "Documents inside a relational table"
from_way: "Jackson to parse JSON into Maps or POJOs, and a JSON column mapped with Hibernate's JSON support."
native_way: "jsonb columns queried with ->, ->>, @> and JSON_TABLE, and built with json_build_object."
sensei_tip: "Do not fetch a jsonb column to parse it in Java just to filter on one field: filter with ->> or @> in the WHERE, and index that expression."
---

| Java | SQL |
| --- | --- |
| node.get("order").get("id").asLong() | (payload -> 'order' ->> 'id')::bigint |
| mapper.readValue(json, Map.class) | a jsonb value |
| objectMapper.writeValueAsString(dto) | json_build_object(...), json_agg(...) |
| @JdbcTypeCode(SqlTypes.JSON) | a jsonb column |
| list.stream() over a JSON array | jsonb_array_elements or JSON_TABLE |
