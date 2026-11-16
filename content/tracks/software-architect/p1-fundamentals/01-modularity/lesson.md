# Modularity, Coupling and Cohesion at the Level of Modules

The Design Patterns track's lesson *Composition, Coupling and Cohesion* looks at classes. This
lesson looks one level up, at **modules**: packages, libraries, components, services. The ideas
are the same; the price of getting them wrong is larger, because a module boundary is much more
expensive to move than a class.

## Information hiding: why modules exist

In 1972 David Parnas compared two ways of splitting the same program. The first followed the
**steps** of the processing (read input, shift lines, sort, print): a flowchart turned into
modules. The second gave each module a **design decision to hide**: how lines are stored, how
they are sorted. When a decision changed (store lines compressed, say), the first design needed
changes in every module; the second, in one.

His criterion still holds: **each module hides a decision that is likely to change**, behind an
interface that does not reveal it. A good module has a small interface and a lot of hidden
implementation behind it; John Ousterhout calls this a **deep** module. A module whose interface
is as big as its body (a pass-through, a getter per field) is shallow: it adds a boundary and
hides nothing.

Questions to ask of every module:

- What does it hide? If the answer is "nothing", it is not a module, it is a folder.
- What would change if that decision changed? Only this module, or its users too?
- Could you describe its interface without describing its implementation?

## Connascence: a sharper word for coupling

"Coupled" says that two parts must change together, not how badly. **Connascence** (Meilir
Page-Jones) names the kind of knowledge two parts share, from weak to strong:

| Static kind (visible in the code) | Two parts must agree on ... |
| --- | --- |
| name | the name of a thing (a method, a field) |
| type | the type of a thing |
| meaning (convention) | what a value means (`status == 3` is "shipped") |
| position | the order of values (`new Order(a, b, c, d)`) |
| algorithm | an algorithm (both sides hash passwords the same way) |

| Dynamic kind (visible only at runtime) | Two parts must agree on ... |
| --- | --- |
| execution order | the order of calls (`open` before `write`) |
| timing | when things happen (a timeout, a race) |
| value | values that must change together (an invariant across two records) |
| identity | the very same instance |

Three rules of thumb:

1. **Weaker is better**: turn strong connascence into weaker forms (a magic `3` becomes a named
   enum: meaning becomes name; four positional arguments become a record: position becomes
   name).
2. **Strength must fall with distance**: strong connascence is fine inside a class, worrying
   across modules and dangerous across services owned by different teams.
3. **Locality**: keep the parts that share strong connascence close together, ideally in one
   module.

## Principles for module dependencies

Robert C. Martin collected principles for **components** (the unit you release and deploy):

- **Acyclic dependencies principle (ADP)**: the dependency graph of modules has no cycles. Two
  modules in a cycle must be built, tested and released together: they are one module with two
  names. Break a cycle by **inverting** one arrow (the lower module owns an interface the upper
  one implements), by moving the shared piece into a new module both depend on, or by turning a
  call into an event. The *Break the Cycle* kata does exactly this.
- **Stable dependencies principle (SDP)**: depend in the direction of stability. A module many
  others depend on is hard to change (it is **stable** in this sense, whatever its quality);
  it should not depend on a module that changes often.
- **Stable abstractions principle (SAP)**: a stable module should be abstract (mostly
  interfaces), so it can be extended without being changed; an unstable module can be concrete.

Cohesion has principles too: classes that change together belong in the same module (**common
closure**, the single responsibility principle for modules), and classes used together belong
together (**common reuse**: do not force users to depend on things they do not use).

### Measuring it

For a package:

| Metric | Meaning |
| --- | --- |
| `Ca`, afferent coupling | classes outside that depend on classes inside: who needs me |
| `Ce`, efferent coupling | classes outside that classes inside depend on: whom I need |
| `I = Ce / (Ca + Ce)` | instability: 0 is maximally stable, 1 maximally unstable |
| `A` | abstractness: abstract types / all types in the package |
| `D = abs(A + I - 1)` | distance from the **main sequence**, the line where `A + I = 1` |

Packages near `A = 0, I = 0` sit in the **zone of pain**: concrete and depended on by everything,
so every change hurts (a `common` package full of entities and utilities). Packages near
`A = 1, I = 1` sit in the **zone of uselessness**: abstractions nobody uses. Stable *and*
concrete is fine for things that never change (the JDK's `String`); for your own code it is a
warning. The *Fitness Function Suite* kata in Mastery computes these numbers from compiled
classes.

## Granularity: how big is a module?

Too few modules and nothing is hidden; too many and every change crosses five boundaries. Cut
along **reasons to change**: business capabilities (pricing, shipping) change for business
reasons; technical concerns (logging, the HTTP framework) change for technical ones. The next
lesson compares cutting by layer and cutting by feature.

## Further reading

- David L. Parnas, "On the Criteria To Be Used in Decomposing Systems into Modules" (CACM, 1972): <https://dl.acm.org/doi/10.1145/361598.361623>
- John Ousterhout, *A Philosophy of Software Design*, 2nd ed. (Yaknyam Press, 2021): deep and shallow modules
- connascence.io, a reference of the kinds of connascence: <https://connascence.io/>
- Robert C. Martin, *Clean Architecture* (Prentice Hall, 2017), part IV, "Component Principles"
- Wikipedia, "Software package metrics": <https://en.wikipedia.org/wiki/Software_package_metrics>
- roadmap.sh topic "Coupling and Cohesion": <https://github.com/nilbuild/developer-roadmap/blob/master/roadmaps/software-design-architecture/content/coupling-and-cohesion@TXus3R5vVQDBeBag6B5qs.md>
