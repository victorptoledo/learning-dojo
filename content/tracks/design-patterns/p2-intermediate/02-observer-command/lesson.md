# Observer, Command and Chain of Responsibility

Three behavioural patterns about **who calls whom**. Observer reverses a dependency so the
caller does not know its listeners; Command turns a call into an object; Chain of
Responsibility passes a request along a line of handlers.

## Observer

**Problem.** When something happens in one object (an order is placed, a quote is priced),
several others must react, and the list keeps growing. If the subject calls each of them, it
depends on all of them.

**Solution.** The subject keeps a list of **listeners** behind one interface and notifies
them; listeners **subscribe** themselves.

```java
@FunctionalInterface
interface QuoteListener { void quoted(QuoteRequest request, Quote quote); }

Subscription subscribe(QuoteListener listener) {
    listeners.add(listener);
    return () -> listeners.remove(listener);
}
```

Design questions every observer implementation must answer:

| Question | Typical answer |
| --- | --- |
| How do listeners leave? | return a subscription handle (`AutoCloseable`); forgotten listeners leak memory |
| What if one throws? | catch, count or log, and carry on: one listener must not break the others or the subject |
| Can listeners subscribe during a notification? | iterate over a copy, or use `CopyOnWriteArrayList` |
| Synchronous or asynchronous? | sync is simple and ordered; async needs an executor and error handling |
| Push or pull? | push the facts in an immutable event (a record), so listeners need not call back |

**In Spring**, `ApplicationEventPublisher.publishEvent(new OrderPlaced(...))` and
`@EventListener` methods are observer with the container doing the wiring;
`@TransactionalEventListener(phase = AFTER_COMMIT)` runs a listener only if the transaction
commits. **In the JDK**, Swing and JavaFX listeners, `PropertyChangeListener`, and
`java.util.concurrent.Flow` (reactive streams: observer plus back-pressure). The old
`java.util.Observable` is deprecated: it forced inheritance.

Observer's cost: control flow becomes implicit. Reading `place()` no longer tells you what
happens next. Keep events few and named after facts (`OrderPlaced`), and do not chain
listeners that publish events that trigger listeners. Across services, in-process events are
not enough; you need a broker and an outbox (see the System Design track).

## Command

**Problem.** You want to treat a request as a **thing**: queue it, log it, retry it, send it
elsewhere, or undo it.

**Solution.** An object that captures everything needed to perform an action, with a method
to execute it (and often one to undo it). An **invoker** runs commands without knowing what
they do.

```java
interface Command {
    void execute(Document document);
    void undo(Document document);
    String describe();
}
```

An editor keeps two stacks: run a command and push it on *done*; undo pops it, undoes it and
pushes it on *undone*; redo does the reverse; a new command clears *undone*. A **macro** is a
command made of commands (a composite), and it must undo the steps it already ran if one fails.

Modern Java: a command with only `execute` and no undo is a `Runnable`, `Callable` or
`Consumer`. `ExecutorService.submit(task)` is the invoker of the command pattern; so is a
job queue. Records are good commands when the data is all they carry (`record Insert(int at,
String text)`); undo that must remember what it removed needs state, so a class.

**Memento** is the companion for undo when commands cannot reverse themselves: capture the
state before (a snapshot), restore it to undo. Snapshots are simple but cost memory; reversible
commands are cheaper but harder. The editor kata uses reversible commands and one memento-like
command (replace-all keeps the text before).

## Chain of responsibility and pipelines

**Problem.** A request must pass through several handlers (authentication, validation,
rate limiting, logging), each of which may handle it, change it, or stop it, and the set and
order of handlers should be configurable.

**Solution.** Handlers share an interface and each knows the next (or a runner calls them in
order).

- **Classic chain**: each handler decides whether to pass on. Servlet `Filter` and
  `FilterChain.doFilter`, Spring Security's filter chain, Spring MVC `HandlerInterceptor`,
  `java.util.logging.Logger` parents.
- **Pipeline**: every step runs and transforms the value: `UnaryOperator<Quote>` steps
  folded in order, `Function.andThen`, stream operations, the quote engine in this phase's
  checkpoint.

Modern Java makes steps lambdas, so inserting a "black friday" rule is one list element.

## Mediator

When many objects talk to each other in all directions, a **mediator** centralises the
conversation: each colleague talks only to the mediator. A form controller that enables the
"submit" button when three fields are valid is a mediator; so is a chat room. Spring's
`ApplicationEventPublisher` is often used as a mediator. Watch that the mediator does not
become a god class.

## Further reading

- Refactoring.Guru, "Observer": <https://refactoring.guru/design-patterns/observer>
- Refactoring.Guru, "Command": <https://refactoring.guru/design-patterns/command>
- Refactoring.Guru, "Chain of Responsibility": <https://refactoring.guru/design-patterns/chain-of-responsibility>
- Refactoring.Guru, "Memento": <https://refactoring.guru/design-patterns/memento>
- Refactoring.Guru, "Mediator": <https://refactoring.guru/design-patterns/mediator>
- Spring Framework reference, "Additional Capabilities of the ApplicationContext" (standard and custom events): <https://docs.spring.io/spring-framework/reference/core/beans/context-introduction.html>
- Martin Fowler, "Event Collaboration": <https://martinfowler.com/eaaDev/EventCollaboration.html>
- Java SE 25 API, `java.util.concurrent.Flow`: <https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/concurrent/Flow.html>
