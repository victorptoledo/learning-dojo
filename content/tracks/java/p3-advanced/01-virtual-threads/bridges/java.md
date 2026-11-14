---
from: java
status: draft
concept: "Beyond reactive spaghetti"
from_way: "Reactive types (Mono/Flux) to get high concurrency at the cost of readability and stack traces."
native_way: "Virtual threads run ordinary synchronous code (`Thread.ofVirtual()`); the JVM unmounts them while they wait on blocking I/O."
sensei_tip: "With virtual threads in Java 21, plain synchronous code is a strong default again for I/O-heavy services."
---
