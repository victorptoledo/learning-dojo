# Composition, Coupling and Cohesion

Before the patterns, the forces they balance. Almost every pattern in this track exists to
lower **coupling**, raise **cohesion**, or replace **inheritance** with **composition**.

## Composition over inheritance

Inheritance says "B **is an** A and gets all of A". Composition says "B **has** an A and asks
it for help". Inheritance is the tighter bond:

| Problem | What happens |
| --- | --- |
| **Fragile base class** | a change in the parent (a new call order, a new method) breaks children you never looked at |
| **Combinatorial explosion** | one subclass per combination: `LoggingRetryingHttpClient`, `RetryingCachingHttpClient` ... |
| **Fixed at compile time** | you cannot add or remove behaviour at runtime or per instance |
| **Leaky** | children see protected state and depend on how the parent works, not on what it promises |

A classic trap from the JDK: `class CountingSet extends HashSet` overriding `add` and
`addAll` to count insertions counts twice, because `HashSet.addAll` happens to call `add`. The
subclass depends on an implementation detail. A **wrapper** that holds a `Set` and forwards
calls has no such problem, and works with any `Set`.

Composition gives you the structural patterns of the next lessons: **decorator** (wrap and
add), **adapter** (wrap and translate), **strategy** (hold a replaceable algorithm), and
**bridge** (two independent hierarchies, such as shape and renderer, joined by a field instead
of multiplied by subclasses).

Inheritance still has its place: a true "is a" relation with a stable parent, framework hook
classes designed for it, and sealed hierarchies of variants. In Java, make classes `final`
unless you designed them to be extended; records and enums already are.

## Coupling

Coupling is how much one module must know about another to work. Less is better, because
every piece of knowledge is a reason to change together. From tightest to loosest:

| Kind | Example |
| --- | --- |
| **Content** | reaching into another class's internals (reflection, shared mutable fields) |
| **Common / global** | several classes reading and writing a static map or singleton |
| **Control** | passing a flag that tells the callee what to do (`save(order, true, false)`) |
| **Stamp** | passing a whole object when the callee needs one field |
| **Data** | passing just the values needed |
| **Message** | calling a method on an interface; knowing only what it does |

Signs of tight coupling: you cannot test a class without starting half the system; a change in
one file forces edits in five; you need to know the order in which to call methods.

### The Law of Demeter

"Only talk to your immediate friends": a method should call methods of its own fields, its
parameters, objects it creates, and itself, not of objects returned by them.
`order.getCustomer().getAddress().getCountry().getVatRate()` couples the caller to four
classes and their navigation. Prefer asking the object that has the knowledge:
`order.vatRate()`. This is the "tell, don't ask" idea. It does not forbid fluent chains on the
**same** kind of object (`stream.filter(...).map(...)`, a builder): those are not navigation.

## Cohesion

Cohesion is how strongly the parts of a module belong together. High cohesion: everything in
the class works on the same data toward one purpose. Low cohesion: a `Utils` class, a
`Manager` with unrelated methods, fields used by only one method each. High cohesion and low
coupling go together: when related things live in one place, fewer places need to talk.

A quick test: describe the class in one sentence without "and". If the fields split into
groups that different methods use, you probably have two classes.

## DRY, KISS and YAGNI, and their limits

- **DRY, don't repeat yourself**: every piece of *knowledge* should have one authoritative
  place. A VAT rate written in five places will be changed in four.
- **KISS, keep it simple**: the simplest design that solves today's problem is the easiest
  to change tomorrow.
- **YAGNI, you aren't gonna need it**: do not build for a requirement you only imagine. The
  cost is not only writing it but carrying and changing it.

The limit of DRY: it is about knowledge, not about text. Two functions that look the same but
change for different reasons (a shipping weight limit and a pricing tier that both happen to
be 30) are **coincidental** duplication. Merging them couples two things that should change
apart, and the shared helper soon grows flags to serve both: control coupling. A common rule
of thumb is to wait for the third occurrence before extracting ("rule of three"), and to
prefer duplication over the wrong abstraction.

The limit of YAGNI: it is not an excuse to skip design that keeps code changeable. Tests,
clear names and small functions are not speculative features.

## Where Spring and the JDK show these ideas

- `Collections.unmodifiableList`, `Collections.synchronizedMap`: composition by wrapping,
  not by subclassing `ArrayList`.
- `java.io` streams: decorators composed at runtime instead of a subclass per combination.
- Spring beans are composed by injection; Spring's own extension points are interfaces you
  implement, not classes you extend.
- Records and enums are final: the language nudges you toward composition.

## Further reading

- Wikipedia, "Composition over inheritance": <https://en.wikipedia.org/wiki/Composition_over_inheritance>
- roadmap.sh topic "Composition over Inheritance": <https://github.com/nilbuild/developer-roadmap/blob/master/roadmaps/software-design-architecture/content/composition-over-inheritance@Izno7xX7wDvwPEg7f_d1Y.md>
- roadmap.sh topic "Coupling and Cohesion": <https://github.com/nilbuild/developer-roadmap/blob/master/roadmaps/software-design-architecture/content/coupling-and-cohesion@TXus3R5vVQDBeBag6B5qs.md>
- Wikipedia, "Law of Demeter": <https://en.wikipedia.org/wiki/Law_of_Demeter>
- Martin Fowler, "Tell Don't Ask": <https://martinfowler.com/bliki/TellDontAsk.html>
- Martin Fowler, "Yagni": <https://martinfowler.com/bliki/Yagni.html>
- Refactoring.Guru, "Bridge": <https://refactoring.guru/design-patterns/bridge>
