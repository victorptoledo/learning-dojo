# Layered Architecture

Layers are the oldest architectural style still in daily use, and the one most applications
start with, often without anyone deciding it.

## The layered style

The code is split into horizontal **layers**, each with one kind of responsibility, and each
layer uses only the layers below it:

```text
presentation   controllers, views, API endpoints
application    use cases: coordinate a request, transactions, security
domain         business rules
persistence    data access
database
```

Variants exist (three layers, four, with or without a separate application layer), but the
rule is the same: dependencies point **down**, never up. A layer knows nothing about the layers
above it, so you can, in principle, put a new user interface on the same business layer.

### Closed and open layers

- A **closed** (strict) layer must be passed through: presentation calls application, never
  persistence directly. Each layer isolates the ones above from changes below.
- An **open** (relaxed) layer may be skipped: a shared services layer anyone may call.

Open layers save code and cost isolation. Decide which layers are open on purpose, write it
down (an ADR), and check it with a fitness function, because a relaxed rule nobody wrote down
erodes into "anything calls anything".

### What layers are good at

- **Separation of technical concerns**: HTTP details stay in presentation, SQL in persistence.
- **A familiar structure**: every developer knows where a controller goes.
- **Testability of the middle**: business rules can be tested without the web layer.

## Where layers hurt

### The sinkhole

The **architecture sinkhole** anti-pattern: requests pass through every layer with no logic
added on the way. A controller calls a service that calls a repository that runs a query, and
the service only forwards. Some of that is normal (Richards and Ford suggest an 80/20 rule:
worry when most requests are sinkholes). When most of them are, the layers cost files and
indirection and buy nothing; open the layer or reconsider the style.

### Change cuts across layers

Business changes usually concern one **feature** ("members with unpaid invoices may not book"),
and a layered structure spreads every feature across every layer. One change touches a
controller, a service, a repository and an entity, in four packages, next to thirty unrelated
classes each. Layers optimise for technical change ("replace the database") and pessimise
business change, which is far more frequent.

### Layers do not stop the domain depending on technology

"Domain above persistence" means the domain **depends on** persistence: business rules call the
repository classes, and JPA or SQL types leak upward. The hexagonal and clean styles in the next
lessons invert that one arrow.

### The layer that is everything

A `services` package of 60 classes with no internal structure is not a layer, it is a heap. And
because every class is `public` (it must be, to be called from another package), nothing is
hidden.

## Package by layer, package by feature

**Package by layer** groups code by technical kind:

```text
controllers/  MemberController  BookingController  PaymentController
services/     MemberService     BookingService     PaymentService
repositories/ MemberRepository  BookingRepository  PaymentRepository
```

**Package by feature** groups it by what it does for the business:

```text
members/   MemberController  MemberService  MemberRepository  Member
bookings/  BookingController BookingService BookingRepository Booking
payments/  PaymentController PaymentService PaymentRepository Payment  Invoices (public)
```

By feature, a change stays in one package, related code is side by side, and most classes can be
**package-private**: only a small interface (`Invoices.hasUnpaid(memberId)`) is public. The
package now hides a decision, as the last lesson asked. Inside a feature you can still layer if
it grows; Simon Brown's **package by component** puts a component's public interface and its
implementation in one package and keeps the controller outside.

Package by feature is the step that leads to the modular monolith of the Intermediate phase:
features become modules with published interfaces, and the rules between them become fitness
functions.

### When layers are still the right choice

A small CRUD application with little business logic, a team that knows the style and a framework
built around it. Layers are cheap to understand, and the sinkhole is harmless when the system is
small. Recognise the signs that it has outgrown them: many sinkholes, changes that always touch
four packages, and a domain that imports the database.

## Further reading

- Mark Richards and Neal Ford, *Fundamentals of Software Architecture*, 2nd ed. (O'Reilly, 2025), chapter "Layered Architecture Style"
- Martin Fowler, "Presentation Domain Data Layering": <https://martinfowler.com/bliki/PresentationDomainDataLayering.html>
- Simon Brown, *Software Architecture for Developers*, vol. 2 (Leanpub), and his talk "Modular Monoliths": package by component
- Martin Fowler, *Patterns of Enterprise Application Architecture* (Addison-Wesley, 2002), "Layering"
- Wikipedia, "Multitier architecture": <https://en.wikipedia.org/wiki/Multitier_architecture>
