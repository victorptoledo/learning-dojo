---
from: java
status: draft
concept: "Creating, changing and removing stored records"
from_way: "repository.save(entity), dirty checking in JPA, and JDBC executeUpdate returning a row count."
native_way: "INSERT, UPDATE and DELETE over sets of rows, RETURNING the result and ON CONFLICT for upserts."
sensei_tip: "One UPDATE ... WHERE over a thousand rows beats a thousand saves. And check the int that executeUpdate returns: 0 rows updated is a result, not success."
---

| Java | SQL |
| --- | --- |
| repo.save(new Customer(...)) | INSERT INTO customers (...) VALUES (...) RETURNING id |
| entity.setPrice(...) then flush | UPDATE products SET price = ... WHERE id = ... |
| repo.deleteById(id) | DELETE FROM ... WHERE id = ... |
| int n = ps.executeUpdate() | the row count: UPDATE 3 |
| findById, then save (racy) | INSERT ... ON CONFLICT (key) DO UPDATE |
