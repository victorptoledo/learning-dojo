# Tactical DDD

Strategic DDD splits the domain into bounded contexts. Inside one context, the tactical patterns
give the model its shape. They are design patterns, but with architectural consequences: an
aggregate is a **consistency and transaction boundary**, and that decides what can be stored,
locked, cached and distributed.

## Entities and value objects

- An **entity** has an **identity** that lasts while its attributes change: a customer who moves
  house is the same customer. Two entities with equal fields but different ids are different.
- A **value object** is defined only by its attributes: two `Money(500, "EUR")` are the same
  thing. Value objects are **immutable**, validate themselves on creation, and carry behaviour
  (`plus`, `times`, `convertTo`).

Most domain concepts are values more often than people expect: money, quantities, date ranges,
addresses, email addresses, ids themselves (`OrderId` instead of a bare `String`). Values remove
whole classes of bugs (adding euros to dollars, passing a customer id where an order id was
expected) and make entities smaller. In Java, records are value objects with almost no code.

## Aggregates

An **aggregate** is a cluster of entities and values treated as one unit for changes, with one
entity as its **root**. Outside code holds references only to the root and changes the aggregate
only through the root's methods, which keep its **invariants**: rules that must be true after
every change (an order's total never exceeds the limit; a submitted order accepts no lines).

Vaughn Vernon's four rules of aggregate design:

1. **Model true invariants in consistency boundaries.** An aggregate exists to protect rules that
   must hold **immediately** (in the same transaction). If a rule can be eventually consistent,
   it does not need one aggregate.
2. **Design small aggregates.** Large aggregates load slowly, collide in concurrent updates and
   lock more than they need. Most aggregates are a root and a few values.
3. **Reference other aggregates by identity.** An `Order` holds a `CustomerId`, not a `Customer`.
   That keeps boundaries clear, loading cheap and services splittable.
4. **Use eventual consistency outside the boundary.** One transaction changes one aggregate; when
   a change must affect another, publish a domain event and update the other later.

The *Design an Aggregate* kata applies all four to an anemic order.

### Invariants that span aggregates

Some rules cross aggregates: "a discount code may be used at most 100 times across all orders".
The options are a separate aggregate that owns the rule (a `DiscountCode` with a usage counter,
reserved before the order is paid), accepting eventual consistency and compensating on the rare
race, or asking the business how strict the rule really is. Often the answer is "selling 101 is
fine, selling 150 is not".

## Repositories, factories and domain services

- **Repository**: one per **aggregate root**, with a collection-like interface (`find(id)`,
  `save(order)`), loading and saving the whole aggregate. No repositories for the entities inside
  an aggregate. The Design Patterns track's lesson *Repository, Unit of Work and Data Mapping*
  covers the code side.
- **Factory**: creates a complex aggregate in a valid state (`Order.draft(...)`), when a
  constructor is not enough.
- **Domain service**: a stateless operation that belongs to the domain but not to one entity
  ("transfer money between two accounts", "price a cart with promotions"). Keep them few: logic
  that belongs to one aggregate should live in it, or the model becomes **anemic** (data classes
  with setters and the rules in services, which Fowler calls an anti-pattern).
- **Application service**: not a domain pattern but a layer around it: load the aggregate, call
  it, save it, publish events, manage the transaction. No business rules.

## Domain events

A **domain event** records something that happened that domain experts care about, named in the
past tense: `OrderSubmitted`, `PaymentFailed`. The aggregate records it when its state changes;
the application service publishes it after saving. Events let other aggregates and contexts react
without the aggregate knowing them, which is how rule 4 is implemented, and they are the bridge to
the event-driven architecture of the next lesson.

Rules of thumb: events are immutable facts with the data a consumer needs (ids, amounts), not
whole aggregates; they are published only for changes that were committed; and their names come
from the ubiquitous language.

## Further reading

- Eric Evans, *Domain-Driven Design* (Addison-Wesley, 2003), part II
- Vaughn Vernon, "Effective Aggregate Design", parts I–III (2011): <https://www.dddcommunity.org/library/vernon_2011/>
- Vaughn Vernon, *Implementing Domain-Driven Design* (Addison-Wesley, 2013)
- Martin Fowler, "Anemic Domain Model": <https://martinfowler.com/bliki/AnemicDomainModel.html>
- Martin Fowler, "Value Object": <https://martinfowler.com/bliki/ValueObject.html>
- Martin Fowler, "Domain Event": <https://martinfowler.com/eaaDev/DomainEvent.html>
