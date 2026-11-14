# Library API with JPA

The entities `Author` and `Book` (a `@ManyToOne(fetch = LAZY)` to its author), the records
and the error handling are written. Spring Boot creates an in-memory H2 database and its
tables from the entities. You write the two repositories, the service and the controller.

## Endpoints

| Request | Answer |
| --- | --- |
| `POST /books` `{"isbn", "title", "year", "author"}` (validated) | `201`, `Location: .../books/{isbn}`, the `BookView` |
| `GET /books/{isbn}` | `200`, the `BookView` |
| `GET /books?q=hobbit` | `200`, the books whose title contains the text, **ignoring case**, sorted by title |
| `GET /authors/{name}/books?page=0&size=10` | `200`, a `PageView` of that author's books, sorted by **year, then title** |
| `GET /authors/stats` | `200`, `AuthorStats` for every author with books: most books first, then by name |

## Rules

- `add` is **transactional**: it strips the text fields, refuses an isbn that exists
  (`LibraryException.duplicateIsbn`), and **reuses** the author with that name or creates it.
- An unknown isbn is `LibraryException.bookNotFound`.
- `BookView.of(book)` reads `book.getAuthor().getName()`, so loading books without their
  author causes one extra query per book (**N+1**). The search must run **one** SQL statement,
  and an author's page at most two (the page and its count): fetch the authors with the books.
- `stats` is one JPQL query that selects straight into the record:
  `select new dojo.AuthorStats(a.name, count(b)) from Book b join b.author a group by ...`.

> Tip: `Optional<Author> findByName(String name)`, `boolean existsByIsbn(String isbn)`,
> `@Query("select b from Book b join fetch b.author where ...")`, `@EntityGraph(attributePaths = "author")`,
> `Page<Book> findByAuthorName(String name, Pageable pageable)`,
> `PageRequest.of(page, size, Sort.by("year").and(Sort.by("title")))`, and `@Transactional`.
