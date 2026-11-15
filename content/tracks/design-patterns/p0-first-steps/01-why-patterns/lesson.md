# What Design Patterns Are (and Are Not)

A **design pattern** is a named, reusable answer to a problem that keeps coming back when you
design software. The name is the most useful part: "put a decorator around the client"
says in four words what would otherwise take a paragraph and a whiteboard.

A pattern is not a piece of code you copy. It is a description of:

| Part | Question it answers |
| --- | --- |
| **Problem** | What situation are you in? ("the algorithm varies by customer type") |
| **Forces** | What pulls in different directions? (new types arrive often; callers must not change) |
| **Solution** | What arrangement of responsibilities resolves the forces? |
| **Consequences** | What does it cost? (more types, indirection, a new place to look) |

If you remember only the class diagram, you will apply the pattern where the problem is
absent. If you remember the problem and the forces, you will recognise it in any language.

## Where they came from

The idea comes from the architect Christopher Alexander, who catalogued recurring solutions
for buildings and towns. In 1994 Gamma, Helm, Johnson and Vlissides (the "Gang of Four", GoF)
published *Design Patterns: Elements of Reusable Object-Oriented Software* with 23 patterns
drawn from real C++ and Smalltalk systems. Martin Fowler's *Patterns of Enterprise Application
Architecture* (2002) added the patterns of business applications: repository, unit of work,
data mapper, service layer. Eric Evans' *Domain-Driven Design* (2003) added specification and
the anti-corruption layer. This track covers the ones a backend developer meets every week.

Two ideas from the GoF book matter more than any single pattern:

- **Program to an interface, not an implementation.** Callers depend on what an object does,
  not on which class it is.
- **Favour object composition over class inheritance.** Build behaviour by combining small
  objects, not by growing deep subclass trees.

## The three families

| Family | Concern | Patterns you will meet here |
| --- | --- | --- |
| **Creational** | how objects are created, so callers do not depend on concrete classes | factory method, abstract factory, builder, singleton, prototype |
| **Structural** | how objects are composed into larger structures | adapter, decorator, proxy, facade, composite, bridge, flyweight |
| **Behavioural** | how objects share work and communicate | strategy, observer, command, chain of responsibility, state, visitor, template method, iterator, mediator, memento |

Beyond the GoF: **application patterns** (repository, unit of work, dependency injection,
specification, ports and adapters, anti-corruption layer) organise a whole service, and
**anti-patterns** (god class, singleton abuse, service locator) are recurring *bad* answers
with a name, so you can spot and undo them.

## Modern Java changed the code, not the problems

Several GoF patterns exist because C++ and Java 1.0 lacked a feature. When the language gains
the feature, the pattern shrinks to a line or two:

| Pattern | What it needed in 1994 | In Java 25 |
| --- | --- | --- |
| Strategy, Command | one interface plus a class per variant | a functional interface and a lambda |
| Iterator | a hand-written iterator class | `Iterable`, the enhanced `for`, streams |
| Visitor | double dispatch over a class hierarchy | a `sealed` interface, records and an exhaustive `switch` |
| Template method | an abstract class with hook methods | often a method that takes a function |
| Value objects | hand-written `equals`, `hashCode`, `toString` | `record` |
| Singleton | a private constructor and a static instance | an `enum`, or one instance managed by a DI container |

The *problem* did not go away: you still need to vary an algorithm, walk a structure, or add
an operation to a closed set of types. You just solve it with less ceremony. And many
patterns, such as adapter, facade, decorator, repository and ports and adapters, are about
**where responsibilities live**. No syntax removes them.

## Patterns you already use

You meet patterns every day in the JDK and Spring, often without the name:

| You write | Pattern |
| --- | --- |
| `list.sort(Comparator.comparing(User::name))` | strategy |
| `new BufferedReader(new InputStreamReader(in))` | decorator, adapter |
| `List.of(...)`, `Optional.of(...)`, `LocalDate.of(...)` | static factory method |
| `HttpClient.newBuilder()...build()` | builder |
| `@Transactional` on a Spring bean | proxy |
| `JdbcTemplate.query(sql, rowMapper)` | template method with a callback |
| `ApplicationEventPublisher.publishEvent(...)` | observer |
| Spring Data `interface UserRepository extends JpaRepository<...>` | repository |

## When not to use a pattern

Patterns add indirection. Indirection pays when the thing it isolates actually varies or is
actually hard to test. Signs you are over-using them:

- An interface with exactly one implementation that nobody plans to replace or fake.
- A factory that only calls one constructor.
- A strategy chosen once at startup and never again, with one strategy.
- Class names that are a list of patterns (`AbstractConfigurableHandlerFactoryProvider`).

Start with the simplest code that works, keep it tested, and introduce a pattern when the
forces show up: a second variant arrives, a dependency gets in the way of a test, a condition
starts repeating. Refactoring *toward* a pattern is usually better than designing one in
up front. Every kata in this track works that way: you get working, messy code and refactor
it until a pattern appears.

## Further reading

- Refactoring.Guru, "Design Patterns" (catalogue with problems, structure and examples): <https://refactoring.guru/design-patterns/catalog>
- Wikipedia, "Design Patterns" (the GoF book, its families and its criticism): <https://en.wikipedia.org/wiki/Design_Patterns>
- roadmap.sh, Software Design and Architecture roadmap: <https://roadmap.sh/software-design-architecture>
- roadmap.sh topic "GoF Design Patterns": <https://github.com/nilbuild/developer-roadmap/blob/master/roadmaps/software-design-architecture/content/gof-design-patterns@hlHl00ELlK9YdnzHDGnEW.md>
- Martin Fowler, "Is Design Dead?" (patterns, evolutionary design and when to add them): <https://martinfowler.com/articles/designDead.html>
- Martin Fowler, "Design Stamina Hypothesis": <https://martinfowler.com/bliki/DesignStaminaHypothesis.html>
