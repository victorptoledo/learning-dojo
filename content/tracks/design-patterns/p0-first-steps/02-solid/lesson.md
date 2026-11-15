# SOLID Without the Slogans

SOLID is five principles that Robert C. Martin collected around 2000 for object-oriented
design. They are often recited as slogans and then ignored. They work much better as
**questions you ask of a piece of code**.

## S: Single responsibility

> A class should have one reason to change.

"One reason" does not mean "one method". It means one **actor**: one group of people whose
requests change the code. A `Payroll` class that computes pay (finance's rules), formats the
payslip (HR's layout) and saves it (the DBA's schema) has three reasons to change. A change
for one actor risks breaking the others, and three teams end up editing the same file.

Ask: *who would ask me to change this, and would they ever ask at the same time?* Split along
those lines. In the first kata of this track, a report that fetches, totals and formats
becomes three pieces, one per reason.

## O: Open/closed

> Software should be open for extension and closed for modification.

When a new case arrives (a carrier, a payment method, a file format), can you add it **by
adding code** instead of editing a tested method? The classic symptom of a violation is a
`switch` or `if/else` chain on a type code that grows with every new case, often repeated in
several places.

The fix is to find the axis that varies and put an abstraction there: an interface with one
implementation per case (strategy), or a sealed hierarchy when the set of cases is closed and
you want the compiler to check every `switch`. Be careful: you cannot be closed against every
change. Pick the axis that has actually changed, usually after the second or third time.

## L: Liskov substitution

> Objects of a subtype must be usable wherever the supertype is expected, without surprises.

Barbara Liskov's rule is about **behaviour**, not signatures. A subclass that compiles but
throws `UnsupportedOperationException`, ignores a call, demands stronger input or returns
weaker guarantees breaks every caller that trusted the supertype.

The textbook case: `Square extends Rectangle`. Setting the width of a square also changes its
height, so code that sets width 2 and height 5 and expects area 10 gets 25. In Java you see
the real-world version every day: `List.of(1, 2).add(3)` throws, because immutable lists
implement a `List` interface whose `add` is optional. Signs of a violation:

- callers write `if (x instanceof Special)` before using a supertype;
- overrides that do nothing or throw;
- documentation that says "do not call this on subtype X".

Fixes: do not inherit (compose instead), split the interface so the subtype only promises what
it can do, or model the variants as a sealed set of records.

## I: Interface segregation

> Clients should not depend on methods they do not use.

A `UserService` interface with 40 methods forces every fake in every test to implement 40
methods, and couples a read-only report to the code that deletes users. Prefer small,
role-shaped interfaces defined by what the **client** needs: `UserLookup`, `UserRegistration`.
One class can implement several. Java's `java.util.function` interfaces are the extreme case:
one method each, which is why lambdas fit them.

## D: Dependency inversion

> High-level policy should not depend on low-level detail. Both should depend on abstractions.

A `TransferService` that calls `new JdbcAccountDao()` and `LocalDate.now()` cannot be tested
without a database and a real clock, and it cannot be reused with another store. Invert it:
the service declares what it needs (an `Accounts` interface, a `Clock`) and receives it in its
constructor. The **abstraction belongs to the high-level side**: it is shaped by what the
policy needs, not by what the database offers. Phase 3 builds ports and adapters on this
idea; Spring's constructor injection is the everyday tool that wires it.

Dependency inversion is not "add an interface to every class". Depend on abstractions at the
boundaries that matter: I/O, time, randomness, third-party services and anything you need to
replace in a test.

## Reading code with SOLID

| Question | Principle | Common fix |
| --- | --- | --- |
| Who asks for changes to this class? | S | split by actor |
| What do I edit to add a new case? | O | strategy, sealed types |
| Can every subtype be used where the supertype is expected? | L | composition, smaller interfaces |
| Does this client need all these methods? | I | role interfaces |
| Can I test this without a database, a clock or a network? | D | inject abstractions |

## The limits

SOLID principles are heuristics, and they pull against each other and against simplicity. Ten
one-method classes can be harder to follow than one clear 80-line class. Apply them where
change and testing hurt, measure by "how hard was the last change?", and keep YAGNI (next
phase) in mind.

## Where Spring and the JDK apply them

- **S**: Spring separates `@Controller`, `@Service` and `@Repository` roles.
- **O**: `HandlerInterceptor`, `Converter`, `HttpMessageConverter`: you add behaviour by
  registering beans, not by editing the framework.
- **L**: the `Collection` contracts are the cautionary tale: optional operations throw.
- **I**: `Runnable`, `Supplier`, `Comparator`: tiny interfaces everywhere.
- **D**: constructor injection; `java.time.Clock` exists so that code can take time as a
  dependency.

## Further reading

- Wikipedia, "SOLID": <https://en.wikipedia.org/wiki/SOLID>
- Robert C. Martin, "The Single Responsibility Principle": <https://blog.cleancoder.com/uncle-bob/2014/05/08/SingleReponsibilityPrinciple.html>
- Wikipedia, "Liskov substitution principle": <https://en.wikipedia.org/wiki/Liskov_substitution_principle>
- roadmap.sh topic "SOLID": <https://github.com/nilbuild/developer-roadmap/blob/master/roadmaps/software-design-architecture/content/solid@3XckqZA--knUb8IYKOeVy.md>
- roadmap.sh topic "Program Against Abstractions": <https://github.com/nilbuild/developer-roadmap/blob/master/roadmaps/software-design-architecture/content/program-against-abstractions@UZeY36dABmULhsHPhlzn_.md>
- Dave Cheney, "SOLID Go Design" (the principles outside Java): <https://dave.cheney.net/2016/08/20/solid-go-design>
