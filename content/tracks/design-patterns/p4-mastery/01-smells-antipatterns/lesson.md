# Code Smells, Anti-Patterns and Refactoring Moves

A **code smell** (a term from Kent Beck and Martin Fowler) is a surface sign that the design
probably has a problem. It is a hint, not a verdict. An **anti-pattern** is a recurring
solution that looks reasonable and causes more trouble than it solves, with a name so teams
can talk about it. A **refactoring** is a small change to the structure of code that does not
change its behaviour; many small ones in a row make a big redesign safe.

## The safety net comes first

You cannot refactor what you cannot check. With legacy code:

1. **Characterisation tests** pin what the code does today, right or wrong. Run it with
   realistic inputs, record the outputs, assert them. ("Golden master" or approval tests do
   this at scale.) Every kata in this track gave you such tests.
2. Find or make a **seam**: a place where you can change behaviour without editing the code
   around it (a constructor parameter, an interface, an overridable method).
3. Take **small steps**, run the tests after each, commit often. Your IDE's automated
   refactorings (rename, extract method, inline, move, change signature) are safer than
   editing by hand.
4. Do not fix bugs while refactoring. Note them, keep the behaviour, fix them in a separate
   commit with their own test.

## Smells and their moves

| Smell | What you see | Typical moves |
| --- | --- | --- |
| **Long method** | a method you must scroll; comments that label sections | Extract Function; Replace Temp with Query; Decompose Conditional |
| **Large class / god class** | thousands of lines, many fields used by few methods each | Extract Class; Move Function; split by reason to change |
| **Switch statements** on a type code | the same `switch` or `if` chain in several places | Replace Conditional with Polymorphism; Replace Type Code with Subclasses (or an enum, or sealed records) |
| **Primitive obsession** | money as `long`, ids and statuses as `String`, arrays as records | Replace Primitive with Object; Introduce Parameter Object; records |
| **Data clumps** | the same three parameters travel together everywhere | Introduce Parameter Object; Extract Class |
| **Long parameter list** | seven parameters, booleans among them | Introduce Parameter Object; builder; split the function |
| **Feature envy** | a method that mostly uses another object's data | Move Function to that object |
| **Shotgun surgery** | one change needs edits in ten files | Move Function; Combine Functions into Class |
| **Divergent change** | one class changes for many unrelated reasons | Extract Class; split by actor |
| **Duplicated code** | the same knowledge in several places | Extract Function; Pull Up; but check it is the same knowledge |
| **Message chains** | `a.getB().getC().getD()` | Hide Delegate; tell, don't ask |
| **Speculative generality** | hooks, parameters and interfaces nobody uses | Collapse Hierarchy; Inline Class; remove |
| **Comments** explaining what code does | `// calculate the discount` above 40 lines | Extract Function with a name that says it |
| **Mutable data** shared widely | setters called from everywhere | Encapsulate; records; immutable copies |

## Anti-patterns

| Anti-pattern | Description | Way out |
| --- | --- | --- |
| **God class** (blob) | one class knows and does everything | extract cohesive classes; move behaviour to the data it uses |
| **Anemic domain model** | entities are bags of getters and setters; all logic is in services | move rules into the objects (when the domain is complex enough to need it) |
| **Singleton abuse** | global access to mutable state | inject one instance |
| **Service locator** | dependencies pulled from a registry at runtime | constructor injection |
| **Golden hammer** | the same pattern or tool for every problem ("everything is an event") | choose by forces |
| **Pattern fever** | patterns added for their own sake; a factory of factories | remove until something hurts |
| **Lava flow** | dead code nobody dares delete | tests, then delete; version control remembers |
| **Poltergeist** | classes that only pass a call on and vanish | inline them |
| **Copy-paste programming** | fixes applied to one copy only | extract the shared knowledge |
| **Magic numbers and strings** | `if (type.equals("guest"))`, `* 1.23` | named constants, enums, value objects |

On the anemic model, be precise: for CRUD over simple data, services with plain records are
fine and often better. It is an anti-pattern when complex rules are scattered over services
that all poke the same setters, so invariants cannot be enforced in one place.

## How smells lead to patterns

Many patterns in this track are where a refactoring ends:

- switch on type code → **strategy** or **state** (or sealed records with a switch);
- constructor with nine parameters → **builder** or **parameter object**;
- class that calls a vendor SDK everywhere → **adapter**, **anti-corruption layer**;
- logging and retries copied into every method → **decorator** or **proxy**;
- finders multiplying → **specification**;
- a service that news up its dependencies → **dependency injection**.

Joshua Kerievsky's *Refactoring to Patterns* describes these paths. The reverse also exists:
inline a pattern that no longer pays for itself.

## In the Java ecosystem

IntelliJ IDEA and Eclipse automate most moves in Fowler's catalogue; SonarQube, PMD and
Error Prone detect many smells (cognitive complexity, long methods, duplicated blocks); ArchUnit
keeps the structure from rotting again. Tools find candidates; judgement decides.

## Further reading

- Refactoring catalogue (Martin Fowler, second edition): <https://refactoring.com/catalog/>
- Refactoring.Guru, "Code Smells": <https://refactoring.guru/refactoring/smells>
- Refactoring.Guru, "Refactoring techniques" catalogue: <https://refactoring.guru/refactoring/catalog>
- Martin Fowler, "Code Smell": <https://martinfowler.com/bliki/CodeSmell.html>
- Martin Fowler, "Anemic Domain Model": <https://martinfowler.com/bliki/AnemicDomainModel.html>
- Wikipedia, "Anti-pattern": <https://en.wikipedia.org/wiki/Anti-pattern>
- roadmap.sh topic "Anemic Models": <https://github.com/nilbuild/developer-roadmap/blob/master/roadmaps/software-design-architecture/content/anemic-models@nVaoI4IDPVEsdtFcjGNRw.md>
- roadmap.sh topic "Keep Methods, Classes and Files Small": <https://github.com/nilbuild/developer-roadmap/blob/master/roadmaps/software-design-architecture/content/keep-methods--classes--files-small@XEwC6Fyf2DNNHQsoGTrQj.md>
