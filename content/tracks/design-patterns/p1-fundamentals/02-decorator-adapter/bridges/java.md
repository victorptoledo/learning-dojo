---
from: java
status: draft
concept: "Object composition"
from_way: "Deep subclass chains like `LoggingBufferedTransactionalUserService`, five levels down."
native_way: "A decorator wraps the original object behind the same interface, delegates the call and adds logging or metrics."
sensei_tip: "The Gang of Four's motto is 'favour composition over inheritance'. Decorator is its textbook example."
---
