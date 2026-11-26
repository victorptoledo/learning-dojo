---
from: go
status: draft
concept: "Bounded buffers and fresh goroutines"
from_way: "Ring buffers, bounded channels, and goroutines that receive only the data they need."
native_way: "Keep the agent's context bounded: a window of recent turns, old payloads replaced by placeholders, older history folded into a summary, and sub-agents started with a brief that holds only what they need."
sensei_tip: "Pass a sub-agent a small struct (goal, context, constraints, output format) the way you pass a goroutine only the values it needs, and return a short result over the channel, not the raw data."
---

| Go | Context-engineering concept |
| --- | --- |
| `container/ring` or a bounded slice | recent turns kept verbatim |
| replacing a large `[]byte` with its length | a cleared tool result placeholder |
| a goroutine with its own arguments | a sub-agent with a fresh context |
| a result struct sent over a channel | a condensed sub-agent result |
| a file written with `os.WriteFile` | notes outside the window |
