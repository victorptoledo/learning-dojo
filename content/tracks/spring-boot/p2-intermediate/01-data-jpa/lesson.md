# Spring Data JPA and Transactions

Most services store data in a relational database. **JPA** (Jakarta Persistence) maps Java
classes to tables, **Hibernate** is the implementation Spring Boot uses, and **Spring Data JPA**
writes the repository code for you. Transactions keep a set of changes all-or-nothing.

## Entities

An **entity** is a class whose instances are rows of a table:

```java
@Entity
@Table(name = "books")
public class Book {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, unique = true)
    private String isbn;

    private int copies;

    protected Book() {}                       // JPA needs a no-argument constructor

    public Book(String title, String isbn, int copies) {
        this.title = title;
        this.isbn = isbn;
        this.copies = copies;
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public int getCopies() { return copies; }

    public void lend() {                      // behaviour that changes state, with its rule
        if (copies == 0) throw new IllegalStateException("no copies left of " + isbn);
        copies--;
    }
}
```

Rules worth knowing:

- An entity needs `@Entity`, an `@Id`, and a no-argument constructor (it can be `protected`).
- Entities are **mutable classes, not records**: Hibernate creates them empty and fills the
  fields, and tracks changes to them. Records stay perfect for DTOs and projections.
- `@GeneratedValue(strategy = IDENTITY)` lets the database assign ids on insert.
- Without `@Column`, every field is a column named after it (`copies` → `copies`).
- Give entities methods with rules (`lend()`) instead of public setters for everything.

With an embedded database (H2) on the classpath and no URL configured, Boot creates an
in-memory database and generates the tables from the entities
(`spring.jpa.hibernate.ddl-auto=create-drop`). In real projects the schema is managed by a
migration tool such as **Flyway** or **Liquibase**, and `ddl-auto` is `validate` or `none`.

## Repositories for free

```java
public interface BookRepository extends JpaRepository<Book, Long> {
    Optional<Book> findByIsbn(String isbn);
    List<Book> findByTitleContainingIgnoreCaseOrderByTitle(String text);
    boolean existsByIsbn(String isbn);
    long countByCopiesGreaterThan(int copies);
}
```

You write the interface; Spring Data generates the implementation at startup.
`JpaRepository<Book, Long>` already has `save`, `findById`, `findAll`, `count`, `deleteById`,
`existsById` and paging variants. **Derived query methods** turn their name into a query:

| Keyword | Example | SQL idea |
| --- | --- | --- |
| `findBy`, `existsBy`, `countBy`, `deleteBy` | `findByIsbn(isbn)` | `where isbn = ?` |
| `And`, `Or` | `findByAuthorAndYear(a, y)` | `where author = ? and year = ?` |
| `Containing`, `StartingWith`, `IgnoreCase` | `findByTitleContainingIgnoreCase(t)` | `where lower(title) like %t%` |
| `GreaterThan`, `Between`, `In`, `IsNull` | `findByYearBetween(a, b)` | `where year between ? and ?` |
| `OrderBy...Asc/Desc`, `First`, `Top3` | `findTop3ByOrderByCopiesDesc()` | `order by ... limit 3` |

A method name that does not match the entity's fields fails at **startup**, not at run time.
When names get long, write the query yourself (next lesson).

## Transactions

A **transaction** groups database changes so they all happen or none do. In Spring you mark
the boundary with `@Transactional`, usually on **service** methods:

```java
@Service
public class LendingService {
    private final BookRepository books;
    private final LoanRepository loans;

    LendingService(BookRepository books, LoanRepository loans) { ... }

    @Transactional
    public Loan lend(String isbn, long memberId) {
        Book book = books.findByIsbn(isbn).orElseThrow(() -> new BookNotFound(isbn));
        book.lend();                                       // changes the entity...
        return loans.save(new Loan(book, memberId));       // ...and adds a loan
    }                                                      // commit: both, or neither
}
```

What `@Transactional` gives you:

- **Atomicity**: if the method throws a **runtime** exception, everything it changed is
  **rolled back**. (Checked exceptions do *not* roll back unless you say
  `@Transactional(rollbackFor = Exception.class)`.)
- **Dirty checking**: entities loaded inside the transaction are *managed*. Changing them
  (`book.lend()`) is enough; Hibernate writes the update at commit, no `save` needed.
- **One connection** for the whole method, so reads see your own writes.

`@Transactional(readOnly = true)` on read methods tells Hibernate it can skip dirty checking.

> [!WARNING]
> `@Transactional` works through a **proxy**: it applies when another bean calls the method.
> A call from a method in the **same class** (`this.lend(...)`) skips the proxy and runs
> without a transaction. Put transactional methods on a bean that others call.

Keep transactions **short** and around database work only: do not call slow remote services
or wait for user input inside one; that holds a connection and locks.

## Do not return entities from controllers

Map entities to response records in the service or controller:

```java
public record BookView(long id, String title, int copies) {
    static BookView of(Book book) {
        return new BookView(book.getId(), book.getTitle(), book.getCopies());
    }
}
```

Serialising entities directly exposes every column, ties the API to the schema, and can
trigger lazy loading (or errors) outside the transaction.

## Summary

- Entities are classes with `@Entity`, an `@Id` and a no-argument constructor; keep rules in
  their methods.
- Extend `JpaRepository<T, ID>` and declare derived query methods; Spring Data implements them.
- `@Transactional` service methods commit everything or roll back on runtime exceptions, and
  write changes to managed entities automatically.
- Self-invocation skips the transaction proxy; keep transactions short.
- Return records, not entities, from the API.
