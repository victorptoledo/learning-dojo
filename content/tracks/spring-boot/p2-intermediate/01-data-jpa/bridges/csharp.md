---
from: csharp
status: draft
concept: "Database access with Spring Data JPA"
from_way: "Entity Framework Core with a DbContext, change tracking and migrations."
native_way: "JPA with Hibernate, a persistence context, and Spring Data repositories."
sensei_tip: "DbContext with change tracking is JPA's EntityManager; SaveChanges is the flush at commit. Spring Data repositories replace hand-written DbSet queries."
---

| C# | Spring Boot 4 |
| --- | --- |
| DbContext | EntityManager / persistence context |
| DbSet<Book> | JpaRepository<Book, Long> |
| SaveChangesAsync() | flush at commit |
| dotnet ef migrations | Flyway |
| Include(b => b.Author) | join fetch or @EntityGraph |
