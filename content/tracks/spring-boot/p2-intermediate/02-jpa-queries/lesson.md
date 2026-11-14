# Relationships, Queries and Pagination

Real data is connected: books have authors, orders have lines, customers have addresses.
This lesson covers mapping those relationships, the performance trap they bring (**N+1
queries**), writing your own queries, and returning large results one page at a time.

## Mapping relationships

The most common pair is **many-to-one** with its inverse **one-to-many**:

```java
@Entity
public class Author {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;

    @OneToMany(mappedBy = "author")
    private List<Book> books = new ArrayList<>();
    ...
}

@Entity
public class Book {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String title;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id")
    private Author author;
    ...
}
```

- The **many** side (`Book.author`) owns the foreign key column `author_id`.
- `mappedBy = "author"` says the `Author.books` list is the inverse view of that same column;
  it does not create another one.
- Often you do not need the `@OneToMany` at all: `bookRepository.findByAuthorId(id)` answers the
  same question without loading a collection.
- `@ManyToMany` exists (with a join table), but a many-to-many with extra data (a date, a role)
  is better modelled as its own entity with two `@ManyToOne`.

## Lazy and eager loading

`FetchType.LAZY` loads the related entity **only when you touch it**; `EAGER` loads it with
the parent every time. Defaults: `@ManyToOne` is **eager**, `@OneToMany` is lazy.

> [!TIP]
> Make every association `LAZY` (set it explicitly on `@ManyToOne`) and fetch what each use
> case needs in its query. Eager loading on everything pulls half the database for one row.

Lazy associations can only load while the persistence context is open, that is, inside the
transaction. Touching one afterwards throws `LazyInitializationException`. That is one more
reason to map entities to records inside the `@Transactional` service method. Set
`spring.jpa.open-in-view=false` so this shows up in development instead of being hidden.

## The N+1 problem

```java
@Transactional(readOnly = true)
public List<BookView> all() {
    return books.findAll().stream()
            .map(b -> new BookView(b.getTitle(), b.getAuthor().getName()))  // one query per book!
            .toList();
}
```

One query loads the books, then **one more per book** loads its author: 1 + N queries. With
1,000 books that is 1,001 round trips. Fixes:

```java
// 1. a fetch join: authors come in the same query
@Query("select b from Book b join fetch b.author order by b.title")
List<Book> findAllWithAuthor();

// 2. an entity graph on a derived or inherited method
@EntityGraph(attributePaths = "author")
List<Book> findByTitleContainingIgnoreCase(String text);

// 3. a projection: select only the columns you need, straight into a record
@Query("select new dojo.BookView(b.title, a.name) from Book b join b.author a order by b.title")
List<BookView> findAllViews();
```

Watch the SQL in development (`logging.level.org.hibernate.SQL=debug`) and count the queries
a request makes.

## Writing queries with @Query

`@Query` takes **JPQL**, a query language over entities and their fields (not tables and
columns):

```java
@Query("""
        select b from Book b
        where b.author.name = :author and b.year >= :from
        order by b.year desc
        """)
List<Book> recentByAuthor(@Param("author") String author, @Param("from") int from);

@Modifying
@Query("update Book b set b.copies = b.copies + :n where b.id = :id")
int addCopies(@Param("id") long id, @Param("n") int n);
```

- Use **named parameters** (`:author`), never string concatenation: it prevents SQL
  injection and lets the database reuse the plan.
- `@Modifying` marks an update or delete query; it needs a transaction.
- `nativeQuery = true` runs real SQL when you need database features JPQL lacks.

## Pagination

Never return an unbounded list from an API. Repositories accept a `Pageable` and return a
`Page`:

```java
Page<Book> findByAuthorName(String name, Pageable pageable);

Page<Book> page = books.findByAuthorName("Austen",
        PageRequest.of(0, 20, Sort.by("title").ascending()));
page.getContent();        // the 20 books of page 0
page.getTotalElements();  // how many match in total
page.getTotalPages();
```

`Page` runs an extra `count` query; `Slice` skips it when you only need "is there a next page".
In a controller, a `Pageable` parameter binds `?page=0&size=20&sort=title,asc` automatically.
Map the page to your own response record rather than returning `Page` itself, so the JSON
shape stays under your control:

```java
public record PageView<T>(List<T> items, int page, int size, long total) {
    static <E, T> PageView<T> of(Page<E> page, Function<E, T> mapper) {
        return new PageView<>(page.map(mapper).getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements());
    }
}
```

## Summary

- `@ManyToOne` owns the foreign key; `@OneToMany(mappedBy = ...)` is its inverse, and often
  not needed.
- Make associations `LAZY` and fetch what each use case needs; lazy loading needs an open
  transaction.
- Fix N+1 with `join fetch`, `@EntityGraph` or projections into records.
- `@Query` with JPQL and named parameters; `@Modifying` for updates.
- Return pages (`Pageable`, `Page`) mapped to your own record, never unbounded lists.
