# Evolutionary Architecture

Every lesson so far assumed you design and then build. Real systems live for years, and their
requirements, teams and technology change under them. Neal Ford, Rebecca Parsons and Patrick Kua
define an **evolutionary architecture** as one that supports **guided, incremental change**
across multiple dimensions. "Guided" is the key word: by fitness functions. "Incremental": by
small, safe steps.

## Fitness functions

A **fitness function** is an objective check of how close the architecture is to one of its
goals. You have written several already: "the domain uses nothing from adapters", "modules talk
only through their api", "no package cycles". They are not only about code structure:

| Quality attribute | Example fitness function |
| --- | --- |
| modularity | dependency rules and cycle checks over the compiled classes (ArchUnit) |
| performance | p95 latency of the checkout endpoint under 300 ms in a load test |
| availability, resilience | error rate in production below 0.1 %; chaos experiments that kill an instance |
| security | no dependency with a known critical vulnerability; secrets scanning |
| deployability | build and deploy in under 15 minutes |
| data | every schema migration is backward compatible with the previous release |
| cost | cloud spend per order below a threshold |

Ways to classify them, which help decide where each one runs:

- **atomic** (one attribute in one context: a dependency rule) or **holistic** (several together:
  latency **and** consistency in a staging environment);
- **triggered** (runs on an event: a build, a deploy) or **continuous** (monitors production all
  the time: an SLO alert);
- **static** (fixed threshold) or **dynamic** (relative to something: "no worse than last week");
- **automated** or, rarely, **manual** (a review step for a legal requirement).

A fitness function turns an ADR's consequence into something checked: the decision says "the
domain does not depend on the database", the fitness function makes sure it stays true.

### Adopting them on legacy code

A new rule on old code fails a thousand times on day one. Turning it off defeats it; fixing
everything first stops the team. The **ratchet** (the *Fitness Function Suite* kata builds one):

1. record today's violations in a **baseline** file kept in the repository;
2. the build fails only on violations **not** in the baseline;
3. when a violation is fixed, remove it from the baseline (the build can insist), so it cannot
   come back.

ArchUnit calls this "freezing" rules; many linters have the same idea. The number only goes
down.

## Incremental migration

Big-bang rewrites fail often and expensively: the old system keeps changing while the new one is
built, and the cut-over day is a cliff. Migrate in slices instead.

### Strangler fig

Martin Fowler's **strangler fig application**, named after a vine that grows around a tree until
it replaces it:

1. put a **facade** in front of the old system (a proxy, a gateway, a routing layer in the
   monolith);
2. build one slice of functionality in the new system;
3. route that slice's traffic to the new system, keeping a way back;
4. repeat until the old system serves nothing, then remove it.

Pick early slices for **learning** and low risk (one product type, one region, read-only
screens), not for size. Data is usually the hard part: who owns it during the transition, and
how it is synchronised (events, change data capture, or a period of dual reads).

### Branch by abstraction

For a change inside one codebase (replacing a persistence library, a payment provider), without a
long-lived branch:

1. introduce an **abstraction** (a port) in front of the old implementation and move all callers
   to it;
2. build the new implementation behind the same abstraction, switched off;
3. switch callers over gradually (a feature toggle per caller or per customer);
4. remove the old implementation and, if it no longer earns its keep, the abstraction.

The main branch stays releasable the whole time, which is the point.

### Proving equivalence

Before switching traffic, compare: a **parallel run** sends the same input to the old and the
new implementation, uses the old answer and logs differences (GitHub's Scientist library is built
for this); **shadow traffic** replays production requests against the new system; a **dark
launch** runs the new path in production without anyone seeing its results. Each turns "we think
it is the same" into evidence.

## Sacrificial architecture and technical debt

Fowler also describes **sacrificial architecture**: deliberately building something you expect to
throw away when the business grows (eBay and Google rewrote their early systems more than once).
Knowing which parts are sacrificial changes how much to invest in them. Technical debt, in Ward
Cunningham's original sense, is the gap between what the code says and what the team now
understands; evolutionary architecture is the habit of paying it down in small steps, guided by
fitness functions, instead of in rewrites.

## Further reading

- Neal Ford, Rebecca Parsons, Patrick Kua and Pramod Sadalage, *Building Evolutionary Architectures*, 2nd ed. (O'Reilly, 2022)
- Martin Fowler, "Strangler Fig Application": <https://martinfowler.com/bliki/StranglerFigApplication.html>
- Martin Fowler, "Branch By Abstraction": <https://martinfowler.com/bliki/BranchByAbstraction.html>
- Martin Fowler, "Sacrificial Architecture": <https://martinfowler.com/bliki/SacrificialArchitecture.html>
- ArchUnit user guide, "Freezing Arch Rules": <https://www.archunit.org/userguide/html/000_Index.html#_freezing_arch_rules>
- GitHub, Scientist (parallel runs): <https://github.com/github/scientist>
