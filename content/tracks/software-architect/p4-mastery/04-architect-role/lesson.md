# The Architect's Role

Everything in this track is something an architect does. This last lesson is about **how**: with
whom, through which processes, and with which skills that are not about technology.

## What architects do

Mark Richards and Neal Ford list eight expectations of an architect: make architecture decisions;
continually analyse the architecture; keep current with trends; ensure compliance with decisions;
have diverse exposure and experience; have business domain knowledge; possess interpersonal
skills; understand and navigate organisational politics. Only the first three are about
technology.

Gregor Hohpe's image is the **architect elevator**: the architect rides between the penthouse,
where strategy and budgets are decided, and the engine room, where the code runs, and translates
in both directions. An architect who only rides to the penthouse becomes a "PowerPoint
architect" whose plans the engine room ignores; one who never leaves the engine room optimises
code nobody asked to optimise.

Fowler's "Who Needs an Architect?" distinguishes the architect who makes all the important
decisions (and becomes a bottleneck) from the one who **mentors** the team so it makes good
decisions itself, is aware of what is going on, and handles the hardest problems. The second kind
scales.

### Staying technical

Credibility with developers and good judgement both come from the code. Ways to stay close
without becoming the bottleneck: write proofs of concept and spikes, pair with teams, review
pull requests in areas with architectural impact, fix non-critical bugs, write the fitness
functions and the tooling that enforces decisions. Avoid owning critical-path features: you will
be in meetings when they are due.

### Working with stakeholders

Architecture serves business goals, so the architect must understand them and explain technical
trade-offs in business terms: not "eventual consistency" but "the stock count on the page can be
a few seconds behind; in exchange, checkout keeps working when the warehouse system is down".
Quality attribute scenarios are the shared language: business people can rank them, engineers
can build to them.

## Evaluating an architecture

The SEI's **ATAM** (Architecture Tradeoff Analysis Method) is the classic heavyweight method; its
core ideas work in an afternoon:

1. present the **business drivers** and the architecture;
2. build a **utility tree**: quality attributes refined into concrete scenarios, each rated for
   business importance and technical difficulty;
3. for the highest-rated scenarios, walk through how the architecture responds, and record
   **sensitivity points** (a decision that strongly affects one attribute), **trade-off points**
   (a decision that affects several in opposite directions) and **risks**;
4. summarise the risks into **risk themes** that go back to the business drivers.

Lighter cousins: a **pre-mortem** ("it is a year from now and this failed; why?"), a design
review against the top five scenarios, and **architecture katas** (Ted Neward's idea, popularised
by Neal Ford): small teams design a system for a fictional brief in an hour and defend it, which is
the best practice there is for the trade-off thinking this job needs.

## Governance that scales

Central review boards that approve every decision slow teams down and are routed around. Modern
governance moves decisions to the teams and keeps a few things true automatically:

- **Decide what is decided where**: a short list of decisions with wide blast radius
  (authentication, data ownership, integration styles, the platform) is guided centrally;
  everything else belongs to teams.
- **The advice process** (Andrew Harmel-Law, after Dennis Bakke): **anyone** can make an
  architectural decision, provided they first ask for advice from everyone affected and from
  people with expertise. They need not follow the advice, but must consider it and record the
  decision in an ADR. An **architecture advisory forum** meets regularly to give advice, not
  approval.
- **Paved roads**: make the recommended way the easiest way (templates, platforms, libraries), so
  teams choose it because it saves them work.
- **Fitness functions**: rules that matter are checked by the build and by production
  monitoring, as you did in this track, not by a person who reads every design.
- **A technology radar** (Thoughtworks' format): adopt, trial, assess, hold, so teams know which
  technologies are encouraged and which are being retired.

The architect's job in this model is to make good decisions **likely**: by teaching, advising,
writing down principles, building the paved roads and the fitness functions, and stepping in on
the few decisions that need one owner.

## Further reading

- Martin Fowler, "Who Needs an Architect?": <https://martinfowler.com/ieeeSoftware/whoNeedsArchitect.pdf>
- Gregor Hohpe, *The Software Architect Elevator* (O'Reilly, 2020)
- Andrew Harmel-Law, "Scaling the Practice of Architecture, Conversationally": <https://martinfowler.com/articles/scaling-architecture-conversationally.html>
- Rick Kazman, Mark Klein and Paul Clements, "ATAM: Method for Architecture Evaluation", SEI technical report CMU/SEI-2000-TR-004 (2000)
- Neal Ford, "Architectural Katas": <https://nealford.com/katas/>
- Thoughtworks, "Build your own Technology Radar": <https://www.thoughtworks.com/radar/byor>
- Mark Richards and Neal Ford, *Fundamentals of Software Architecture*, 2nd ed. (O'Reilly, 2025), part III
