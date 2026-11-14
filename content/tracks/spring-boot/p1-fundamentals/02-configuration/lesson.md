# Configuration Properties and Profiles

The same JAR should run on your laptop, in the test environment and in production. What
changes between them (URLs, limits, feature switches, credentials) belongs in
**configuration**, read when the application starts. Spring Boot gives you one model for it,
from a properties file up to environment variables, and typed objects to read it safely.

## application.properties or application.yaml

```properties
# src/main/resources/application.properties
server.port=8080
shop.currency=EUR
shop.free-shipping-from=50.00
shop.max-items-per-order=20
```

The same in YAML:

```yaml
server:
  port: 8080
shop:
  currency: EUR
  free-shipping-from: 50.00
  max-items-per-order: 20
```

Pick one format per project. Boot's own settings (`server.port`, `spring.datasource.url`,
`logging.level.*`, ...) are documented in the *Common Application Properties* appendix, and
the Spring Boot extension completes them as you type.

## Typed configuration: @ConfigurationProperties

Reading settings one by one with `@Value("${shop.currency}")` spreads string keys all over
the code. Group them in a **record** instead:

```java
@ConfigurationProperties("shop")
@Validated
public record ShopProperties(
        @NotBlank String currency,
        @DecimalMin("0") BigDecimal freeShippingFrom,
        @Min(1) @Max(100) int maxItemsPerOrder,
        @DefaultValue("PT30M") Duration cartTimeout) {
}
```

Register it once, usually on the application class:

```java
@SpringBootApplication
@ConfigurationPropertiesScan          // finds every @ConfigurationProperties record
public class ShopApplication { ... }
```

and inject it like any bean:

```java
@Service
class ShippingService {
    private final ShopProperties shop;

    ShippingService(ShopProperties shop) { this.shop = shop; }

    boolean isFree(BigDecimal subtotal) {
        return subtotal.compareTo(shop.freeShippingFrom()) >= 0;
    }
}
```

What you get:

- **Relaxed binding**: `max-items-per-order`, `maxItemsPerOrder` and the environment variable
  `SHOP_MAXITEMSPERORDER` all map to `maxItemsPerOrder`.
- **Type conversion**: `Duration` from `30m` or `PT30M`, `DataSize` from `10MB`, `URI`,
  enums, lists (`shop.regions=eu,us`) and maps.
- **Validation at startup** with `@Validated` and Bean Validation annotations: a bad value
  stops the application with a message naming the property. Fail fast, not at the first
  request.
- Defaults with `@DefaultValue`, or in the properties file.

## Where values come from, and who wins

Boot reads many **property sources**. A later (higher) source overrides an earlier one:

1. Defaults in your code (`@DefaultValue`)
2. `application.properties` / `.yaml` inside the JAR
3. Profile-specific files inside the JAR (`application-prod.yaml`)
4. The same files **outside** the JAR (next to it, or in `./config/`)
5. **Environment variables** (`SHOP_CURRENCY=USD`)
6. Java system properties (`-Dshop.currency=USD`)
7. **Command-line arguments** (`--shop.currency=USD`)

In containers and the cloud, environment variables are the usual way to set production
values. Tests can override properties too, with `@SpringBootTest(properties = "shop.currency=USD")`
or `@TestPropertySource`.

## Profiles

A **profile** is a named set of configuration that is active only sometimes:

```yaml
# application.yaml: defaults for everyone
shop:
  currency: EUR
---
spring:
  config:
    activate:
      on-profile: dev
shop:
  max-items-per-order: 1000
```

or a separate `application-dev.yaml`. Activate profiles with
`spring.profiles.active=dev` (property, environment variable `SPRING_PROFILES_ACTIVE=dev`, or
`--spring.profiles.active=dev`). Beans can be conditional on a profile too:

```java
@Bean
@Profile("dev")
Mailer fakeMailer() { return new LoggingMailer(); }
```

> [!TIP]
> Keep profiles few and about **environments or modes** (`dev`, `test`), not about features.
> For a feature switch, use a property and `@ConditionalOnProperty("shop.recommendations.enabled")`.

## Secrets

Passwords, tokens and keys never go into `application.properties` in the repository. Provide
them through environment variables or a secret manager (Vault, a cloud secret store, a
Kubernetes secret mounted as a file and read with `spring.config.import=configtree:/run/secrets/`).

## Summary

- Settings live in `application.properties` or `.yaml`, outside the code.
- Group them in a validated `@ConfigurationProperties` record and inject it; avoid scattered
  `@Value` strings.
- Later sources win: environment variables and command-line arguments override the files.
- Profiles switch configuration and beans per environment; secrets come from the environment.
