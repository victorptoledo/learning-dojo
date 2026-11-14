# Checkpoint 2: Intermediate

This checkpoint closes the **Intermediate** phase. It has two parts, and you need both:

1. **The quiz** (press **Q**): eight questions over the whole phase. You need 7 right.
2. **The integration kata** (press **Enter**): the loans of a small library, with a generic
   repository, `Optional` lookups, a sealed hierarchy of exceptions and stream queries.

Do them in any order. Before you start, review:

| Lesson | Ask yourself |
| --- | --- |
| Lambdas and Generics | Which `java.util.function` type fits? What do `? extends` and `? super` allow? |
| Streams | Which collector groups and sorts? When is a loop clearer? |
| Optional and Exceptions | `orElse` or `orElseThrow`? Checked or unchecked? Did I keep the cause? |
| Packages, Maven and JUnit | What does `@BeforeEach` give each test? How do I test an exception? |

> [!TIP]
> The library's clock is a `Supplier<LocalDate>`, so the tests can move time. That is the
> same trick you should use whenever code depends on "now".
