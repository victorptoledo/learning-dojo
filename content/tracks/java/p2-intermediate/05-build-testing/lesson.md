# Packages, Maven and JUnit 5

Code that works on your machine is half the job. The other half is code that others can
**build** with one command and **trust** because it is tested. In Java that means packages
and visibility, a build tool (Maven here, Gradle is the other common one) and JUnit 5.

## Packages and visibility

A package groups related classes and is the first line of encapsulation:

```java
package shop.pricing;     // lives in src/main/java/shop/pricing/

public final class Pricing { ... }     // usable from anywhere
final class DiscountRules { ... }      // package-private: only shop.pricing sees it
```

| Modifier | Visible in |
| --- | --- |
| `public` | everywhere |
| `protected` | the package and subclasses |
| *(none)* package-private | the package only |
| `private` | the class only (and its nested classes) |

Package-private is underused. Making helpers package-private keeps your public surface small,
and tests in the **same package** (under `src/test/java/shop/pricing/`) can still reach them.

Organise packages **by feature** (`shop.pricing`, `shop.orders`) rather than by layer
(`shop.controllers`, `shop.services`): a feature package can keep most of its classes
package-private.

### Modules, in one paragraph

Since Java 9 a `module-info.java` can declare which packages a JAR **exports** and which
modules it **requires**. The JDK itself is modular (`java.base`, `java.sql`, ...), which is
what makes small runtimes with `jlink` possible. Most applications still run on the classpath
without a module descriptor; libraries benefit most from it. Java 25 also lets a source file
`import module java.base;` to import every exported package of a module at once.

## Maven

Maven describes a project in `pom.xml` and builds it by convention:

```xml
<project>
    <modelVersion>4.0.0</modelVersion>
    <groupId>shop</groupId>
    <artifactId>pricing</artifactId>
    <version>1.0</version>

    <properties>
        <maven.compiler.release>25</maven.compiler.release>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
            <version>5.13.4</version>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
```

- **Coordinates** `groupId:artifactId:version` identify every library. Maven downloads them
  from Maven Central into `~/.m2/repository`.
- `<scope>test</scope>`: the dependency is on the classpath only for tests.
- `maven.compiler.release` sets the Java version the code is compiled for.
- The layout is fixed: `src/main/java`, `src/main/resources`, `src/test/java`; output goes
  to `target/`.

The commands you will use:

| Command | Does |
| --- | --- |
| `mvn compile` | compiles `src/main/java` |
| `mvn test` | compiles and runs every test (Surefire plugin) |
| `mvn test -Dtest=PricingTest` | runs one test class |
| `mvn package` | tests, then builds the JAR in `target/` |
| `mvn dependency:tree` | shows which library pulled in which |

Each command runs every **lifecycle phase** before it: `package` implies `compile` and
`test`. Many projects ship the **Maven Wrapper** (`mvnw`), which downloads the right Maven
version for you.

## JUnit 5

```java
package shop.pricing;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.*;

class PricingTest {
    private Pricing pricing;

    @BeforeEach
    void setUp() {
        pricing = new Pricing(TaxRate.of(21));
    }

    @Test
    void adds_tax_to_the_net_price() {
        assertEquals(1210, pricing.gross(1000));
    }

    @Test
    void rejects_negative_prices() {
        var error = assertThrows(IllegalArgumentException.class, () -> pricing.gross(-1));
        assertEquals("price must not be negative: -1", error.getMessage());
    }
}
```

| Annotation / method | Use |
| --- | --- |
| `@Test` | a test method (package-private is fine) |
| `@BeforeEach` / `@AfterEach` | run around every test: each test gets a fresh object |
| `@BeforeAll` / `@AfterAll` | once per class (`static` methods) |
| `@DisplayName("...")` | a readable name in reports |
| `@Disabled("reason")` | skip, with the reason in the report |
| `assertEquals(expected, actual)` | equality; with a delta for doubles |
| `assertTrue`, `assertFalse`, `assertNull`, `assertNotNull` | conditions |
| `assertThrows(Type.class, () -> ...)` | the code throws that type; returns the exception |
| `assertAll(() -> ..., () -> ...)` | report every failed assertion, not only the first |
| `assertIterableEquals`, `assertArrayEquals` | collections and arrays |

### Parameterized tests

One test, many inputs:

```java
@ParameterizedTest
@CsvSource({
    "0,    0",
    "1000, 1210",
    "99,   119",
})
void gross_price(long net, long expected) {
    assertEquals(expected, pricing.gross(net));
}

@ParameterizedTest
@ValueSource(strings = {"", " ", "\t"})
void blank_names_are_rejected(String name) {
    assertThrows(IllegalArgumentException.class, () -> new Customer(name));
}
```

`@MethodSource("cases")` reads arguments from a `static Stream<Arguments> cases()` method
when the inputs are objects.

### What makes a good test

- **One behaviour per test**, and a name that says it: `rejects_negative_prices`.
- **Arrange, act, assert**: build the inputs, call once, check the result.
- Test the **public behaviour**, not private details, so refactoring does not break tests.
- Cover the **edges**: empty, zero, one, many, the boundary values, the error paths.
- Tests must be **independent** and **repeatable**: no order between them, no real clock,
  network or random values without control (pass a `Clock`, a seed, a fake).

## Summary

- Packages group classes; package-private is the default you should reach for.
- Maven: coordinates, scopes, the standard layout and `mvn test` / `mvn package`.
- JUnit 5: `@Test`, `@BeforeEach`, `assertEquals(expected, actual)`, `assertThrows`, and
  `@ParameterizedTest` with `@CsvSource` or `@ValueSource`.
- Good tests check one behaviour, follow arrange-act-assert and do not depend on each other.
