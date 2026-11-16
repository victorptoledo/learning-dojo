# The Modular Monolith

"Monolith" has become an insult, and the cure most often prescribed is microservices. But the
problem of most monoliths is not that they are deployed as one unit; it is that they have **no
inside structure**: any class calls any class and any query reads any table. Brian Foote and
Joseph Yoder called that the **big ball of mud**. A **modular monolith** keeps the single
deployable and fixes the structure.

## What it is

- **One deployable** (one process, one release), usually **one database server**.
- **Modules by business capability** (catalog, orders, shipping), not by technical layer.
- Each module **publishes** a small interface (an `api` package: a facade, a few records, maybe
  events) and **hides** everything else.
- Each module **owns its data**: its own tables or schema. Another module that needs the data
  asks through the published interface, never with a join across modules.
- The boundaries are **enforced**, not just drawn.

```text
dojo.store.catalog.api       Catalog, ProductView          published
dojo.store.catalog.internal  CatalogService, ProductTable  hidden
dojo.store.orders.api        Orders, OrderConfirmation
dojo.store.orders.internal   OrderService
dojo.store.App               composition root
```

## What it buys

Compared with the ball of mud: changes stay inside a module, teams can own modules, tests can
run per module, and the dependency graph is a picture you can reason about.

Compared with microservices, it **keeps what the network takes away**:

- calls are function calls: no latency, no partial failure, no retries, no serialisation;
- a business operation across modules can still be **one database transaction** when it must;
- one build, one deployment, one place to look at logs;
- refactoring a boundary is a code change, not a coordinated release of two services and a data
  migration.

Shopify, with one of the largest Ruby on Rails codebases in the world, chose to modularise its
monolith rather than split it, and wrote about it; so did many others.

## Enforcing the boundaries

A module boundary that nothing checks lasts until the first deadline. Layer the defences:

1. **Visibility**: internal classes are package-private (Java), `internal` (C#), unexported (Go);
   only the `api` is public.
2. **Build modules**: a Maven or Gradle module, a .NET project, a workspace package per module,
   so the compiler refuses an import that is not declared.
3. **Fitness functions**: tests over the dependency graph, like the *Module Boundaries* kata:
   "a module may use another only through its `api`", "an `api` must not expose `internal`
   types", "no cycles between modules". Tools: ArchUnit, Spring Modulith's `verify()`,
   `import-linter`, Nx `enforce-module-boundaries`, `dependency-cruiser`.
4. **Data ownership**: one schema (or table prefix) per module, and a database user per module
   if you want the database to enforce it too.

## Talking between modules

- **Direct calls** to the other module's published interface, for queries and for commands that
  need an answer now.
- **In-process events** for "this happened, whoever cares": `OrderPlaced` published inside the
  process, handled by shipping and notifications. The publisher does not know its listeners, so
  the arrow points only one way. Spring Modulith can persist these events in a table so a
  listener that fails is retried, which is the outbox idea of the Advanced phase.

Avoid a shared `common` module that grows into the zone of pain: share only truly stable things
(money, ids), as a deliberate **shared kernel**.

## Monolith first

Martin Fowler's "Monolith First" observes that almost every successful microservice system
started as a monolith that grew too big, and that systems built as microservices from scratch
often ended in trouble: early on you do not yet know where the stable boundaries are, and moving
a boundary between services is expensive. A modular monolith lets you **find** the boundaries
cheaply, then extract the ones that need it.

Signs that a module should become a service:

- it needs to **scale** very differently (image processing next to a CRUD admin);
- it needs a **different release cadence** or availability target;
- a **separate team** owns it and is slowed down by the shared release;
- it needs a **different technology** (a language, a database) for good reason;
- **isolation**: a fault or a security boundary must not take the rest down.

If none applies, the module is fine where it is. Because it already talks through an interface
and owns its data, extracting it means replacing the in-process adapter with a network one, not
untangling a ball of mud.

## Further reading

- Martin Fowler, "Monolith First": <https://martinfowler.com/bliki/MonolithFirst.html>
- Brian Foote and Joseph Yoder, "Big Ball of Mud" (1997): <http://www.laputan.org/mud/>
- Shopify Engineering, "Deconstructing the Monolith": <https://shopify.engineering/deconstructing-monolith-designing-software-maximizes-developer-productivity>
- Spring Modulith reference documentation: <https://docs.spring.io/spring-modulith/reference/>
- Kamil Grzybek, "Modular Monolith: A Primer": <https://www.kamilgrzybek.com/blog/posts/modular-monolith-primer>
- Sam Newman, *Monolith to Microservices* (O'Reilly, 2019)
