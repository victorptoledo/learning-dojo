# Adapter, Decorator, Proxy and Facade

Four structural patterns that all **wrap** an object. Their class diagrams look alike; their
intentions do not. Name the intention and the right one follows.

| Pattern | Wraps to ... | Interface seen by the caller |
| --- | --- | --- |
| **Adapter** | translate an interface you cannot change into the one you need | different from the wrapped object's |
| **Decorator** | add behaviour (logging, retry, caching) without changing the object | the same as the wrapped object's |
| **Proxy** | control access to the object (lazy loading, security, transactions, remoting) | the same as the wrapped object's |
| **Facade** | give one simple entry point to a whole subsystem | a new, smaller one |

## Adapter

**Problem.** You need an interface, and the thing that can do the work has a different one:
a 2011 SDK with string amounts and status codes, a library that throws checked exceptions, a
legacy class you cannot edit.

**Solution.** Define the interface **in your own terms** (a port), and write one class that
implements it by calling the foreign API and translating types, units and errors.

```java
interface PaymentGateway {
    Receipt charge(Payment payment);
}

final class LegacyPayAdapter implements PaymentGateway {
    private final LegacyPayClient client;
    LegacyPayAdapter(LegacyPayClient client) { this.client = client; }

    public Receipt charge(Payment payment) {
        int status = client.submit(payment.account(), "%d.%02d".formatted(payment.cents() / 100, payment.cents() % 100), payment.currency());
        return switch (status) {
            case LegacyPayClient.OK -> new Receipt(client.lastReference(), payment.cents());
            case LegacyPayClient.DECLINED -> throw new PaymentDeclinedException(payment.account());
            default -> throw new GatewayUnavailableException("status " + status);
        };
    }
}
```

Only the adapter knows the SDK. Replacing the provider means writing another adapter, and
tests use a lambda as the gateway. JDK examples: `InputStreamReader` adapts bytes to
characters, `Arrays.asList` adapts an array to a `List`, `Collections.enumeration` adapts a
collection to the old `Enumeration`. In Spring, `HandlerAdapter` lets the dispatcher call
handlers of different shapes.

## Decorator

**Problem.** You want to add behaviour (retries, metrics, caching, logging) to some instances,
in different combinations, without subclassing for every combination.

**Solution.** A class that implements the **same** interface, holds another instance of it,
does its extra work and delegates.

```java
final class RetryingGateway implements PaymentGateway {
    private final PaymentGateway inner;
    private final int attempts;
    // constructor ...
    public Receipt charge(Payment payment) {
        for (int i = 1; ; i++) {
            try {
                return inner.charge(payment);
            } catch (GatewayUnavailableException e) {
                if (i == attempts) throw e;
            }
        }
    }
}

var gateway = new MeteredGateway(new RetryingGateway(new LegacyPayAdapter(client), 3));
```

Decorators stack, and **order matters**: a meter outside the retries counts payments; inside
it counts attempts. A cache outside a retry avoids retrying cached answers. The classic JDK
example is `java.io`: `new BufferedInputStream(new GZIPInputStream(new FileInputStream(f)))`.
`Collections.synchronizedList` and `unmodifiableList` decorate any list.

With one-method interfaces, a decorator can be a higher-order function:
`static PaymentGateway retrying(PaymentGateway g, int n) { return p -> ...; }`.

## Proxy

**Problem.** Access to an object needs control: create it only when used, check permissions,
open a transaction around each call, or call it over the network as if it were local.

**Solution.** Same structure as a decorator; the intention is control, and the proxy is
often **generated**. The JDK can create one at runtime for any interface
(`java.lang.reflect.Proxy`), and libraries generate subclasses for classes (ByteBuddy, CGLIB).

This is how Spring works. A bean with `@Transactional`, `@Cacheable`, `@Async` or a security
annotation is wrapped in a proxy; the caller gets the proxy, which starts the transaction,
calls your method and commits or rolls back. Two consequences every Spring developer meets:

- **Self-invocation bypasses the proxy.** `this.save()` inside the bean calls the real
  object, so the annotation on `save` does nothing. Call through another bean.
- Proxies of classes need non-final classes and methods; that is why Spring Boot enables
  class-based proxies by default and Kotlin needs the `all-open` plugin.

JPA uses proxies too: a lazy `@ManyToOne` is a proxy that loads the entity on first access
(and throws `LazyInitializationException` when the session is gone).

## Facade

**Problem.** A subsystem has many classes and a lot of ceremony; most callers want two or
three operations.

**Solution.** One class with a small, task-shaped interface that coordinates the subsystem.
The subsystem stays available for the callers who need its full power.

`JdbcTemplate` is a facade over JDBC's connections, statements, result sets and exceptions.
`RestClient` is a facade over HTTP connections and message converters. `java.net.http.HttpClient`
with `send` is one over the whole HTTP stack. A good facade does not add logic; it removes
ceremony. A facade that starts making business decisions is turning into a god class.

## Telling them apart

Ask what the wrapper is **for**:

- different interface, same job → adapter;
- same interface, extra job → decorator;
- same interface, controls whether and how the call happens → proxy;
- new, smaller interface over many objects → facade.

## Further reading

- Refactoring.Guru, "Adapter": <https://refactoring.guru/design-patterns/adapter>
- Refactoring.Guru, "Decorator": <https://refactoring.guru/design-patterns/decorator>
- Refactoring.Guru, "Proxy": <https://refactoring.guru/design-patterns/proxy>
- Refactoring.Guru, "Facade": <https://refactoring.guru/design-patterns/facade>
- Spring Framework reference, "Understanding AOP Proxies" (self-invocation): <https://docs.spring.io/spring-framework/reference/core/aop/proxying.html>
- Java SE 25 API, `java.lang.reflect.Proxy`: <https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/lang/reflect/Proxy.html>
- Java SE 25 API, `FilterInputStream` (the base of the java.io decorators): <https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/io/FilterInputStream.html>
