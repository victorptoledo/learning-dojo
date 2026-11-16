---
from: csharp
status: draft
concept: "Events and handlers"
from_way: "event EventHandler<T> fields, += to subscribe and -= to unsubscribe, handlers called synchronously by Invoke, and leaks from handlers that are never removed."
native_way: "EventEmitter with named events, on to subscribe and off to unsubscribe, listeners called synchronously by emit, and the same leaks with a MaxListeners warning."
sensei_tip: "The leak is the same as a forgotten -= on a long-lived publisher. Node.js at least warns you after 10 listeners."
---

| C# | Node.js |
| --- | --- |
| `public event EventHandler<Order> Placed;` | `EventEmitter<{ placed: [order: Order] }>` |
| `x.Placed += Handler` | `x.on("placed", handler)` |
| `x.Placed -= Handler` | `x.off("placed", handler)` |
| `Placed?.Invoke(this, order)` | `this.emit("placed", order)` |
| `TaskCompletionSource` for one event | `events.once` |
