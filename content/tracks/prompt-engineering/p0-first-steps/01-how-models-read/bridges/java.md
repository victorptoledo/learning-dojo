---
from: java
status: draft
concept: "A model call as a non-deterministic function"
from_way: "A method call with typed parameters that returns the same result for the same input, and a stack trace when something is wrong."
native_way: "A model call takes one long text (the context) and returns a sample from a probability distribution; there is no exception for a wrong answer, only output you must check."
sensei_tip: "Think of the prompt as the whole argument list and the model as a remote service that may answer differently each time: validate the response like untrusted JSON from an external API, never like a return value you can trust."
---

| Java habit | With a language model |
| --- | --- |
| `String.length()` measures size | size and cost are measured in tokens |
| a method is deterministic | output is sampled; test several runs |
| `@NotNull` and types reject bad input | the model accepts any text and always answers |
| an exception signals a failure | a wrong answer looks like a right one; check it |
| a session object keeps state | the client resends the conversation every call |
