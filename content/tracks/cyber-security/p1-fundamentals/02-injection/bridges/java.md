---
from: java
status: draft
concept: "Keep code and data apart"
from_way: "Statement.executeQuery(\"... WHERE name = '\" + name + \"'\") and string-built JPQL."
native_way: "PreparedStatement with ? parameters, JPA named parameters, allow-lists for ORDER BY, and ProcessBuilder with an argument list."
sensei_tip: "Spring Data's `Sort.by(...)` from user input still needs an allow-list of property names; `JpaSort.unsafe` is called that for a reason."
---

| Java | Injection-safe way |
| --- | --- |
| Statement with concatenation | PreparedStatement.setString(1, name) |
| "ORDER BY " + sort | an allow-list map to column names |
| Runtime.exec("ping " + host) | new ProcessBuilder("ping", "-c", "1", host) |
| new File(base, name) | base.resolve(name).normalize().startsWith(base) |
| createNativeQuery(sql + input) | named parameters |
