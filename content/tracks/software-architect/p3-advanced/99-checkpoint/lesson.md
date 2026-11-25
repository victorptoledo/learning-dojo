# Checkpoint 3: Advanced

This checkpoint closes the **Advanced** phase. It has two parts, and you need both:

1. **The quiz** (press **Q**): eight questions over the whole phase. You need 7 right.
2. **The integration kata** (press **Enter**): a trip booking where an anemic trip applies
   supplier messages itself and a saga that, when the hotel says no, leaves the flight booked.
   You turn the trip into an **aggregate** that guards its rules and records **domain events**,
   and the saga into an **orchestrator** with compensations, an **idempotent receiver** and a
   **dead letter channel**, behind ports.

Do them in any order. Before you start, review:

| Lesson | Ask yourself |
| --- | --- |
| Tactical DDD | Which rules must always hold together? That is the aggregate. What is an entity, and what is only a value? |
| Design an Aggregate (kata) | Can a caller break an invariant? Does a refused command really change nothing? |
| Event-Driven Architecture, CQRS and the Outbox | Is this a command or an event? What happens when the same message arrives twice? |
| Outbox and Projection (kata) | Where can a crash leave the database and the broker disagreeing? |
| Integration Patterns, Sagas and API Gateways | What undoes each step? Orchestration or choreography? Where does a message go when nobody can handle it? |

Three ideas carry the kata:

- **A saga has no rollback.** Each step commits on its own, so a failure later on is answered
  with compensating actions, run in the reverse order of the steps they undo. A compensation is
  a new business action (release the room), not a database undo.
- **At least once means duplicates.** The receiver remembers what it has handled and does
  nothing the second time. Without it, one redelivered reply books a second car.
- **Not every message can be handled.** A reply for an unknown trip or for the wrong leg is not
  retried forever and not dropped silently: it goes to a dead letter channel, where a person
  can look at it.

> [!TIP]
> If a quiz question surprises you, open the lesson it comes from again before retrying. The
> explanation after each answer says which idea it tests.
