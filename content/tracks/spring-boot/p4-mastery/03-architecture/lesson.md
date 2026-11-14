# Application Architecture with Spring

Spring makes it easy to start; it does not stop an application from turning into a ball of mud
where every class can call every other. Architecture is about **boundaries**: which parts know
about which others, and what each part promises. This lesson covers how to draw them in a Spring
Boot application, and when a boundary should become a separate service.

## Package by feature, not by layer

```text
by layer (hard to keep apart)          by feature (boundaries you can see)
shop/                                   shop/
├── controllers/                        ├── catalog/
│   ├── ProductController               │   ├── CatalogApi.java        public
│   └── OrderController                 │   ├── ProductController.java
├── services/                           │   ├── ProductService.java    package-private
│   ├── ProductService                  │   └── ProductRepository.java package-private
│   └── OrderService                    ├── orders/
└── repositories/                       │   ├── OrderApi.java          public
    ├── ProductRepository               │   └── ...
    └── OrderRepository                 └── shipping/
```

In a feature package, most classes can be **package-private**: only a small API (a service
interface, events, DTO records) is public. Other features cannot reach into `catalog`'s
repository, because Java forbids it.

## Modules inside one application: a modular monolith

A **modular monolith** is one deployable application made of modules with explicit boundaries.
It keeps the simplicity of one process (one deploy, local calls, one transaction when needed)
while staying easy to change and, later, to split.

**Spring Modulith** turns top-level packages under the application package into modules and
**verifies** the rules:

```java
class ModularityTests {
    @Test
    void modules_respect_their_boundaries() {
        ApplicationModules.of(ShopApplication.class).verify();
    }
}
```

The test fails when a module uses another module's internal (non-API) types or when modules
depend on each other in a cycle. It can also generate diagrams and documentation of the modules.

### Talk through events

Direct calls between modules create dependencies in both directions over time. Events keep them
one-way:

```java
// orders publishes
events.publishEvent(new OrderPlaced(order.id(), order.lines()));

// shipping listens, in its own module
@ApplicationModuleListener                 // transactional, after commit, asynchronous
void on(OrderPlaced event) { shipments.prepare(event); }
```

With Spring Modulith's **event publication registry**, published events are stored with the
transaction and retried if a listener fails, which is the outbox pattern without writing it.
`@ApplicationModuleTest` tests one module on its own.

## Hexagonal thinking: ports and adapters

Inside a module, keep the **domain** (rules) independent of the **technology** (HTTP, database,
messaging):

```text
            adapters in                 core                     adapters out
HTTP  ──>  OrderController  ──>  PlaceOrder (use case)  ──>  OrderStore (port)  <── JpaOrderStore
                                  Order, OrderLine (domain)   PaymentPort       <── StripePayments
```

- The core defines **ports**: interfaces for what it needs (`OrderStore`, `PaymentPort`).
- **Adapters** implement them with Spring Data, `RestClient`, Kafka...
- The core can be tested with fakes, no Spring, no database.

You do not need this everywhere. Simple CRUD modules can stay "controller → service → repository".
Apply it where the business rules are rich or the technology is likely to change.

## When to split into services

Microservices give **independent deployment and scaling** to separate teams, at a real cost:
network calls that fail, distributed data and no shared transactions, more infrastructure, harder
debugging. Split a module out when there is a concrete reason:

- different teams need to release it **independently** and often;
- it needs very different **scaling** or availability (a public catalog versus back-office
  reports);
- it has a different **technology** need or a strict isolation requirement (payments, PCI scope).

Not good reasons: "it is big", "microservices are modern", "to make the code clean". A modular
monolith with verified boundaries gives clean code, and its modules are already the candidates if
you ever need to split.

> [!TIP]
> Each module owning its own tables (no other module reads them directly) is the step that makes
> a later split cheap. Joining across module tables is the step that makes it expensive.

## Summary

- Package by feature; keep most classes package-private behind a small public API.
- A modular monolith keeps one deployable with explicit, tested module boundaries (Spring
  Modulith `verify()`).
- Modules communicate through events (`@ApplicationModuleListener`, the publication registry).
- Ports and adapters keep rich domains independent of technology.
- Split into services for independent deployment, scaling or isolation, not for size or fashion;
  modules that own their data are ready to split.
