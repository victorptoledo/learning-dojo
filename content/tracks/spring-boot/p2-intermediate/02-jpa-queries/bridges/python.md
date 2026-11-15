---
from: python
status: draft
concept: "Joins, queries and pages"
from_way: "Django querysets with select_related and prefetch_related, or SQLAlchemy joinedload."
native_way: "Derived query methods, JPQL with @Query, fetch joins, projections and pagination."
sensei_tip: "select_related is a join fetch, values() is a record projection, and the N+1 problem looks exactly the same: watch the SQL log."
---

| Python | Spring Boot 4 |
| --- | --- |
| Book.objects.filter(author__name=n) | findByAuthorName(n) |
| select_related('author') | join fetch / @EntityGraph |
| values('title', 'price') | a record projection |
| Paginator | Pageable and Page<T> |
| raw SQL | @Query(nativeQuery = true) |
