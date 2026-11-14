# System Architecture

Architecture is the set of decisions that are expensive to change later: how the system is split,
how the parts talk, who owns which data. There is no best architecture, only trade-offs that fit a
team, a domain and a stage of the product.

## Monolith, modular monolith, microservices

| | Monolith | Modular monolith | Microservices |
| --- | --- | --- | --- |
| Deployables | one | one | many |
| Boundaries | by convention (often eroded) | enforced modules with APIs | network and separate databases |
| Calls between parts | in-process | in-process | network: latency and failures |
| Data | one database | one database, tables owned per module | a database per service |
| Transactions | local, easy | local, easy | distributed: sagas, eventual consistency |
| Teams | small, one codebase | several teams, one release | independent teams, independent releases |
| Operations | simple | simple | many pipelines, observability is mandatory |

Start with a **modular monolith**: it keeps the simplicity of one deployable while drawing the
boundaries you would need anyway. Extract a service when a module needs **independent deployment,
scaling or isolation** badly enough to pay for the network.

## Boundaries from the domain: bounded contexts

Domain-Driven Design's **bounded context** is the most useful tool for drawing boundaries. A
"product" means different things to the catalogue (description, images), pricing (price lists,
discounts), inventory (stock per warehouse) and shipping (weight, dimensions). Forcing one shared
`Product` model on all of them couples everything; giving each context its own model, language and
data keeps them independent.

- A good boundary has **high cohesion** inside and **few, explicit interactions** outside.
- Each context **owns its data**; others ask it (API) or listen to it (events), never read its
  tables directly.
- Map how contexts relate: one may be upstream of another, translate at the border with an
  **anti-corruption layer**, or share a published language (an event schema).

## Communication styles

- **Synchronous** (HTTP/gRPC): simple request-response, immediate answers; but the caller depends on
  the callee being up and fast. Chains of synchronous calls multiply latency and failure.
- **Asynchronous** (events, queues): the publisher does not wait or depend on consumers; consumers
  can be down, catch up later, scale independently. Costs: eventual consistency and harder debugging.

Use synchronous calls for queries that need an answer now, events for things other parts react to.

## Event-driven architecture

- **Event notification**: "OrderPlaced {id}"; consumers call back for details. Small events, more
  calls.
- **Event-carried state transfer**: the event carries the data consumers need, so they keep their
  own copy and stop calling. Fewer calls, more duplicated data.
- **Event sourcing**: the event log **is** the source of truth; current state is derived by
  replaying events. Powerful for audit and temporal queries, and a big commitment.

## Distributed workflows: sagas

Placing an order touches payment, inventory and shipping, each with its own database. There is no
transaction across them. A **saga** is a sequence of local transactions, each with a
**compensating action** that undoes it if a later step fails:

```text
reserve stock ──▶ charge payment ──▶ create shipment
      ▲                 │ fails
      └── release stock ◀┘   (compensation)
```

- **Choreography**: each service reacts to the previous one's event. Few moving parts, but the flow
  is hidden across services.
- **Orchestration**: one coordinator tells each service what to do and handles failures. The flow is
  explicit and easier to change and observe.

Compensations are business actions (refund, release, cancel), not rollbacks, and every step must be
idempotent because messages repeat.

## Making decisions

Record significant decisions in **ADRs** (architecture decision records): context, options, the
decision and its consequences. Prefer decisions that are **reversible**, delay irreversible ones until
you know more, and revisit them when the context changes. Conway's law applies: systems end up
mirroring the communication structure of the teams that build them, so design teams and boundaries
together.

## Summary

- Monolith, modular monolith and microservices trade simplicity for independence; start modular,
  extract with a reason.
- Draw boundaries with bounded contexts that own their data.
- Synchronous calls for immediate answers, events for reactions; mind latency chains.
- Events notify, carry state, or become the source of truth (event sourcing).
- Coordinate cross-service workflows with sagas and compensations, choreographed or orchestrated.
- Record decisions in ADRs; teams and architecture shape each other.
