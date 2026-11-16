# Hexagonal, Onion and Clean Architecture

The layered style has one arrow pointing the wrong way: the business rules depend on the
database code. Three closely related styles fix that arrow. They differ in vocabulary and
pictures; they share one rule.

## The dependency rule

> Source code dependencies point **inwards**, towards the business rules. Nothing in an inner
> circle knows anything about an outer circle.
> (Robert C. Martin, *The Clean Architecture*, 2012)

The business rules are the centre because they are what the system is **for** and what changes
for business reasons. Frameworks, databases, message brokers and user interfaces are details at
the edge: important, but replaceable, and changing for technical reasons. The rule protects the
centre from them.

How can the domain save a loan without depending on the database? It **owns an interface**
that describes what it needs, in its own words (`Loans.add(loan)`), and the database code
**implements** it. The arrow from domain to database is inverted: dependency inversion applied
to a whole architecture.

## Hexagonal architecture (ports and adapters)

Alistair Cockburn, 2005. The application is a hexagon (the number of sides does not matter) that
talks to the outside only through **ports**:

- **Driving (primary) ports**: what the outside can ask the application to do. Use cases:
  `BorrowBook.borrow(member, isbn)`. **Driving adapters** call them: an HTTP controller, a CLI, a
  message consumer, a test.
- **Driven (secondary) ports**: what the application needs from the outside: `Books`,
  `Notifier`, `Clock`. **Driven adapters** implement them: a JDBC repository, an email client, a
  fake in a test.

Cockburn's original goal was to let the application be driven equally by users, programs,
automated tests or batch scripts, and to be developed and tested in isolation from its eventual
devices and databases. A test is just another adapter.

The Design Patterns track's lesson *Ports and Adapters and the Anti-Corruption Layer* builds this
at the scale of one use case in code; this lesson is about the system-wide rule.

## Onion and clean architecture

- **Onion** (Jeffrey Palermo, 2008) draws concentric rings: domain model at the centre, domain
  services around it, application services next, and UI, infrastructure and tests in the outer
  ring. Infrastructure is outside, not at the bottom.
- **Clean architecture** (Robert C. Martin, 2012) draws four circles: **entities** (enterprise
  rules), **use cases** (application rules), **interface adapters** (controllers, presenters,
  gateways) and **frameworks and drivers**. It adds explicit request and response models at the
  use case boundary.

All three put the domain in the middle, have the inner code own the interfaces and push
technology outward. Pick one vocabulary per codebase and stick to it.

## The composition root

If the domain only knows interfaces, someone must create the real adapters and plug them in.
That is the **composition root**: one place, at the outermost edge (`main`, a Spring
`@Configuration`, an `App.create`), that knows every adapter and wires the graph. It is the only
code allowed to depend on everything. Without it, adapters get created inside the core and the
rule is broken on the first day.

## What it buys

- **Tests of business rules without infrastructure**: fakes for the driven ports, milliseconds
  per test, no containers.
- **Replaceable technology**: a new driving adapter (a webhook next to the Kafka consumer) or a
  new driven one (a different database) leaves the domain untouched.
- **Deferred decisions**: you can build and test the domain before choosing the database, which
  is the last responsible moment applied to infrastructure.
- **A checkable rule**: "the domain package uses nothing outside itself and the JDK" is a fitness
  function, as the Fundamentals checkpoint kata shows.

## What it costs

- **More types**: an interface and an adapter per external dependency, and mapping between
  domain objects and persistence or transport objects.
- **Ceremony on thin domains**: for a CRUD service with no rules, a hexagon is layers with extra
  steps. If the use case is "save the form", say so and keep it simple.
- **Temptations**: a "domain" that is only JPA entities with annotations; ports named after
  technology (`KafkaPublisher`), which leaks the detail into the centre; one interface per class
  "for testability" with no second implementation in sight.

A pragmatic middle ground, common in practice: hexagonal for the parts with real business rules,
plain layers or simple CRUD for the rest, chosen per module and recorded in an ADR.

## Further reading

- Alistair Cockburn, "Hexagonal Architecture": <https://alistair.cockburn.us/hexagonal-architecture/>
- Jeffrey Palermo, "The Onion Architecture, part 1" (2008): <https://jeffreypalermo.com/2008/07/the-onion-architecture-part-1/>
- Robert C. Martin, "The Clean Architecture" (2012): <https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html>
- Robert C. Martin, *Clean Architecture* (Prentice Hall, 2017)
- Tom Hombergs, *Get Your Hands Dirty on Clean Architecture*, 2nd ed. (Packt, 2023)
- Herberto Graça, "DDD, Hexagonal, Onion, Clean, CQRS, … How I put it all together": <https://herbertograca.com/2017/11/16/explicit-architecture-01-ddd-hexagonal-onion-clean-cqrs-how-i-put-it-all-together/>
