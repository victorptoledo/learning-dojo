# Ports and Adapters and the Anti-Corruption Layer

Alistair Cockburn's **ports and adapters** (also called *hexagonal architecture*) puts the
application's logic in the middle and everything technical around it. This lesson stays at the
scale of **one service and its use cases**, as a design pattern; how to organise whole systems
of services is the subject of the Software Architecture track.

## Ports

A **port** is an interface owned by the application, named and typed in its own terms.

| Kind | Direction | Examples |
| --- | --- | --- |
| **Driving** (primary, inbound) | the outside calls the application | `TransferMoney` use case interface, or the application service's public methods |
| **Driven** (secondary, outbound) | the application calls the outside | `Accounts` (persistence), `ExchangeRates` (a rate feed), `TransferLog` (notification), `Transactions` (unit of work), `Clock` |

## Adapters

An **adapter** connects a port to a technology:

- driving adapters **call** the application: a REST controller, a message listener, a CLI, a
  test;
- driven adapters **implement** driven ports: a JPA repository, an HTTP client, an in-memory
  fake, a console logger.

```text
 REST controller ─┐                          ┌─ JpaAccounts       (Accounts)
 Kafka listener ──┼──▶ TransferService ──▶ ports ─┼─ LegacyFxRates   (ExchangeRates)
 JUnit test ──────┘     (domain + use case)  └─ ConsoleTransferLog (TransferLog)
```

## The dependency rule

**Source code dependencies point inward.** The domain and the use cases know only their
ports. Adapters know the ports and their technology. One **composition root** (Spring
configuration or `main`) knows everything and wires it.

You can check the rule mechanically:

- **packages**: `…domain` must not import `…adapters` or any framework or vendor package;
- **modules**: put the domain in its own Maven or Gradle module (or JPMS module) that does not
  depend on the adapters at all, so a violation does not compile;
- **tests**: ArchUnit rules such as `noClasses().that().resideInAPackage("..domain..")
  .should().dependOnClassesThat().resideInAPackage("..adapters..")`. The hexagonal kata checks
  the same rule by reading your sources.

The payoff: the domain is tested with fakes in milliseconds; a database or provider change is
a new adapter; and the code of the business rule reads like the business rule.

## The anti-corruption layer

When you integrate with a system whose model is different, old or simply not yours (a legacy
mainframe, a vendor API, another team's service), its concepts tend to leak into your code:
their status codes, their strings, their names. Eric Evans calls the defence an
**anti-corruption layer (ACL)**: an adapter whose main job is **translation** between the two
models, in both directions.

What an ACL translates:

| Foreign | Domain |
| --- | --- |
| `"EURUSD=1.0842"` strings and `"N/A"` | `BigDecimal` rates and a `RateUnavailableException` |
| status codes in a 200 response | domain exceptions or result types |
| their identifiers and enums | your value objects |
| missing data, defaults and quirks (a pair known only one way) | complete domain values |

An ACL can be one adapter class, or a facade plus adapters plus mappers for a large legacy
system. Its tests use recorded responses from the foreign system. It is the adapter and facade
patterns of phase 1, applied with intent.

## Ports and adapters in Spring

- Driving adapters: `@RestController`, `@KafkaListener`, `@Scheduled` methods.
- Driven adapters: Spring Data repositories behind a domain interface, `RestClient`-based
  clients, `JavaMailSender` behind a `Notifications` port.
- Composition root: `@Configuration` classes; keep `@Transactional` at the application
  service, or behind a `Transactions` port if the domain must stay framework-free.
- The domain package can have zero Spring imports. That is a choice, not a law: many teams
  accept `@Transactional` and `@Service` in application services and keep only the core domain
  clean.

## Costs and limits

More interfaces and mapping code; a CRUD screen over one table does not need it. Apply it where
the domain logic is worth protecting and the integrations are many or unstable.

## Further reading

- Alistair Cockburn, "Hexagonal architecture": <https://alistair.cockburn.us/hexagonal-architecture/>
- Microsoft Azure Architecture Center, "Anti-corruption Layer pattern": <https://learn.microsoft.com/en-us/azure/architecture/patterns/anti-corruption-layer>
- Domain Language, "DDD Reference" (anti-corruption layer and the other strategic patterns): <https://www.domainlanguage.com/ddd/reference/>
- roadmap.sh topic "Keep Framework Code Distant": <https://github.com/nilbuild/developer-roadmap/blob/master/roadmaps/software-design-architecture/content/keep-framework-code-distant@OoCCy-3W5y7bUcKz_iyBw.md>
- roadmap.sh topic "Policy vs Detail": <https://github.com/nilbuild/developer-roadmap/blob/master/roadmaps/software-design-architecture/content/policy-vs-detail@b_PvjjL2ZpEKETa5_bd0v.md>
- Refactoring.Guru, "Adapter": <https://refactoring.guru/design-patterns/adapter>
