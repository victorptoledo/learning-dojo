# Microservices and Their Costs

## What a microservice is

James Lewis and Martin Fowler's 2014 description is still the reference: an application built
as a suite of small services, each **running in its own process**, communicating through
lightweight mechanisms, **built around business capabilities** and **independently deployable**
by fully automated machinery, with decentralised data management.

The two properties that matter most:

1. **Independent deployability**: you can change and release one service without releasing any
   other. If you cannot, you do not have microservices, whatever the number of repositories.
2. **Data ownership**: each service owns its data, and nobody else reads its database. Other
   services ask through its API or subscribe to its events.

Size is **not** the definition. "Micro" says small enough for one team to own and understand,
not a line count.

## What they buy

- **Independent releases**: a team ships its service ten times a day without coordinating.
- **Independent scaling**: the image service gets fifty instances, the admin service two.
- **Fault isolation**: a crash in recommendations does not take checkout down (if the callers
  are built for it).
- **Technology freedom**: a different language or database where it really helps.
- **Team autonomy**: a team owns a service end to end, from code to production.

## What they cost

Every one of these benefits is paid for with the **network**. The fallacies of distributed
computing (Peter Deutsch and colleagues at Sun) list what developers wrongly assume: the network
is reliable, latency is zero, bandwidth is infinite, the network is secure, topology does not
change, there is one administrator, transport cost is zero, the network is homogeneous.

In practice:

- **Partial failure**: any call can time out, fail, or succeed without you hearing back.
  Timeouts, retries, idempotency and circuit breakers become mandatory (the System Design
  track's lesson on overload, timeouts and retries covers them).
- **Latency and availability multiply**: a request that crosses five services each at 99.9 %
  availability is at best 99.5 % available, and their latencies add up.
- **No transactions across services**: a business operation over two services needs sagas and
  eventual consistency (the Advanced phase).
- **Data duplication and queries across services**: a report over orders and customers needs an
  API composition or a read model fed by events.
- **Operations**: per service a pipeline, monitoring, logs, tracing, secrets, versions and on-call.
  A platform team often becomes necessary.
- **Testing**: contract tests between services, end-to-end environments that are slow and flaky.

Fowler calls the total the **microservice premium**: worth paying only when the system is complex
enough that a monolith's coordination costs more.

## The distributed monolith

The worst of both worlds: services that must be **changed and deployed together**, because they
share a database, a library of domain classes, or chatty synchronous call chains. You pay the
network costs and get none of the independence. Signs:

- releases need a fixed order of deployments;
- one feature touches many services every time;
- services read each other's tables;
- a shared "common model" library forces lock-step upgrades;
- an outage in one service takes down five.

Cure it by moving boundaries (merging services that always change together is a legitimate
fix), giving each piece of data one owner, and replacing synchronous chains with events where the
caller does not need an answer now.

## Conway's law and team topologies

> Organizations which design systems are constrained to produce designs which are copies of the
> communication structures of these organizations. (Melvin Conway, 1968)

Service boundaries that cut across team boundaries need constant cross-team coordination; teams
that share a service step on each other. The **inverse Conway manoeuvre** shapes the teams first,
so the architecture you want is the one the organisation naturally produces.

Matthew Skelton and Manuel Pais (*Team Topologies*) name four team types: **stream-aligned**
teams own a slice of the business end to end, **platform** teams provide self-service
infrastructure, **enabling** teams coach, and **complicated-subsystem** teams own a part that
needs rare expertise. They also stress **cognitive load**: a team should own no more services
than it can understand. Twelve services for one team of six is a warning sign.

## Choosing

Start from the modular monolith of the previous lesson. Extract a service when a force demands
it (scaling, cadence, team, isolation) and the platform can carry it (automated deployment,
monitoring, tracing). Many systems end with a few services around a modular core, and that is a
good outcome, not a failure to finish.

## Further reading

- James Lewis and Martin Fowler, "Microservices" (2014): <https://martinfowler.com/articles/microservices.html>
- Martin Fowler, "Microservice Premium": <https://martinfowler.com/bliki/MicroservicePremium.html>
- Martin Fowler, "Conway's Law": <https://martinfowler.com/bliki/ConwaysLaw.html>
- Wikipedia, "Fallacies of distributed computing": <https://en.wikipedia.org/wiki/Fallacies_of_distributed_computing>
- Sam Newman, *Building Microservices*, 2nd ed. (O'Reilly, 2021)
- Matthew Skelton and Manuel Pais, *Team Topologies*, 2nd ed. (IT Revolution, 2025)
- Chris Richardson, microservices.io patterns: <https://microservices.io/patterns/>
