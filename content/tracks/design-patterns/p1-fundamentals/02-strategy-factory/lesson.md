# Strategy and Factory: Removing Giant if/else Chains

Two of the most used patterns, and the most common refactoring in business code: a method
that branches on a type code (`if (method.equals("express")) ... else if ...`) becomes a set
of interchangeable algorithms, and the decision of **which** one to use moves to one place.

## Strategy

**Problem.** An algorithm varies (shipping cost by carrier, tax by country, sort order by
column), the variants keep growing, and the code that uses the algorithm should not change
when a variant is added.

**Solution.** Define one interface for the algorithm; each variant implements it; the
context holds a reference to one strategy and calls it.

```java
@FunctionalInterface
interface ShippingRule {
    long quoteCents(Parcel parcel);
}

final class Checkout {
    private final ShippingRule rule;
    Checkout(ShippingRule rule) { this.rule = rule; }
    long total(Cart cart) { return cart.cents() + rule.quoteCents(cart.parcel()); }
}
```

**Consequences.** Each variant is testable alone; a new variant is new code, not an edit
(open/closed); the caller chooses at runtime. The cost: more types, and someone has to choose.

### Strategies as lambdas

When the strategy has **one method and no state**, it is a function. Make the interface a
`@FunctionalInterface` and a lambda or method reference is a strategy:

```java
ShippingRule flat = parcel -> 499;
ShippingRule byWeight = parcel -> 199 + parcel.grams() / 100;
ShippingRule tested = Rules::express;       // a static method
```

Keep a named class when the strategy has state, several methods, its own configuration, or
is big enough to deserve its own file and tests. Classes and lambdas mix freely behind the
same interface.

The JDK is full of strategies: `Comparator` for `sort`, `Collector` for `collect`,
`RejectedExecutionHandler` for thread pools, `java.util.function` everywhere.

## Factory

Strategy answers "how do I make the algorithm replaceable?". Factories answer "who creates the
right object?", so callers do not need to know concrete classes.

| Variant | What it is | Example |
| --- | --- | --- |
| **Static factory method** | a static method instead of a constructor: it has a name, can cache, can return a subtype | `List.of`, `Optional.empty`, `Integer.valueOf`, `LocalDate.of` |
| **Simple factory** | one method that picks a class from a parameter | `ShippingRules.forName("express")` |
| **Factory method (GoF)** | a method subclasses override to choose the product | `Iterable.iterator()`, `Collection.stream()` |
| **Registry** | a map from a key to a strategy or a supplier, filled at startup | a `Map<String, ShippingRule>`, a Spring map of beans |

The simple factory moves the `switch` into one place; the **registry** removes it:

```java
final class ShippingCalculator {
    private final Map<String, ShippingRule> rules;

    ShippingCalculator(Map<String, ShippingRule> rules) { this.rules = Map.copyOf(rules); }

    long quoteCents(String method, Parcel parcel) {
        ShippingRule rule = rules.get(method);
        if (rule == null) throw new IllegalArgumentException("unknown shipping method: " + method);
        return rule.quoteCents(parcel);
    }
}
```

A new method is one new entry. Tests build the calculator with the two rules they need.

### Enum strategies

When the set of variants is closed and known at compile time, an `enum` whose constants carry
their behaviour is a compact strategy and factory in one: `Carrier.valueOf("EXPRESS").quote(p)`,
with exhaustive `switch` support. Use a registry when variants come from configuration,
plugins or other modules.

## Strategy and factory in Spring

Spring can build the registry for you. Every bean implementing an interface can be injected
as a list, or as a map keyed by bean name:

```java
@Component("express") class ExpressRule implements ShippingRule { ... }
@Component("standard") class StandardRule implements ShippingRule { ... }

@Service
class ShippingCalculator {
    ShippingCalculator(Map<String, ShippingRule> rules) { ... }   // {"express": ..., "standard": ...}
}
```

Spring itself is built from strategies: `PasswordEncoder`, `HttpMessageConverter`,
`TaskExecutor`, `ResourceLoader`; and from factories: `BeanFactory`, `FactoryBean`, `@Bean`
methods (factory methods the container calls).

## When not to

- Two branches that will never grow: an `if` is clearer than an interface.
- A single strategy chosen once: inject the concrete class, add the interface when the
  second variant arrives.
- A `switch` over a **sealed** type where each case is one expression and the cases change
  together: the compiler-checked switch may be better than scattering logic over classes.
  Phase 2 discusses this trade-off (operations vs types).

## Further reading

- Refactoring.Guru, "Strategy": <https://refactoring.guru/design-patterns/strategy>
- Refactoring.Guru, "Factory Method": <https://refactoring.guru/design-patterns/factory-method>
- Refactoring catalogue, "Replace Conditional with Polymorphism": <https://refactoring.com/catalog/replaceConditionalWithPolymorphism.html>
- roadmap.sh topic "Encapsulate What Varies": <https://github.com/nilbuild/developer-roadmap/blob/master/roadmaps/software-design-architecture/content/encapsulate-what-varies@DlefJ9JuJ1LdQYC4WSx6y.md>
- Java SE 25 API, package `java.util.function`: <https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/function/package-summary.html>
- Java SE 25 API, `Comparator`: <https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/Comparator.html>
- Spring Framework reference, "Dependencies" (injecting collections and maps of beans): <https://docs.spring.io/spring-framework/reference/core/beans/dependencies/factory-collaborators.html>
