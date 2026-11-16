# Structured Output: JSON Schemas, Validation and Repair

When code consumes a model's answer, prose is a liability. You want **data**: a category, a list of
fields, a decision with a reason. Structured output is the set of techniques for getting JSON that
your code can trust, and the checks that make that trust justified.

## Four ways to get JSON

| Technique | How | Guarantee |
| --- | --- | --- |
| Ask in the prompt | "Answer only with JSON like `{...}`", maybe an example | None: you may get prose, fences or broken JSON |
| JSON mode | a flag that forces syntactically valid JSON | Valid JSON, but any shape |
| Schema-constrained output | you pass a JSON Schema; decoding is constrained to it | Valid against the schema (within its supported features) |
| Tool-based extraction | define a tool whose input schema is your record; the model "calls" it | Arguments shaped by the schema, strictly so with strict mode |

Major providers now support schema-constrained output (Anthropic `output_config.format` and
strict tools, OpenAI Structured Outputs, Gemini `responseSchema`). **Constrained decoding**
works by masking, at every step, the tokens that would break the schema, so the output cannot be
syntactically wrong. Prefer it when available. You still need the rest of this lesson: older or
self-hosted models may not support it, only a subset of JSON Schema is supported, and a
**valid shape can still hold wrong values**.

## Writing the schema

```json
{
  "type": "object",
  "properties": {
    "category": {"type": "string", "enum": ["billing", "shipping", "technical", "other"]},
    "urgency": {"type": "integer", "minimum": 1, "maximum": 5},
    "order_id": {"type": ["string", "null"], "description": "Order number like A-1234, or null if absent"},
    "summary": {"type": "string", "description": "One sentence, in English"}
  },
  "required": ["category", "urgency", "order_id", "summary"],
  "additionalProperties": false
}
```

Good habits:

- **Enums** for categories, so the model cannot invent "Billing issue".
- **Descriptions** on fields: the model reads them like instructions.
- **Nullable fields** for data that may be absent. If every field is required and non-null, the
  model will invent a value rather than leave it empty.
- **`additionalProperties: false`** so extra keys are errors, not silent noise.
- Keep the shape flat and small; deep nesting and dozens of optional fields lower accuracy.
- Put a `reasoning` or `evidence` field **before** the answer fields when the task needs thought:
  the model generates fields in order, so it "thinks" before it decides.

In Python, a Pydantic model can generate the schema (`Model.model_json_schema()`) and validate the
result (`Model.model_validate_json(text)`), and the official SDKs accept Pydantic models directly.
The katas use the standard library only, so you will write a small validator yourself.

## Parsing what comes back

Without constrained decoding, real replies look like this:

````text
Sure! Here is the data:
```json
{"category": "billing", "urgency": 4, "order_id": "A-1234", "summary": "Charged twice."}
```
Let me know if you need anything else.
````

A robust parser tries, in order: the whole text as JSON; the content of a fenced code block;
the first balanced `{...}` object (scanning braces while respecting strings and escapes). Then
it **validates** against the schema and reports errors with a path (`$.items[2].quantity:
expected integer`).

## The repair loop

When parsing or validation fails, ask again, **with the errors**:

1. Send the original request.
2. Parse and validate the reply.
3. If it fails, append the model's reply as an assistant message and a user message like
   "Your answer did not match the schema: `$.urgency: expected integer, got string`. Reply with
   corrected JSON only."
4. Repeat up to a small limit (2–3 attempts), then fail loudly: return an error, route to a
   human, or use a default.

Also treat **stop reason `max_tokens`** as a failure (the JSON is cut off), and log every repair:
a rising repair rate is an early warning that a prompt or model change hurt quality.

## Valid is not the same as right

A schema checks shape. It cannot know that:

- the `total` does not equal the sum of the lines;
- the `date` is the delivery date, not the invoice date;
- the `order_id` was **invented** because the email had none.

Add **semantic checks in code** (recompute totals, check that an extracted id appears in the
source text, cross-check with your database) and measure accuracy on a golden set
(Intermediate). Where the business impact is high, let a human confirm low-confidence records.

## Summary

- Prefer schema-constrained output or tool-based extraction; fall back to prompting plus parsing.
- Design schemas with enums, descriptions, nullable fields and no extra properties.
- Parse robustly (whole text, code fence, first balanced object), then validate with paths.
- Repair with the errors fed back, a small attempt limit and logging; truncation is a failure.
- Validation catches shape, not truth: add semantic checks and measure.

## Further reading

- Anthropic docs, structured outputs: <https://platform.claude.com/docs/en/build-with-claude/structured-outputs>
- OpenAI docs, Structured Outputs: <https://platform.openai.com/docs/guides/structured-outputs>
- Google, Gemini API structured output: <https://ai.google.dev/gemini-api/docs/structured-output>
- JSON Schema, "Understanding JSON Schema": <https://json-schema.org/understanding-json-schema>
- Pydantic docs, JSON Schema: <https://docs.pydantic.dev/latest/concepts/json_schema/>
- Willard and Louf, "Efficient Guided Generation for Large Language Models" (constrained decoding, 2023): <https://arxiv.org/abs/2307.09702>
