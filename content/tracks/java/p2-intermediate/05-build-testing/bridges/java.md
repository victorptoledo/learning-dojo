---
from: java
status: draft
concept: "From JUnit 4 to JUnit 5"
from_way: "JUnit 4 with public test classes, @Before, @Test(expected = ...) and @RunWith(Parameterized.class); maven.compiler.source and target 1.8."
native_way: "JUnit 5 (Jupiter) with package-private tests, @BeforeEach, assertThrows returning the exception, and @ParameterizedTest; maven.compiler.release 25."
sensei_tip: "`@Test(expected = X.class)` passes if any line throws X; `assertThrows` pins the exact call and lets you check the message."
---

| JUnit 4 / Java 8 | JUnit 5 / Java 25 |
| --- | --- |
| `public class FooTest`, `public void test...()` | `class FooTest`, `void ...()` |
| `@Before`, `@After`, `@BeforeClass` | `@BeforeEach`, `@AfterEach`, `@BeforeAll` |
| `@Test(expected = IllegalArgumentException.class)` | `assertThrows(IllegalArgumentException.class, () -> ...)` |
| `@RunWith(Parameterized.class)` | `@ParameterizedTest` + `@CsvSource` |
| `@Ignore` | `@Disabled("reason")` |
| `<source>1.8</source><target>1.8</target>` | `<maven.compiler.release>25</maven.compiler.release>` |
