# Structured Extraction

Write `extract.py`: find the JSON object in a model's reply, validate it against a schema, and
repair it by asking the model again. `llm.py` (given, do not edit) has the request and response
types and `ScriptedModel`, a fake model that answers with scripted replies and records the
requests it receives.

## 1. `find_json(text) -> dict`

Try, in order, and return the first **JSON object** (a `dict`) found:

1. the whole text (stripped) parsed as JSON;
2. the content of the first fenced code block (```` ```json ```` or plain ```` ``` ````);
3. the first balanced `{...}` in the text. Braces inside JSON strings do not count:
   `{"note": "use {braces}"}` is one object.

A strategy that parses something other than an object (a list, a number) does not count. If
nothing works, raise `ExtractionError("no JSON object found")`.

## 2. `validate(value, schema, path="$") -> list[str]`

Return a list of error messages (empty when valid; order does not matter). Supported keywords:

| Keyword | Rule and message |
| --- | --- |
| `type` | a name or a list of names: `string`, `integer`, `number`, `boolean`, `null`, `array`, `object`. `bool` is not an `integer` or `number`; an `int` is also a `number`. `"{path}: expected integer, got string"` (a list of names is joined with `" or "`: `expected string or null`). When the type is wrong, check nothing else at that path. |
| `enum` | `"{path}: \"urgent\" is not one of [\"low\", \"high\"]"`: the value and the list as `json.dumps` renders them |
| `minimum`, `maximum` | `"{path}: 0 is less than the minimum 1"`, `"{path}: 9 is greater than the maximum 5"` |
| `required` | `"{path}: missing required property 'order_id'"` |
| `properties` | each present property is checked with the path `f"{path}.{name}"` |
| `additionalProperties: false` | `"{path}: unexpected property 'color'"` |
| `items` | each element is checked with the path `f"{path}[{index}]"` |

The JSON type names of Python values: `str` string, `bool` boolean, `int` integer, `float`
number, `None` null, `list` array, `dict` object.

## 3. `extract(model, text, schema, max_attempts=3) -> dict`

1. Send `Request(messages=(Message("user", text),), system=...)`. The system prompt must contain
   `json.dumps(schema)` and ask for JSON only.
2. Look at the response:
   - stop reason `refusal`: raise `ExtractionError` at once (no retry);
   - stop reason `max_tokens`: the problem is `"the reply was cut off (max_tokens)"`;
   - otherwise `find_json(response.text)`; if it fails the problem is `"no JSON object found"`,
     else the problems are `validate(data, schema)`. No problems: return the data.
3. On problems, send the **whole conversation again** plus the model's reply as an `assistant`
   message and a `user` message that starts with `"Your reply did not match the schema:"`,
   lists every problem on its own line starting with `"- "`, and ends with
   `"Reply with corrected JSON only."`
4. After `max_attempts` requests, raise `ExtractionError` with `errors` set to the last problems
   and `attempts` to the number of requests made.

`max_attempts` below 1 raises `ValueError`.

Run the visible tests with `uv run pytest`. There are 26 tests: 2 in `tests/test_visible.py`
and 24 hidden ones, plus ruff on each module.
