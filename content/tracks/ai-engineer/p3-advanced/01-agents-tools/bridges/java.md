---
from: java
status: draft
concept: "Agents that act on the real world"
from_way: "Calling an LLM API only to generate passive text."
native_way: "The LLM decides when to call tools (run SQL, query APIs, read files) and reacts to the results."
sensei_tip: "Always cap the agent's iterations (`max_steps`) to avoid expensive infinite loops."
---
