# Architecture Decision Records

The previous lesson ended with the second law of architecture: **why** matters more than
**how**. The code shows how; the reasons vanish when the people who knew them change teams. An
**architecture decision record** (ADR) keeps them.

## Which requirements are architectural?

Not every requirement shapes the architecture. The ones that do are called **architecturally
significant requirements** (ASRs). Signs that a requirement is one:

- it has a **high cost of change** if you get it wrong (data residency, tenancy model, offline
  mode);
- it is a demanding **quality attribute scenario** (50,000 buyers in ten minutes; recover in five
  minutes);
- it cuts across many parts (audit every change; every call authenticated);
- it is a **constraint** you cannot negotiate: a regulation, a budget, a deadline, a technology
  the company already runs, the skills of the team.

Constraints are not decisions: they remove options before you start. Write them down anyway,
because a constraint that silently disappears ("we no longer have to run on-premises") is a
reason to revisit decisions.

## Comparing options

For a significant decision, list at least two real options (doing nothing is often one), the
criteria that matter (the quality attributes and constraints from the ASRs) and how each option
fares. A short table is enough:

| Criterion | Read replica | Nightly warehouse export | Cache reports |
| --- | --- | --- | --- |
| checkout protected | yes | yes | partly (cache misses still hit the database) |
| report freshness | seconds | one day | minutes to hours |
| new things to run | one replica | a warehouse and a pipeline | a cache |
| cost | medium | high | low |

The table does not decide; it shows the trade-off so the decision is explicit. When options
are close, prefer the one that is easier to reverse.

## Reversible and irreversible decisions

Jeff Bezos calls them **two-way doors** and **one-way doors**. A two-way door (a library behind
an interface, a cache in front of a query) can be walked back cheaply: decide quickly, try it,
measure. A one-way door (a public API contract, a database that other teams start querying,
splitting the data of a service) deserves slow, careful work.

Two related ideas from lean and agile practice:

- decide at the **last responsible moment**: as late as possible, but not so late that the
  decision is made for you by default;
- **make decisions reversible** where you can: put the choice behind a port, keep data access in
  one module, release behind a feature flag. Much of this track is about structures that turn
  one-way doors into two-way doors.

## The ADR format

Michael Nygard proposed the format in 2011, and it is still the most used. One short Markdown
file per decision, numbered, kept in the repository (`docs/adr/0007-read-replica-for-reports.md`):

```markdown
# 7. Use a read replica for reporting

Status: accepted (2026-03-02)

## Context
Reporting queries run on the primary database and slow checkout every Monday ...

## Decision
We will route all reporting queries to a PostgreSQL read replica ...

## Consequences
Reports can be a few seconds behind. We must monitor replication lag ...
```

- **Context**: the forces at play, in neutral words, including constraints and the ASRs. Someone
  who was not in the room must understand why this was hard.
- **Decision**: one active sentence: "We will ...".
- **Consequences**: what becomes easier **and** what becomes harder. An ADR with only benefits
  is a sales pitch.
- **Status**: proposed, accepted, deprecated, or **superseded by** ADR n.

Variants such as MADR (Markdown Any Decision Records) add "Decision drivers" and "Considered
options" sections; Y-statements squeeze a decision into one sentence ("In the context of ...,
facing ..., we decided ... to achieve ..., accepting ..."). The format matters less than writing
it **when** the decision is made.

### Rules that keep ADRs useful

- **Immutable**: do not rewrite an accepted ADR. When the decision changes, write a new one and
  mark the old one "superseded by 12". The log of decisions is the history of the architecture.
- **Small**: one decision per record, one or two pages at most.
- **Next to the code**, reviewed in pull requests like code, so they are found and kept current.
- **Linked from the code** where it helps: a comment saying "see ADR 7" next to the routing of
  report queries.

### Where ADRs fit

A decision log does not replace diagrams (the C4 lesson) or a design document for a large change;
it records the decisions those documents lead to. Some teams use a lightweight **RFC** or
**advice process** to discuss a proposal and end it with an ADR (see the lesson on the
architect's role).

## Further reading

- Michael Nygard, "Documenting Architecture Decisions" (2011): <https://cognitect.com/blog/2011/11/15/documenting-architecture-decisions>
- ADR GitHub organisation (templates, tools, MADR): <https://adr.github.io/>
- Olaf Zimmermann, the Y-statement format for decisions (described with other templates on <https://adr.github.io/>)
- Jeff Bezos, Amazon letter to shareholders for 2015 (one-way and two-way doors)
- Mark Richards and Neal Ford, *Fundamentals of Software Architecture*, 2nd ed. (O'Reilly, 2025), chapter on architecture decisions
- Thoughtworks Technology Radar, "Lightweight Architecture Decision Records": <https://www.thoughtworks.com/radar/techniques/lightweight-architecture-decision-records>
