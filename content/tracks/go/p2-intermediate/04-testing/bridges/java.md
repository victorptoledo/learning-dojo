---
from: java
status: draft
concept: "Unit tests, parameterised tests and benchmarks"
from_way: "JUnit 5 with @Test and assertions, @ParameterizedTest with sources, Mockito for mocks, AssertJ for fluent checks, JMH for benchmarks and Jazzer for fuzzing."
native_way: "The testing package with plain if checks, table-driven subtests, hand-written fakes over small interfaces, and benchmarks and fuzzing built into go test."
sensei_tip: "Resist importing an assertion library on day one. got/want messages written by hand are the Go norm, and they make failures read like sentences."
---

| JUnit / Java | Go |
| --- | --- |
| `@Test void discount()` | `func TestDiscount(t *testing.T)` |
| `assertEquals(900, d)` | `if got != 900 { t.Errorf("... = %d, want 900", got) }` |
| `@ParameterizedTest` + `@CsvSource` | a table of structs and `t.Run` |
| `@BeforeEach` / `@AfterEach` | setup at the top, `t.Cleanup(...)` |
| `@TempDir` | `t.TempDir()` |
| Mockito `mock(Repo.class)` | a small fake type that satisfies the interface |
| JMH `@Benchmark` | `func BenchmarkX(b *testing.B) { for b.Loop() { ... } }` |
| Jazzer | `func FuzzX(f *testing.F)` and `go test -fuzz` |
| JaCoCo | `go test -cover -coverprofile=c.out` |
