# Dependency Injection and Inversion of Control

**Inversion of control (IoC)** means your code does not drive everything: something else
calls it (a framework calls your controller, a container creates your objects). **Dependency
injection (DI)** is one form of IoC: an object does not create or look up its collaborators;
it **receives** them. It is the practical tool behind dependency inversion (the D of SOLID).

## By hand first

```java
public final class TransferService {
    private final Accounts accounts;
    private final ExchangeRates rates;
    private final Clock clock;

    public TransferService(Accounts accounts, ExchangeRates rates, Clock clock) {
        this.accounts = accounts;
        this.rates = rates;
        this.clock = clock;
    }
}

// The composition root: the one place that knows the concrete classes.
public static void main(String[] args) {
    var bank = new JdbcAccounts(dataSource());
    var service = new TransferService(bank, new LegacyFxRates(new LegacyFxFeed()), Clock.systemUTC());
    new HttpServer(new TransferController(service)).start();
}
```

That is all DI is. A test writes `new TransferService(new InMemoryAccounts(), (a, b) ->
BigDecimal.ONE, Clock.fixed(...))` and runs in milliseconds. The **composition root** is the
only place that names concrete classes; everything else depends on what it receives.

## Constructor injection

Prefer constructors over setters and fields:

- dependencies are **explicit** in the signature and **final**;
- an object cannot exist half-wired;
- tests need no framework, only `new`;
- a constructor with eight parameters is a visible smell (too many responsibilities), where
  eight `@Autowired` fields hide it.

Spring injects through the single constructor without any annotation. **Field injection**
(`@Autowired private Foo foo;`) needs reflection to test, allows cycles, and hides the class's
real dependencies. Setter injection is for genuinely optional dependencies.

## Anti-patterns

| Anti-pattern | Why it hurts |
| --- | --- |
| **Service locator** (`context.getBean(Foo.class)`, a static `Registry.get(...)`) | dependencies are invisible from outside; tests must prepare the global locator; failures move to runtime |
| **Static access** to collaborators (`Clock` via `LocalDate.now()`, a static `FeatureFlags`) | global state; tests cannot control it |
| **`new` of services inside services** | the caller is welded to one implementation |
| **Injecting the container** (`ApplicationContext`) into business code | a service locator in disguise |
| **Everything an interface** | DI does not require interfaces; add one at real boundaries |

When something must be created later or many times, inject a **factory** (`Supplier<T>`,
Spring's `ObjectProvider<T>`) rather than the container.

## What the Spring container does

Spring is a big **factory** (`BeanFactory`, `ApplicationContext`) driven by configuration:

- `@Component` classes and `@Bean` methods are recipes (`@Bean` methods are factory methods);
  `@Configuration` classes group families of beans (abstract factory).
- **Scopes**: singleton (default, one per container), prototype (a new one per injection),
  request and session (web). Injecting a shorter-lived bean into a singleton needs a scoped
  proxy or an `ObjectProvider`.
- **Several implementations** of one interface: inject `List<T>` or `Map<String, T>` (a
  registry), mark one `@Primary`, or pick with `@Qualifier`.
- **Proxies**: for `@Transactional`, `@Cacheable`, `@Async`, the container injects a proxy
  around your bean (phase 1). That is why self-invocation skips the annotation.
- **Lifecycle hooks** (`BeanPostProcessor`, `@PostConstruct`) are how Spring itself decorates
  beans.

Spring Boot adds auto-configuration: `@Bean` methods guarded by `@ConditionalOnClass` and
`@ConditionalOnMissingBean`, so your own bean replaces the default one: open/closed at the
framework level.

## Keeping the domain framework-free

DI makes it possible to keep business code free of framework types: the domain declares
interfaces, adapters implement them, and only the composition root (Spring configuration or
`main`) knows both. The hexagonal kata applies this: `dojo.domain` must not mention the
adapters, and `App` is the composition root.

## Further reading

- Martin Fowler, "Inversion of Control Containers and the Dependency Injection pattern": <https://martinfowler.com/articles/injection.html>
- Spring Framework reference, "The IoC Container" overview: <https://docs.spring.io/spring-framework/reference/core/beans.html>
- Spring Framework reference, "Dependency Injection": <https://docs.spring.io/spring-framework/reference/core/beans/dependencies/factory-collaborators.html>
- Spring Framework reference, "Bean Scopes": <https://docs.spring.io/spring-framework/reference/core/beans/factory-scopes.html>
- roadmap.sh topic "Hollywood Principle" (don't call us, we'll call you): <https://github.com/nilbuild/developer-roadmap/blob/master/roadmaps/software-design-architecture/content/hollywood-principle@WzUhKlmFB9alTlAyV-MWJ.md>
- Microsoft Learn, "Dependency injection in .NET" (the same ideas outside Java): <https://learn.microsoft.com/en-us/dotnet/core/extensions/dependency-injection>
