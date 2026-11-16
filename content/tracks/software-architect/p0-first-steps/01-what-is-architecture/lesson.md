# What Software Architecture Is

Ask ten engineers what software architecture is and you get ten diagrams. The definitions that
hold up are about **decisions**, not boxes.

## Architecture is the expensive stuff

Martin Fowler, after Ralph Johnson, puts it this way: architecture is "the important stuff,
whatever that is", and the important stuff is what is **hard to change** later. Bass, Clements
and Kazman (*Software Architecture in Practice*) say it more formally: the architecture is the set
of **structures** needed to reason about a system, made of elements, the relations between them
and their properties.

Both lead to the same test. A decision is architectural when reversing it is expensive:

| Usually architecture | Usually design |
| --- | --- |
| one deployable or twenty services | the name of a class |
| which database, and who owns which data | which loop you use to fill a list |
| synchronous calls or events between parts | a strategy versus an `if` inside one module |
| how modules may depend on each other | the fields of a record inside one module |
| where authentication happens | the wording of an error message |

The line moves with context: a public API's shape is architecture, because other teams depend on
it; the same shape inside one module is design. Code-level patterns belong to design; the Design
Patterns track covers them. This track is about the decisions above them.

## The structures an architecture describes

- **Module structures**: how the code is split (packages, modules, layers) and which part may
  use which. They decide how easy the system is to change.
- **Runtime structures**: the processes, services, queues and data stores that exist when the
  system runs, and how they talk. They decide performance and availability.
- **Allocation structures**: where things run (machines, regions, containers) and **who owns
  what** (teams). They decide cost, operations and how teams work.

A diagram that mixes all three ("this box is a package, that one is a server, this one is the
billing team") explains nothing. The C4 lesson later in the track shows how to draw each view on
its own.

## Quality attributes drive architecture

Functional requirements say **what** the system does. Almost any architecture can deliver them:
you can sell tickets from a monolith or from fifty services. What separates architectures is
how well they deliver the **quality attributes** (the "-ilities"):

| Attribute | The question it asks |
| --- | --- |
| performance | how fast, under what load? (latency, throughput) |
| scalability, elasticity | what happens when load grows tenfold, or spikes for ten minutes? |
| availability, reliability | how often is it down, and what fails when one part fails? |
| modifiability, evolvability | how much does a typical change cost? |
| testability, deployability | can we check and ship a change quickly and safely? |
| security | who can do what, and what happens when someone tries more? |
| observability, operability | can we tell what it is doing and fix it at 3 a.m.? |
| cost, simplicity | what does it cost to run, and can this team understand it? |

ISO/IEC 25010 gives a standard list; the exact list matters less than choosing the **few** that
matter for this system. "It should be scalable, secure and fast" is not a requirement; every
system wants that.

### Quality attribute scenarios

Make an attribute concrete with a **scenario**: a stimulus arriving from a source, in an
environment, and the response you expect, with a measure.

> When a popular show goes on sale (**stimulus**) 50,000 buyers (**source**) arrive within ten
> minutes during normal operation (**environment**); the system keeps selling (**response**) with
> 95 % of purchases completed in under 3 seconds and no seat sold twice (**measure**).

> A developer changes a ticket price rule (**stimulus**) in the codebase (**environment**); the
> change touches one module and is in production the same day (**measure**).

Scenarios turn arguments ("microservices scale better!") into checks you can design for, test and
monitor. Collected and ranked by importance and difficulty, they form a **utility tree**, the
input of evaluation methods such as ATAM (see the lesson on the architect's role).

## Everything is a trade-off

The first law of software architecture, in Mark Richards and Neal Ford's words, is that
everything is a trade-off. Attributes pull against each other:

- **Performance versus modifiability**: layers and indirections make change cheap and add hops.
- **Availability versus consistency**: in a network partition you choose one (the System Design
  track's lesson on CAP and PACELC covers why).
- **Scalability versus simplicity and cost**: many small services scale apart and cost more to
  run, test and understand.
- **Security versus usability**: every extra check is friction.

So "what is the best architecture?" has no answer; "what is the least bad one for **these**
attributes, **this** team and **this** budget?" does. The second law in the same book is that
**why** is more important than **how**: a decision without its reasons cannot be revisited when
the reasons change. That is what the next lesson's decision records are for.

## Further reading

- Martin Fowler, "Software Architecture Guide": <https://martinfowler.com/architecture/>
- Martin Fowler, "Who Needs an Architect?" (IEEE Software, 2003): <https://martinfowler.com/ieeeSoftware/whoNeedsArchitect.pdf>
- Len Bass, Paul Clements and Rick Kazman, *Software Architecture in Practice*, 4th ed. (Addison-Wesley, 2021): quality attributes and scenarios
- Mark Richards and Neal Ford, *Fundamentals of Software Architecture*, 2nd ed. (O'Reilly, 2025): architecture characteristics and the laws of architecture
- ISO/IEC 25010 product quality model: <https://iso25000.com/index.php/en/iso-25000-standards/iso-25010>
- roadmap.sh, "Software Architect": <https://roadmap.sh/software-architect>
