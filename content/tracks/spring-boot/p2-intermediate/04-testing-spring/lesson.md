# Testing Spring Applications

Spring makes testing easy, and it also makes it easy to write slow tests. The skill is
choosing the **smallest test that proves the behaviour**: a plain unit test when you can, a
**slice** of the application when the framework matters, the **whole context** when the
pieces must work together.

## Three levels

| Level | Starts | Speed | Use for |
| --- | --- | --- | --- |
| Plain unit test | no Spring: `new Service(fakes)` | milliseconds | business rules in services and domain objects |
| Slice test | only one layer (`@WebMvcTest`, `@DataJpaTest`, ...) | ~1 s | controllers' HTTP behaviour, repositories' queries |
| `@SpringBootTest` | the whole application | a few seconds | end-to-end flows across layers |

Constructor injection is what makes the first level possible: no annotation, no context.

```java
class ShippingServiceTest {
    @Test
    void free_from_fifty_euros() {
        var shop = new ShopProperties("EUR", new BigDecimal("50.00"), 20, Duration.ofMinutes(30));
        var shipping = new ShippingService(shop);
        assertTrue(shipping.isFree(new BigDecimal("50.00")));
    }
}
```

## Web slice: @WebMvcTest

`@WebMvcTest` starts only the MVC layer: controllers, advice, converters, validation. Services
and repositories are **not** created; you replace what the controller needs with
**`@MockitoBean`**:

```java
@WebMvcTest(TodoController.class)
class TodoControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean TodoService todos;

    @Test
    void unknown_todo_is_404() throws Exception {
        when(todos.find(7)).thenReturn(Optional.empty());
        mvc.perform(get("/todos/7")).andExpect(status().isNotFound());
    }

    @Test
    void creates_a_todo() throws Exception {
        when(todos.create("learn")).thenReturn(new Todo(1, "learn", false));
        mvc.perform(post("/todos").contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"learn\"}"))
                .andExpect(status().isCreated());
        verify(todos).create("learn");
    }
}
```

In Spring Boot 4 the slice annotations live in the technology's test module:
`org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest` (from
`spring-boot-starter-webmvc-test`), and `@MockitoBean` comes from Spring Framework
(`org.springframework.test.context.bean.override.mockito`). The old `@MockBean` is gone.

`MockMvcTester` is an AssertJ-style alternative to `MockMvc`:

```java
@Autowired MockMvcTester mvc;

assertThat(mvc.get().uri("/todos/7")).hasStatus(HttpStatus.NOT_FOUND);
```

## Data slice: @DataJpaTest

`@DataJpaTest` starts JPA, the repositories and an embedded database, and wraps **every test
in a transaction that is rolled back** at the end, so tests do not see each other's data:

```java
@DataJpaTest
class BookRepositoryTest {
    @Autowired BookRepository books;
    @Autowired TestEntityManager em;

    @Test
    void finds_by_title_ignoring_case() {
        var author = em.persist(new Author("Tolkien"));
        em.persist(new Book("i1", "The Hobbit", 1937, author));
        assertEquals(1, books.findByTitleContainingIgnoreCase("hobbit").size());
    }
}
```

Test the queries you wrote (`@Query`, tricky derived names); there is no value in testing that
`save` saves.

## The whole application: @SpringBootTest

```java
@SpringBootTest
@AutoConfigureMockMvc
class OrderFlowTest {
    @Autowired MockMvc mvc;
    ...
}
```

`@SpringBootTest` starts everything; with `@AutoConfigureMockMvc` you drive it through
MockMvc. `@SpringBootTest(webEnvironment = RANDOM_PORT)` starts a real server on a free port,
which you call with `RestTestClient` or an HTTP client when you need real HTTP.

Things to know:

- **The context is cached.** Test classes with the same configuration share one context, so
  only the first one pays the startup. Every different `@MockitoBean`, property or profile
  creates a new context: keep test configurations few and alike.
- **State leaks between tests** in a shared context: an in-memory map or a database keeps what
  earlier tests wrote. Make tests independent (unique data per test), or use `@Transactional`
  on the test class (rolled back after each test, but only for code running in the test's
  thread), or clean up in `@AfterEach`. `@DirtiesContext` throws the context away and is slow.
- Override settings per test class with `@SpringBootTest(properties = "shop.currency=USD")`.

## Real databases: Testcontainers

H2 is not PostgreSQL: SQL dialect, types and constraints differ. For repository and flow tests
that must match production, **Testcontainers** starts a real database in Docker:

```java
@SpringBootTest
@Testcontainers
class OrderFlowIT {
    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18-alpine");
}
```

`@ServiceConnection` wires the container's URL, user and password into Boot automatically.

## What to test where

- Business rules: **unit tests**, many of them, fast.
- HTTP mapping, status codes, validation, error bodies: **`@WebMvcTest`**.
- Custom queries and mappings: **`@DataJpaTest`** (or Testcontainers).
- A few critical flows end to end: **`@SpringBootTest`**.

## Summary

- Prefer plain unit tests; constructor injection makes them possible.
- `@WebMvcTest` + `@MockitoBean` for controllers, `@DataJpaTest` (rolled back per test) for
  repositories, `@SpringBootTest` for flows.
- In Boot 4, slice annotations moved to per-technology test modules; `@MockBean` was replaced
  by `@MockitoBean`.
- Contexts are cached per configuration; keep tests independent of each other's data.
- Use Testcontainers when the real database matters.
