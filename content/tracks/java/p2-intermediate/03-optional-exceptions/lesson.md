# Optional and Exceptions

Two questions every method answers, explicitly or by accident: *what if there is no result?*
and *what if something goes wrong?* Java's answers are `Optional` for the first and
exceptions for the second. Used well, a reader knows from the signature what can happen.

## Optional: a result that may be absent

```java
Optional<User> findByEmail(String email);
```

The return type says it: maybe there is a user, maybe not. Compare with returning `null`,
which says nothing and breaks at the first `user.name()` somebody forgets to guard.

### Creating

```java
Optional.of(user);          // must not be null (throws NullPointerException)
Optional.ofNullable(maybe); // empty when maybe is null
Optional.empty();
```

### Consuming without get()

| You want | Write |
| --- | --- |
| A default value | `opt.orElse("guest")` |
| A default that is costly to build | `opt.orElseGet(() -> loadDefault())` |
| An exception when absent | `opt.orElseThrow(() -> new UserNotFound(email))` |
| To transform the value | `opt.map(User::name)` |
| To chain another lookup | `opt.flatMap(User::manager)` (when `manager()` returns `Optional`) |
| To keep it only if it matches | `opt.filter(User::active)` |
| To act on it | `opt.ifPresent(u -> send(u))`, `opt.ifPresentOrElse(..., ...)` |
| Another Optional when empty | `opt.or(() -> findByPhone(phone))` |

```java
String city = repository.findByEmail(email)
        .flatMap(User::address)
        .map(Address::city)
        .orElse("unknown");
```

> [!WARNING]
> `opt.get()` throws `NoSuchElementException` when empty; it is `null` checking with extra
> steps. `orElseThrow()` without arguments does the same but says what it does. Prefer the
> methods above, and `isPresent()` + `get()` only when nothing else fits.

### Where Optional belongs

- **Yes:** return types of methods that may find nothing.
- **No:** fields, method parameters, collections (`Optional<List<T>>`: return an empty list
  instead) and map values. For primitives use `OptionalInt`, `OptionalLong`, `OptionalDouble`.

## Exceptions

### The hierarchy

```text
Throwable
├── Error                      (OutOfMemoryError, StackOverflowError: do not catch)
└── Exception                  checked: the compiler forces you to handle or declare it
    ├── IOException, SQLException, ...
    └── RuntimeException       unchecked: no declaration needed
        ├── IllegalArgumentException, IllegalStateException
        ├── NullPointerException, IndexOutOfBoundsException
        └── UnsupportedOperationException, NumberFormatException, ...
```

- **Checked** exceptions (`Exception` but not `RuntimeException`) must be caught or declared
  with `throws`. They suit **recoverable** problems the caller is expected to handle: a file
  that is missing, a network that is down.
- **Unchecked** exceptions report **programming errors** or broken preconditions: a bad
  argument (`IllegalArgumentException`), a call at the wrong time (`IllegalStateException`).

Modern Java code, and most frameworks, lean towards unchecked exceptions: checked ones do
not compose with lambdas and streams, and they leak implementation details up the stack.

### Throwing well

```java
public Account withdraw(long cents) {
    if (cents <= 0) {
        throw new IllegalArgumentException("amount must be positive: " + cents);
    }
    if (cents > balance) {
        throw new InsufficientFundsException(id, cents, balance);
    }
    ...
}
```

- Validate arguments **first** and fail fast with a message that has the actual values.
- Use the standard exceptions when they fit; `Objects.requireNonNull(x, "x")` for nulls.
- Create your **own exception** when callers need to react to that specific case, and give
  it fields, not only a message:

```java
public class InsufficientFundsException extends RuntimeException {
    private final long requested;
    private final long available;

    public InsufficientFundsException(String account, long requested, long available) {
        super("account %s: requested %d, available %d".formatted(account, requested, available));
        this.requested = requested;
        this.available = available;
    }

    public long requested() { return requested; }
    public long available() { return available; }
}
```

### Catching well

```java
try {
    return Integer.parseInt(text.strip());
} catch (NumberFormatException e) {
    throw new InvalidInputException("line " + line + ": not a number: " + text, e);
}
```

- Catch the **most specific** type you can handle. Catching `Exception` hides bugs.
- When you translate an exception, pass the original as the **cause** (the `e` argument):
  the stack trace keeps both.
- Multi-catch handles several types the same way: `catch (IOException | TimeoutException e)`.
- Never swallow: an empty `catch` turns a failure into wrong data later. If ignoring is
  really right, say so: `catch (InterruptedException _) { Thread.currentThread().interrupt(); }`.

### try-with-resources

Anything that implements `AutoCloseable` (files, sockets, database connections, locks you
wrap) is closed automatically, even when an exception is thrown:

```java
try (var reader = Files.newBufferedReader(path);
     var writer = Files.newBufferedWriter(target)) {
    reader.lines().map(String::toUpperCase).forEach(line -> write(writer, line));
}
```

Resources close in reverse order. If both the body and `close()` throw, the body's exception
wins and the other is attached as *suppressed* (`e.getSuppressed()`). There is no reason to
write `finally { reader.close(); }` any more.

## Optional or exception?

| Situation | Use |
| --- | --- |
| Not finding something is a normal outcome | `Optional` |
| The caller broke a rule (bad argument, wrong state) | unchecked exception |
| An expected failure the caller should handle and recover from | a specific exception (often your own) |
| You want every outcome as a value, e.g. many results | a sealed result type (`Ok` / `Failed`) |

## Summary

- Return `Optional<T>` when a result may be missing; consume it with `orElse`, `map`,
  `flatMap`, `orElseThrow`, never with a blind `get()`.
- Unchecked exceptions for broken preconditions, checked or custom ones for recoverable
  failures; validate first and put the values in the message.
- Catch specifically, keep the cause, never swallow.
- Close resources with try-with-resources.
