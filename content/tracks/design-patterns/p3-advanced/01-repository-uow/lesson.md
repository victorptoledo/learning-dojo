# Repository, Unit of Work and Data Mapping

Business code needs to load and store objects. These patterns, catalogued by Martin Fowler
in *Patterns of Enterprise Application Architecture*, decide how much the business code knows
about **how** that happens.

## Repository

**Problem.** Domain and application code fills up with SQL, JDBC or JPA calls, query strings
and mapping code; you cannot test a rule without a database, and the same query is written
in five places.

**Solution.** A repository looks like an in-memory **collection** of domain objects: add,
find, remove, query. Behind it, the mapping to tables happens; in front of it, nothing knows.

```java
public interface Accounts {
    Optional<Account> find(AccountId id);
    void save(Account account);
    List<Account> overdrawnSince(LocalDate day);
}
```

Guidelines:

- **One repository per aggregate root** (an `Order` with its lines, not one for `Order` and
  one for `OrderLine`), named in the domain's words.
- Methods express **domain questions** (`overdrawnSince`), not SQL (`findByStatusAndDateLessThan`)
  — or, when questions multiply, take a **specification** (next lessons).
- The interface belongs to the domain side; the implementation (JPA, JDBC, in memory) is an
  adapter. Tests use an in-memory implementation.
- Avoid the **generic repository** (`Repository<T>` with `findAll`, `update`, `delete` for
  everything): it exposes operations the domain should not allow (deleting order lines on
  their own) and pushes queries back into callers.

**Spring Data** generates repository implementations from interfaces (`interface
AccountRepository extends JpaRepository<Account, Long>`). It is very productive, but its
interfaces expose JPA's whole surface (`findAll`, `deleteAll`, `flush`) and derived query
names. In a larger domain, keep a small domain interface (`Accounts`) and implement it with a
Spring Data repository in the adapter.

## Unit of Work

**Problem.** A business operation changes several objects; they must be written together or
not at all, in the right order, without each object saving itself as it changes.

**Solution.** A unit of work tracks the objects loaded and changed during one business
transaction and writes all the changes at commit, rolling back on failure.

You use one every day: **JPA's persistence context** (the `EntityManager`, Hibernate's
`Session`) is a unit of work, and Spring's `@Transactional` marks its boundaries.

- **Dirty checking**: at flush/commit, JPA compares managed entities with their loaded state
  and issues the `UPDATE`s. Calling `save` on an already managed entity is unnecessary.
- **Write ordering**: inserts, updates and deletes are sent in an order that respects
  constraints.
- **Rollback**: an exception that escapes the `@Transactional` method rolls everything back
  (by default only for unchecked exceptions).

Put the transaction around the **use case** (the application service method), not around each
repository call: two `save` calls in two transactions can leave a debit without its credit.
The hexagonal kata of this phase builds a tiny unit of work by hand so you see the mechanism.

## Identity Map

Within one unit of work, each database row is loaded **once** and represented by **one**
object: load account 42 twice and you get the same instance. JPA's persistence context is an
identity map. It avoids inconsistent copies and saves queries, and it explains surprises:
changes to an entity you only "read" are still flushed, and memory grows when one transaction
loads a million rows (clear the context or use projections for batch jobs).

## Mapping styles

| Pattern | The object ... | Fits |
| --- | --- | --- |
| **Active Record** | saves itself (`order.save()`); one class per table with data and persistence | simple CRUD, Rails, Laravel, Panache |
| **Data Mapper** | knows nothing about the database; a separate mapper (JPA, a hand-written mapper) moves data | rich domain models, testability |
| **Table Data Gateway** / DAO | — a class per table with SQL operations returning rows or DTOs | reports, simple services, `JdbcTemplate` code |
| **Transaction Script** | — procedures that run SQL per use case | small apps, batch jobs |

JPA with annotations sits between active record and data mapper: the entity does not save
itself, but its class carries mapping annotations. When the domain model and the table shape
differ a lot, map explicitly between domain objects and persistence entities (more code, full
freedom).

## Pitfalls

- **N+1 queries**: a loop over orders touching `order.getCustomer()` lazily loads one
  customer per order; fetch what the use case needs (fetch joins, entity graphs, projections).
- **Open session in view**: lazy loading during JSON rendering hides queries in the web
  layer; Spring Boot warns when `spring.jpa.open-in-view` is left on.
- **Entities as API**: returning entities as JSON couples your API to your schema and
  triggers lazy loads; map to response records.
- **Anemic repositories of anemic objects**: if every rule lives in services that call
  setters, the repository is just a DAO. That is fine for CRUD, a smell for complex rules.

## Further reading

- Martin Fowler, "Repository": <https://martinfowler.com/eaaCatalog/repository.html>
- Martin Fowler, "Unit of Work": <https://martinfowler.com/eaaCatalog/unitOfWork.html>
- Martin Fowler, "Identity Map": <https://martinfowler.com/eaaCatalog/identityMap.html>
- Martin Fowler, "Data Mapper": <https://martinfowler.com/eaaCatalog/dataMapper.html> and "Active Record": <https://martinfowler.com/eaaCatalog/activeRecord.html>
- Spring Data JPA reference, "Core concepts" of repositories: <https://docs.spring.io/spring-data/jpa/reference/repositories/core-concepts.html>
- Spring Framework reference, "Declarative Transaction Management": <https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative.html>
- roadmap.sh topic "Repositories": <https://github.com/nilbuild/developer-roadmap/blob/master/roadmaps/software-design-architecture/content/repositories@8y0ot5sbplUIUyXe9gvc8.md>
- roadmap.sh topic "Identity Maps": <https://github.com/nilbuild/developer-roadmap/blob/master/roadmaps/software-design-architecture/content/identity-maps@tb0X1HtuiGwz7YhQ5xPsV.md>
