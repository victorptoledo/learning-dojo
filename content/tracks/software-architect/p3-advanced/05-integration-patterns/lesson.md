# Integration Patterns, Sagas and API Gateways

Once a system has more than one deployable (or has to talk to someone else's), the
interesting design problems move to the seams. This lesson is about the seams. The System Design
track's lessons on messaging and on timeouts, retries and back-pressure cover the
infrastructure; here the focus is the design choices.

## Synchronous or asynchronous

| | Request-response (HTTP, gRPC) | Messaging (queues, topics) |
| --- | --- | --- |
| caller waits | yes | no |
| both must be up | yes (temporal coupling) | no: the broker holds the message |
| failure handling | caller times out and retries | broker redelivers; dead letter channel |
| good for | queries, commands whose answer is needed now | notifications, long work, smoothing spikes, fan-out |
| cost | availability multiplies along chains | eventual consistency, harder tracing, a broker to run |

A common split: **queries synchronous, state changes asynchronous where the user can wait**.
"Accept the request, return 202 and a status URL, finish in the background" turns a slow,
failure-prone chain into a quick reply and a reliable process.

## The enterprise integration patterns that still matter

Gregor Hohpe and Bobby Woolf catalogued 65 patterns in 2003. Kafka, RabbitMQ, cloud queues and
integration frameworks implement many of them under new names. The ones you meet every week:

| Pattern | What it does | Where you see it today |
| --- | --- | --- |
| point-to-point channel | one consumer takes each message | a queue (SQS, RabbitMQ queue) |
| publish-subscribe channel | every subscriber gets a copy | a topic (Kafka, SNS, RabbitMQ fanout) |
| message translator | converts one format into another | a mapper in an adapter or an ACL |
| content-based router | sends a message to a channel based on its content | routing rules, Kafka Streams branches |
| message filter, splitter, aggregator | drop, split into parts, combine parts | stream processing |
| idempotent receiver | ignores duplicates | the consumers of the previous lesson |
| dead letter channel | parks messages that cannot be processed | DLQs in every broker |
| competing consumers | several workers share one queue | consumer groups, worker pools |
| correlation identifier | links a reply to its request | correlation ids, trace ids |
| claim check | stores a large payload and sends a reference | an S3 key in the message |

Knowing the names helps you recognise a problem as solved, and pick the right broker feature
instead of writing it again.

## Sagas

A business transaction that spans services cannot use a database transaction. A **saga**
(Hector Garcia-Molina and Kenneth Salem, 1987) splits it into a sequence of **local**
transactions, each with a **compensating action** that semantically undoes it:

| Step | Action | Compensation |
| --- | --- | --- |
| 1 | reserve the flight | cancel the reservation |
| 2 | reserve the hotel | cancel the reservation |
| 3 | take the payment | refund |

If step 2 fails, run the compensations of the completed steps in reverse (cancel the flight).
Some notes from practice:

- Compensations are **business actions**, not rollbacks: a refund is a new transaction the
  customer may see, and a cancelled reservation may cost a fee. Design them with the domain
  experts.
- Put steps that **cannot be undone** (the pivot) as late as possible, and steps likely to fail
  early.
- Every step and compensation must be **idempotent** and retryable, because messages are
  redelivered.
- The saga's intermediate states are visible: a booking is "pending" for a while. Model that
  state explicitly (and its timeouts) instead of hiding it.
- Sagas give up isolation: another process can see the half-done state. Countermeasures include
  semantic locks (a "pending" flag), re-reading values before acting, and ordering steps so that
  harmful intermediate states are short.

As the previous lesson showed, a saga can be **choreographed** (each service reacts to events)
or **orchestrated** (a saga orchestrator sends commands and tracks the state). Orchestration is
usually clearer past three steps; workflow engines such as Temporal or Camunda persist the
orchestrator's state for you.

## API gateways and backends for frontends

An **API gateway** is one entry point in front of many services: routing, authentication, rate
limiting, TLS, request logging. Keep business logic out of it, or it becomes a new monolith that
every team must change.

A **backend for frontend** (BFF, Phil Calçado and Sam Newman's term) is a small backend per kind
of client (web, mobile, partner API), owned by the client team, that aggregates and shapes the
calls its client needs. It trades a little duplication for clients that do not have to make
twelve calls, and for teams that can change their screens without negotiating a generic API.

## Further reading

- Gregor Hohpe and Bobby Woolf, *Enterprise Integration Patterns* (Addison-Wesley, 2003), and the pattern catalogue: <https://www.enterpriseintegrationpatterns.com/patterns/messaging/>
- Hector Garcia-Molina and Kenneth Salem, "Sagas" (SIGMOD, 1987): <https://dl.acm.org/doi/10.1145/38713.38742>
- Chris Richardson, "Pattern: Saga": <https://microservices.io/patterns/data/saga.html>
- Caitie McCaffrey, "Distributed Sagas: A Protocol for Coordinating Microservices" (talk, 2017)
- Sam Newman, "Backends For Frontends": <https://samnewman.io/patterns/architectural/bff/>
- Chris Richardson, "Pattern: API Gateway": <https://microservices.io/patterns/apigateway.html>
