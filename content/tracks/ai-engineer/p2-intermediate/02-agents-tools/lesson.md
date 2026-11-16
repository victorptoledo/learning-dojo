# Tool Calling and Agents

A model can only produce text. **Tool calling** (also called function calling) lets it ask your
code to do something: look up an order, run a search, call an API. An **agent** is a loop in which
the model decides, step by step, which tools to call until the task is done. Both are built on the
same small protocol.

## The protocol

1. You send the request with **tool definitions**: name, description and a JSON Schema for the
   arguments.

   ```json
   {"name": "get_order", "description": "Look up one of the customer's orders by its number, e.g. A-1234. Returns status, items and delivery date.",
    "parameters": {"type": "object", "properties": {"order_id": {"type": "string"}}, "required": ["order_id"], "additionalProperties": false}}
   ```

2. The model answers with stop reason `tool_call` (`tool_use`, `tool_calls`) and one or more
   **tool calls**: an id, the tool name and the arguments as JSON.
3. **Your code** validates the arguments, runs the function and sends back a **tool result** that
   refers to the call id, in a new user message (a `tool` message on some APIs).
4. The model continues: another tool call, or a final answer with stop reason `end_turn`.

The model never runs anything. It proposes; your code decides and executes. That is where you
put validation, permissions and limits.

Details that matter:

- **Parallel calls.** One response can contain several tool calls. Run them (concurrently if
  safe) and return **all** results together, each with its call id.
- **Errors are results.** When a tool fails (bad argument, not found, timeout), return a result
  marked as an error with a useful message ("order A-9 not found; order ids look like A-1234").
  The model can then correct itself. Raising an exception ends the conversation instead.
- **Validate arguments.** Even with strict schemas, check them: types, ranges, and above all
  **ownership** (is this the customer's order?).
- **Tool choice.** You can let the model decide (`auto`), forbid tools (`none`), or on some APIs
  force a call; forcing is a common way to get structured output.

## The agent loop

```python
messages = [user_message]
for step in range(MAX_STEPS):
    response = model.complete(Request(messages, tools=TOOLS))
    messages.append(assistant_message(response))
    if response.stop_reason != "tool_call":
        return response.text                     # final answer
    results = [run_tool(call) for call in response.tool_calls]
    messages.append(user_message_with(results))
raise StepLimitExceeded
```

Every iteration resends the whole growing conversation, so cost grows quickly. Production loops
add:

- a **step limit** and a **token or cost budget**, with a graceful stop ("I could not finish;
  here is what I found");
- **timeouts** per tool and per run;
- detection of **repeated identical calls** (a model stuck in a loop);
- trimming or summarising old tool results that are no longer needed;
- **human approval** before irreversible actions.

SDKs offer tool runners that implement this loop for you; you still choose the limits.

## Workflows or agents?

| | Workflow | Agent |
| --- | --- | --- |
| Control flow | your code fixes the steps (classify, then retrieve, then answer) | the model chooses the next step |
| Predictability | high, easy to test | lower, needs more evaluation |
| Cost and latency | bounded | variable, often higher |
| Good for | known processes with few branches | open-ended tasks: research, coding, multi-step support |

Start with the simplest thing that works: one call, then a fixed workflow (prompt chaining,
routing to specialised prompts, running parts in parallel), and only then an agent. Many
"agents" in production are workflows with one or two model-chosen steps.

## Designing tools models use well

- **Few, clear tools.** Ten precise tools beat forty overlapping ones. Each name and description
  should say what it does, when to use it, and what it returns, like documentation for a new
  colleague.
- **Typed, constrained parameters**: enums, formats, examples in the description.
- **Useful, compact results.** Return the fields the model needs, not a 5,000-token JSON dump;
  paginate long lists; include identifiers for follow-up calls.
- **Actionable errors** that say how to fix the call.
- **Authorisation from the session, never from arguments.** The customer id comes from the logged-in
  user, not from a `customer_id` parameter the model could fill with anything.
- **Separate reading from doing.** Read-only tools can run freely; tools with side effects
  (payments, emails, deletes) need confirmation, limits, idempotency keys and audit logs.

## MCP

The **Model Context Protocol** (MCP) is an open protocol for exposing tools, resources and prompts
to AI applications through a standard server interface (over stdio or HTTP). Write a tool server
once and any MCP-capable client (desktop assistants, IDEs, your own agent) can use it. It
standardises plumbing; the design rules above, and the security questions, still apply: an MCP
server you install runs with your permissions and its tool descriptions become part of the prompt.

## Multi-agent systems

A lead agent can hand sub-tasks to other agents (with their own context and tools) and merge
their results. This helps with broad tasks that would overflow one context, at a multiple of the
cost. Treat it as an optimisation for specific problems, not a default architecture.

## Summary

- Tool calling: definitions in, calls out, your code executes, results back by call id; errors are
  results.
- An agent is a loop with stopping rules: step limits, budgets, timeouts, loop detection, approvals.
- Prefer workflows when the steps are known; use agents for open-ended tasks.
- Design few, well-described tools with compact results and authorisation from the session.
- MCP standardises how tools are exposed; it does not make them safe.

## Further reading

- Anthropic, "Building effective agents": <https://www.anthropic.com/engineering/building-effective-agents>
- Anthropic, "Writing effective tools for agents": <https://www.anthropic.com/engineering/writing-tools-for-agents>
- Anthropic docs, tool use overview: <https://platform.claude.com/docs/en/agents-and-tools/tool-use/overview>
- OpenAI docs, function calling: <https://platform.openai.com/docs/guides/function-calling>
- Yao et al., "ReAct: Synergizing Reasoning and Acting in Language Models" (2022): <https://arxiv.org/abs/2210.03629>
- Schick et al., "Toolformer: Language Models Can Teach Themselves to Use Tools" (2023): <https://arxiv.org/abs/2302.04761>
- Model Context Protocol, specification and docs: <https://modelcontextprotocol.io>
