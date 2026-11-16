---
from: javascript
status: draft
concept: "Promise chains and middleware"
from_way: "Promise chains (then, then, then) and Express middleware that each do one thing to a request."
native_way: "A chain of prompts, each with one job and a parsed output, with a check between steps and parallel calls where steps are independent."
sensei_tip: "Promise.all is the parallel-sections pattern; a try/catch around each await with a retry counter is the gate. Keep the per-step trace like a request log."
---

| JavaScript | Chaining concept |
| --- | --- |
| `await step1(); await step2()` | a sequential chain |
| validation middleware | a gate |
| an Express router | routing |
| `Promise.all([...])` | parallel sections |
| a request log per step | the trace |
