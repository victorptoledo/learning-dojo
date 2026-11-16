# Prompts for Tool Use

A model can only produce text. **Tool use** (also called function calling) lets it ask your code
to do something: look up an order, search documents, run a calculation, send an email. The
model never runs anything itself. It writes a structured request; your code decides whether and
how to execute it.

## The loop

1. You send the conversation plus a list of **tool definitions**: a name, a description and a
   JSON Schema for the arguments.
2. The model either answers normally or returns a **tool call**: a tool name and arguments
   that (usually) follow the schema.
3. Your code **validates** the call, **executes** it, and sends back a **tool result** linked
   to the call's id.
4. The model continues: another call, or a final answer that uses the results.

```json
{"name": "get_order_status",
 "description": "Look up the shipping status of one order by its id. Use it when the customer asks where an order is. Do not use it for refunds.",
 "input_schema": {
   "type": "object",
   "properties": {
     "order_id": {"type": "string", "description": "The order id, for example A-1182"}
   },
   "required": ["order_id"]}}
```

Field names differ between providers (`input_schema`, `parameters`), but every API has these
three parts.

## Tool definitions are prompts

The model chooses tools by reading their names and descriptions, so write them as carefully as
any prompt:

- **Names** say what the tool does: `search_flights`, not `api` or `tool1`.
- **Descriptions** say what it does, **when to use it**, **when not to**, and what it returns.
  Several sentences are normal for an important tool.
- **Parameters** have types, descriptions, formats ("ISO 8601 date", "IATA airport code") and
  **enums** for closed sets. Mark required ones as required.
- **Few, distinct tools** beat many overlapping ones. If two tools could both fit a request,
  the model will sometimes pick the wrong one. Merge them or make the boundary explicit.
- **Return what the model needs**: concise, relevant fields with meaningful names, not a raw
  dump of an internal record with ids it cannot use.

## Choosing whether to call

APIs let you set the **tool choice**: let the model decide (auto), require some tool call, force
one specific tool, or disable tools. Forcing a tool is a reliable way to get structured output:
the arguments of the forced call are your JSON.

In the system prompt, state the policy in words too: "Look up the order before answering any
question about its status. Never guess a status."

## Errors that help the model recover

Arguments can be wrong: a missing field, a value outside the enum, a date in the wrong format.
Validate them in code before you execute anything, and return the problems as an error result:

```text
Invalid arguments:
- missing required argument 'order_id'
- argument 'units' must be one of: celsius, fahrenheit
Fix the arguments and call the tool again.
```

A model given specific errors usually fixes its call; a model given "error 400" usually gives up
or guesses. The same goes for failures while running: "No order with id A-9999. Ask the
customer to check the id." is a useful result.

## Side effects and safety

Tools turn text into actions, so the rules that matter go in code:

- **validate and authorise** every call: the user may only see their own orders, whatever the
  arguments say;
- **confirm** actions that are hard to undo (pay, book, delete, send): the tool prepares the
  action, a human confirms it;
- make tools **idempotent** where possible, since a model may repeat a call;
- treat **tool results as untrusted** when they contain outside text (web pages, emails): they
  can carry prompt injection (Advanced);
- **limit the loop**: a maximum number of calls per request and a token budget.

## Standard tool protocols

The **Model Context Protocol (MCP)** is an open protocol for offering tools (and other context)
to models: a server describes its tools with the same name, description and JSON Schema, and
any compatible client can use them. Everything in this lesson applies to MCP tools unchanged.

## Further reading

- Anthropic, "Tool use with Claude": <https://docs.anthropic.com/en/docs/agents-and-tools/tool-use/overview>
- Anthropic, "Writing effective tools for agents": <https://www.anthropic.com/engineering/writing-tools-for-agents>
- OpenAI, "Function calling": <https://platform.openai.com/docs/guides/function-calling>
- Google, "Function calling with the Gemini API": <https://ai.google.dev/gemini-api/docs/function-calling>
- Model Context Protocol, specification: <https://modelcontextprotocol.io/specification>
- Yao et al., "ReAct: Synergizing Reasoning and Acting in Language Models" (2022): <https://arxiv.org/abs/2210.03629>
