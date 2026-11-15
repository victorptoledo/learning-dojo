# Order State

`Order` keeps its status in a `String`. Every method re-checks it with `equals`, `describe()`
is an `if/else` ladder over the same strings, and the fields that only exist in one status
(the tracking number, the cancel reason, the refund) sit in the class whether they apply or
not. A typo in a status is a bug the compiler cannot see.

Refactor it to the **State** pattern, the Java 25 way: a **sealed interface** of **records**,
one per state. Each record carries only the data that exists in that state and overrides only
the transitions it allows; everything else is refused by a default method. `Order` delegates
and keeps the history. `describe()` becomes an exhaustive `switch` over the sealed states.

The visible tests pin today's behaviour and pass before you start: keep them green.

## The target design

```java
public sealed interface OrderState permits Created, Paid, Shipped, Delivered, Cancelled {
    String name();                              // "created", "paid", ...
    default OrderState pay()                    // each default throws
    default OrderState ship(String tracking)    //   IllegalStateException("cannot <action> an order that is <name>")
    default OrderState deliver()
    default OrderState cancel(String reason)
}
```

| Record | Allows |
| --- | --- |
| `Created(long totalCents)` | `pay()` → `Paid(totalCents)`; `cancel(reason)` → `Cancelled(reason, 0)` |
| `Paid(long totalCents)` | `ship(tracking)` → `Shipped(tracking)` (a `null` or blank tracking number throws `IllegalArgumentException`); `cancel(reason)` → `Cancelled(reason, totalCents)` |
| `Shipped(String tracking)` | `deliver()` → `Delivered(tracking)` |
| `Delivered(String tracking)` | nothing |
| `Cancelled(String reason, long refundedCents)` | nothing |

`Order` keeps its constructor and its methods `pay`, `ship`, `deliver`, `cancel`, `status()`
(the state's name in upper case) and `describe()`, and adds:

- `OrderState state()`: the current state;
- `List<OrderState> history()`: every state it has been in, starting with `Created`, as an
  unmodifiable list.

## What the grading tests check

- Every allowed and refused transition, with the old messages; a refused or invalid
  transition leaves the order unchanged.
- The states work on their own: `new Created(300).pay().ship("X").deliver()`.
- `OrderState` is sealed and permits exactly the five records.
- `Order` holds only its id, its state and its history: no status string, no `equals`, no
  `instanceof`, and no `default` branch (the compiler checks a switch over sealed types).
