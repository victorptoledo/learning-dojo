---
from: csharp
status: draft
concept: "Joins, queries and pages"
from_way: "LINQ to Entities with Include, Select projections and Skip/Take."
native_way: "JPQL, entity graphs, projections and pagination."
sensei_tip: "Include is join fetch, Select(x => new Dto(...)) is a constructor or record projection, and Skip/Take is a Pageable. EF's split queries are Hibernate's batch fetching."
---

| C# | Spring Boot 4 |
| --- | --- |
| Include(b => b.Author) | @EntityGraph(attributePaths = "author") |
| Select(b => new BookDto(...)) | select new BookDto(...) or a record projection |
| Skip(20).Take(10) | PageRequest.of(2, 10) |
| AsNoTracking() | read-only transaction or projections |
