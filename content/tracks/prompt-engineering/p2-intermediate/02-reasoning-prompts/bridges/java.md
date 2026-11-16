---
from: java
status: draft
concept: "Showing your work"
from_way: "A complex calculation in one expression, versus intermediate variables with names you can inspect in the debugger."
native_way: "The model writes its intermediate steps in <thinking> tags before the <answer>, so later tokens can use them; code reads only the answer."
sensei_tip: "Asking for reasoning is like splitting a long expression into named local variables: the result can be checked step by step. Self-consistency is like running a flaky test five times and trusting the majority."
---

| Java | Reasoning concept |
| --- | --- |
| named intermediate variables | chain-of-thought steps |
| the return value | the `<answer>` tag |
| debug logs not shown to users | reasoning kept out of the user's view |
| a majority vote over replicas | self-consistency |
| a timeout and a thread budget | thinking budgets |
