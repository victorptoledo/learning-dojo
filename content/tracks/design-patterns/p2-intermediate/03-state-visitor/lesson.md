# State, Visitor, Composite and Template Method with Sealed Types

Java 17 to 21 added **sealed** classes and interfaces, **records**, **pattern matching for
switch** and **record patterns**. Together they change how you write a whole group of
patterns that deal with a **closed set of types**.

```java
sealed interface Shape permits Circle, Rect {}
record Circle(double r) implements Shape {}
record Rect(double w, double h) implements Shape {}

double area(Shape shape) {
    return switch (shape) {
        case Circle(double r) -> Math.PI * r * r;
        case Rect(double w, double h) -> w * h;
    };                                   // no default: the compiler checks every case
}
```

If someone adds `Triangle` to `permits`, every switch without a `default` stops compiling
until it handles the new case. That check is what makes the following patterns safe.

## State

**Problem.** An object behaves differently depending on its state (an order: created, paid,
shipped, delivered, cancelled), and the code is full of `if (status.equals("PAID"))` checks,
fields that only make sense in one state, and transitions anyone can break.

**Classic solution.** One class per state implementing the same interface; the context
delegates to its current state, and each state returns the next.

**Java 25 solution.** A sealed interface of **records**. Each record carries only the data of
its state (only `Shipped` has a tracking number; only `Cancelled` has a refund), overrides the
transitions it allows, and inherits a default that refuses the rest:

```java
sealed interface OrderState permits Created, Paid, Shipped, Delivered, Cancelled {
    String name();
    default OrderState ship(String tracking) { throw refused("ship"); }
    // pay(), deliver(), cancel() likewise
}
record Paid(long totalCents) implements OrderState {
    public String name() { return "paid"; }
    public OrderState ship(String tracking) { return new Shipped(tracking); }
    public OrderState cancel(String reason) { return new Cancelled(reason, totalCents); }
}
```

Transitions return **new** immutable states, so a history is just a list, and an impossible
state ("shipped without tracking") cannot be built. Behaviour that is not a transition (a
description, a report line) is often clearer as one exhaustive `switch` over the states. Enums
work for state machines whose states carry no data.

## Visitor

**Problem.** You have a stable set of types (an AST, a document model, a set of shapes) and
keep adding **operations** over it (render, count, validate, export), and you do not want to
add a method to every class for each operation.

**Classic solution.** Double dispatch: each type has `accept(Visitor v) { v.visit(this); }`
and each operation is a visitor class with one `visit` method per type. Lots of ceremony, and
the hierarchy must know about visitors.

**Java 25 solution.** An exhaustive `switch` with record patterns *is* the visitor: one
function per operation, the compiler checks that every type is handled, and the types know
nothing about it. Use the classic visitor only where the hierarchy is not sealed or you cannot
use modern Java. The JDK still uses classic visitors in places, such as `FileVisitor` for
`Files.walkFileTree` and the compiler tree API.

### The expression problem

Two ways to organise a closed hierarchy, with opposite costs:

| | New **type** | New **operation** |
| --- | --- | --- |
| Methods on each type (polymorphism, State) | add one class, touch nothing else | edit every class |
| Switch over sealed types (Visitor) | edit every switch (the compiler lists them) | add one function |

Pick by what changes more often. Operations change often in compilers, document models and
specifications; types change often in plugins and strategies. You can mix: transitions as
methods (they belong to each state), reports as switches.

## Composite

**Problem.** Treat individual objects and groups of them the same way: a file and a folder,
an item and a bundle, a command and a macro, a condition and `and(a, b)`.

**Solution.** The group implements the same interface as the items and holds a list of
children. With sealed records the composite is a recursive data type:

```java
sealed interface Node permits FileNode, Folder {}
record FileNode(String name, long bytes) implements Node {}
record Folder(String name, List<Node> children) implements Node {}

long size(Node node) {
    return switch (node) {
        case FileNode f -> f.bytes();
        case Folder(String n, List<Node> children) -> children.stream().mapToLong(this::size).sum();
    };
}
```

JDK and Spring composites: AWT/Swing `Container`, `CompositeMeterRegistry`, Spring's
`CompositeCacheManager`. The specification kata in phase 3 is a composite of conditions.

## Template method

**Problem.** Several algorithms share the same skeleton and differ in a few steps.

**Classic solution.** An abstract class with a `final` method that defines the skeleton and
calls abstract "hook" methods that subclasses implement.

**Modern view.** This is inheritance-based, with all the costs of the previous phase. Most
of the time pass the varying step as a **function** instead (a strategy): Spring's
`JdbcTemplate.query(sql, rowMapper)` and `TransactionTemplate.execute(callback)` keep the
skeleton (get a connection, run, map, translate exceptions, release) and take the varying part
as a callback. `AbstractList` (implement `get` and `size`, get the rest) and Spring Batch's
`AbstractItemReader` are template methods that still earn their keep, because they come with a
lot of shared behaviour.

## Iterator

Walk a collection without exposing its structure. In Java, implement `Iterable<T>` and the
enhanced `for` and streams work; write the iterator only when the traversal is unusual (a
tree in order, paging through an API). Generators from other languages are usually a `Stream`
or `Spliterator` in Java.

## Further reading

- JEP 409, "Sealed Classes": <https://openjdk.org/jeps/409>
- JEP 441, "Pattern Matching for switch": <https://openjdk.org/jeps/441>
- JEP 440, "Record Patterns": <https://openjdk.org/jeps/440>
- Refactoring.Guru, "State": <https://refactoring.guru/design-patterns/state>
- Refactoring.Guru, "Visitor": <https://refactoring.guru/design-patterns/visitor>
- Refactoring.Guru, "Composite": <https://refactoring.guru/design-patterns/composite>
- Refactoring.Guru, "Template Method": <https://refactoring.guru/design-patterns/template-method>
- Wikipedia, "Expression problem": <https://en.wikipedia.org/wiki/Expression_problem>
