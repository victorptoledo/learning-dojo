# Tool Call Checker

A tool is a dict with `name`, `description` and `input_schema`. Schemas use a small subset of
JSON Schema: `type` (`string`, `integer`, `number`, `boolean`, `array`, `object`),
`properties`, `required`, `description`, `enum`, `minimum`, `maximum` and, for arrays,
`items` with a `type`. Write four functions in `tools.py`. Every problem is one of the exact
strings below.

### `check_tool_definition(tool) -> list[str]`

In this order:

1. `name must be 1-64 letters, digits, '_' or '-'` unless the name is a string matching that;
2. `description must say what the tool does and when to use it (at least 20 characters)`
   unless it is a string of at least 20 characters after stripping;
3. `input_schema must be an object schema` unless it is a dict with `"type": "object"`; in
   that case stop and return the problems so far;
4. for each property (in order): `property 'P' has an unknown type` if its `type` is not one
   of the six, and `property 'P' needs a description` if it has no non-blank description;
5. `required property 'R' is not defined` for each name in `required` (default `[]`) that is
   not in `properties` (default `{}`).

### `validate_arguments(schema, args) -> list[str]`

If `args` is not a dict, only `arguments must be a JSON object`. Otherwise:

1. `missing required argument 'R'` for each required name not in `args`, in `required` order;
2. then for each key of `args`, in order:
   - `unknown argument 'K'` if it is not a property (no other check for it);
   - `argument 'K' must be TYPE` if the value has the wrong type. `integer` is an `int`,
     `number` an `int` or `float`, and a `bool` is **only** a `boolean`;
   - if the type is right: `argument 'K' must be one of: A, B` when there is an `enum` and the
     value is not in it (values joined with `", "` as `str`), `argument 'K' must be at least N`
     / `at most N` for `minimum` / `maximum`, and `argument 'K' items must be TYPE` (once) when
     an array has an item of the wrong type.

### `error_result(call_id, problems) -> dict`

```python
{"type": "tool_result", "tool_use_id": call_id, "is_error": True,
 "content": "Invalid arguments:\n- P1\n- P2\nFix the arguments and call the tool again."}
```

### `dispatch(call, registry) -> dict`

`call` is `{"id": ..., "name": ..., "input": {...}}`; `registry` maps a tool name to
`(tool, function)`. An unknown name gives `error_result(id, ["unknown tool 'NAME'"])`.
Invalid arguments give `error_result(id, problems)`, and the function is **not** called.
Otherwise call `function(**input)`: on success return
`{"type": "tool_result", "tool_use_id": id, "content": str(result)}`; if it raises an
`Exception`, return the same with `"is_error": True` and `"content": "Tool failed: MESSAGE"`.

Run the visible tests with `uv run pytest`. There are 15 tests: 2 in `tests/test_visible.py`
and 13 hidden ones, plus ruff on `tools.py`.
