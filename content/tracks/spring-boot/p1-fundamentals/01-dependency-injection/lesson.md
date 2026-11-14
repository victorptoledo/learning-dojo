# Beans and Dependency Injection

**Dependency injection** means an object does not create the things it needs; it receives
them. Spring's container does the receiving for you: it creates every bean once, works out
the order, and passes each one into the constructors that ask for it. The result is code
whose dependencies are visible in its constructor and easy to replace in tests.

## Declaring beans

### Stereotype annotations

Classes in the scanned packages annotated with a **stereotype** become beans:

| Annotation | Use for |
| --- | --- |
| `@Component` | any bean |
| `@Service` | business logic |
| `@Repository` | data access (also translates database exceptions) |
| `@RestController` / `@Controller` | web endpoints |
| `@Configuration` | a class that declares other beans with `@Bean` methods |

`@Service` and `@Repository` behave like `@Component`; the name tells readers what the class
is for.

### @Bean methods

For classes you do not own (a library's client, a `Clock`), or when creation needs logic,
write a `@Bean` method in a `@Configuration` class. The method's return value is the bean:

```java
@Configuration
class TimeConfig {
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    PriceCalculator priceCalculator(Clock clock, TaxRates rates) {   // parameters are injected too
        return new PriceCalculator(clock, rates);
    }
}
```

## Injecting: use the constructor

```java
@Service
public class CheckoutService {
    private final PriceCalculator prices;
    private final OrderRepository orders;

    public CheckoutService(PriceCalculator prices, OrderRepository orders) {
        this.prices = prices;
        this.orders = orders;
    }
}
```

With a single constructor, Spring uses it without any annotation. **Constructor injection**
is the recommended style:

- the fields can be `final`, so the object is complete as soon as it exists;
- the dependencies are visible in one place, and a long parameter list tells you the class
  does too much;
- a test builds the class with plain `new CheckoutService(fakePrices, fakeOrders)`, no Spring.

Field injection (`@Autowired private OrderRepository orders;`) still works, but hides the
dependencies and needs reflection or Spring to set them. In tests you will still see
`@Autowired` on test fields, which is fine there.

## When there are several candidates

If two beans have the type a constructor asks for, Spring refuses to guess and fails at
startup with `NoUniqueBeanDefinitionException`. The ways out:

```java
@Component("eur")
class EuroFormatter implements MoneyFormatter { ... }

@Component("usd")
@Primary                                   // the default when nothing else is said
class DollarFormatter implements MoneyFormatter { ... }

@Service
class InvoiceService {
    InvoiceService(@Qualifier("eur") MoneyFormatter formatter) { ... }   // pick one by name
}

@Service
class ReportService {
    ReportService(List<MoneyFormatter> formatters) { ... }               // or take all of them
}
```

Asking for a `List<T>` or a `Map<String, T>` (bean name to bean) is a clean way to implement
the strategy pattern: every implementation registers itself just by being a bean.

`Optional<T>` or `ObjectProvider<T>` parameters accept a bean that may not exist.

## Scopes and state

By default a bean is a **singleton**: one instance for the whole application, shared by every
request and every thread. Consequences:

- **Beans must be stateless**, or hold only thread-safe state. A field that stores "the
  current order" in a singleton service is a bug: two requests overwrite each other.
- Keep per-request data in local variables and method parameters, or in the objects you pass.

Other scopes exist (`prototype` creates a new instance per injection; `request` and `session`
in web apps), but you rarely need them.

## Lifecycle hooks

```java
@Component
class CacheWarmer {
    @PostConstruct
    void warmUp() { ... }      // after the bean is created and injected

    @PreDestroy
    void flush() { ... }       // when the context shuts down
}
```

To run something once the whole application is ready, implement `ApplicationRunner` or listen
to `ApplicationReadyEvent`.

## Circular dependencies

If `A` needs `B` and `B` needs `A`, constructor injection cannot build either: Spring Boot
fails at startup with a message showing the cycle. That is a design signal, not a wiring
problem: extract the shared part into a third bean, or let one side publish an event.

## Summary

- Beans come from stereotype annotations (`@Service`, `@Component`, ...) found by scanning,
  and from `@Bean` methods in `@Configuration` classes.
- Inject through the constructor; fields `final`; no Spring needed to test the class.
- Resolve several candidates with `@Primary`, `@Qualifier`, or by injecting a `List`.
- Singletons are shared: keep beans stateless.
- A circular dependency is a design problem to fix, not to hide.
