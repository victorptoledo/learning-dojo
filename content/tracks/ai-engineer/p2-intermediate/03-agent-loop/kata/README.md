# Agent Loop

Write `agent.py`, a tool-calling agent loop. `llm.py` (given, do not edit) has the message types
and `ScriptedModel`. `Tool(name, description, parameters, run)` is given: `parameters` is a JSON
Schema for the arguments, `run` a function called with the arguments as keyword arguments, and
`tool.spec` the `ToolSpec` to send to the model.

## 1. `check_arguments(arguments, schema) -> list[str]`

Check the top level of the arguments only (order of errors does not matter):

- every name in `required` is present: `"missing required argument 'city'"`;
- each argument listed in `properties` has its `type` (`string`, `integer`, `number`, `boolean`,
  `array`, `object`; a `bool` is not an integer or a number, an `int` is a number):
  `"argument 'days': expected integer, got string"` (type names as in the structured-output kata);
- with `additionalProperties: false`, other names are errors: `"unexpected argument 'units'"`.

## 2. `run_tool(tools, call) -> ToolResult`

`tools` maps names to `Tool`s. Always return a `ToolResult` for `call.id`, never raise:

- unknown tool: an error result `"unknown tool 'x'. Available tools: a, b"` (names sorted);
- argument errors: an error result `"invalid arguments: "` followed by the errors joined with `"; "`;
- the tool raises an exception: an error result `"<ExceptionType>: <message>"`, for example
  `"KeyError: 'A-9'"` (that is, `f"{type(e).__name__}: {e}"`);
- otherwise a normal result with the tool's return value: a `str` as it is, anything else as
  `json.dumps(value)`.

## 3. `run_agent(model, tools, task, system="", max_steps=8, max_total_tokens=None) -> AgentResult`

1. Start the history with `Message("user", task)`.
2. Each step sends `Request(messages=tuple(history), system=system, tools=...)` with the specs of
   all tools (in the order of the `tools` dict), and appends the reply as
   `Message("assistant", response.content)`.
3. If the stop reason is `tool_call` and there are tool calls: run them all, in order, and append
   **one** `Message("user", results)` with every `ToolResult` as a tuple. Then the next step.
4. `end_turn` or `stop_sequence`: return `AgentResult(text=response.text, messages=history,
   steps=..., usage=..., stopped="done")`. `refusal`: the same with `stopped="refusal"`.
   `max_tokens`: raise `AgentError` (a cut-off answer is not a result).
5. Limits (each error carries `messages`, the history so far):
   - after `max_steps` requests without a final answer, raise `StepLimitError`;
   - before each request **except the first**, if the tokens used so far (input plus output, added
     over all responses) are at least `max_total_tokens`, raise `BudgetExceededError`;
   - if a step asks for exactly the same calls (names and arguments, in order) as the step before,
     raise `RepeatedCallsError` before running them again.

`usage` in the result is the sum over all responses; `steps` is the number of requests.

Run the visible tests with `uv run pytest`. There are 22 tests: 2 in `tests/test_visible.py`
and 20 hidden ones, plus ruff on each module.
