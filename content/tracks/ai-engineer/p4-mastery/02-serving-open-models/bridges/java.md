---
from: java
status: draft
concept: "Capacity planning for a memory-bound service"
from_way: "Sizing JVM heaps, thread pools and instance counts from load tests."
native_way: "Sizing GPU memory for weights and KV cache, and concurrency for an inference engine, from load tests with realistic prompt and output lengths."
sensei_tip: "The KV cache behaves like a heap that grows per request: too many long conversations at once and the engine queues or evicts, just as a JVM under memory pressure slows down. Load-test with production-like lengths, not hello-world prompts."
---

| Java operations | Model serving |
| --- | --- |
| heap size | GPU memory for weights and KV cache |
| thread pool size | maximum concurrent sequences |
| request batching | continuous batching |
| Gatling or JMeter | load tests measuring TTFT and tokens per second |
| a client with a configurable base URL | an OpenAI-compatible endpoint |
