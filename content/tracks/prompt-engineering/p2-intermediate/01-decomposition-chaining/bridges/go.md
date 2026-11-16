---
from: go
status: draft
concept: "Pipelines of stages"
from_way: "Pipelines of goroutines connected by channels, each stage doing one transformation, with errors returned up the chain."
native_way: "A chain of prompts where each step transforms its input, a check returns an error on bad output, and independent steps run concurrently."
sensei_tip: "errgroup.Group is the parallel-sections pattern with cancellation for free: if one section fails its gate, the others can stop."
---

| Go | Chaining concept |
| --- | --- |
| a pipeline of stages | a sequential chain |
| `if err != nil { return err }` after a stage | a gate |
| a `switch` on a request kind | routing |
| `errgroup.Group` | parallel sections |
| a `for attempt := 1; attempt <= max; attempt++` loop | bounded retries |
