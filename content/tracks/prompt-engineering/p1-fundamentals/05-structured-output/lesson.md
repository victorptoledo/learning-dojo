# Structured Output and Schemas

As soon as code reads the model's answer, the answer is an **API response**. It needs a
contract, and your code needs to check that the contract was kept. This lesson covers both
sides: asking for structure, and not trusting it blindly.

## Describe the format exactly

The weakest request is "answer in JSON". Better is a precise description with an example:

```text
Reply with only a JSON object, no other text, with exactly these fields:
{
  "category": "billing" | "bug" | "account" | "other",
  "priority": "low" | "medium" | "high",
  "summary": string, at most 200 characters
}
```

Strongest is a **JSON Schema**, which both humans and APIs understand:

```json
{
  "type": "object",
  "properties": {
    "category": {"type": "string", "enum": ["billing", "bug", "account", "other"]},
    "priority": {"type": "string", "enum": ["low", "medium", "high"]},
    "due_date": {"type": ["string", "null"], "description": "ISO 8601 date, or null if none"},
    "summary": {"type": "string", "maxLength": 200}
  },
  "required": ["category", "priority", "due_date", "summary"],
  "additionalProperties": false
}
```

Design rules for output schemas:

- **Enums for closed sets.** A free-text field called `category` will produce "Billing",
  "billing issue" and "payments".
- **One explicit value for missing data**, usually `null`, and say so. Otherwise you get "N/A",
  "", "unknown" or a hallucinated value.
- **Machine-friendly types**: ISO 8601 dates, amounts as numbers in a stated unit, ISO currency
  codes. Do the formatting for humans in code.
- **Field order matters.** The model writes fields in order, and later fields are conditioned
  on earlier ones. Put a `reasoning` or `evidence` field **before** the `answer` field if you
  want the answer to use it; a reason written after the answer is only a justification.
- **Keep it flat and small.** Deeply nested schemas with dozens of optional fields are harder
  to fill correctly. Split big extractions into steps.

## Ways to get structure

| Technique | Guarantee | Notes |
| --- | --- | --- |
| Format instructions and examples | none | works everywhere; validate everything |
| Prefilling `{` (where supported) | starts as JSON | not available on every model |
| "JSON mode" | valid JSON syntax | not your schema |
| Structured outputs / strict schemas | output matches the schema | constrained decoding; supports a subset of JSON Schema |
| A forced tool call | arguments follow the tool's schema | the tool-use lesson in Intermediate |

Schema-constrained decoding (offered by several providers as "structured outputs") restricts
the tokens the model may produce so that the result always parses and matches the schema.
That removes syntax errors, not **semantic** errors: the `summary` can still be wrong, the
`priority` can still be the wrong enum value, and a refusal may come back in place of the
object. You still validate.

## Parse defensively

When you do not have constrained output, real answers include:

- a preamble ("Sure! Here is the JSON:") or a closing remark;
- a Markdown code fence: ` ```json ... ``` `;
- trailing commas, comments or single quotes (invalid JSON);
- the right fields with the wrong types, or extra fields.

A robust parser strips fences, takes the text from the first `{` to the last `}`, parses it,
then **validates** it: required fields present, no unknown fields, enums respected, lengths
and ranges respected. It reports **all** the problems, not just the first, because the next
step needs them.

## Validate, then retry with the errors

When validation fails, send the problems back and ask again:

```text
Your answer could not be used:
- priority must be one of: low, medium, high
- missing field 'summary'
Reply again with only the JSON object.
```

Specific errors are far more effective than "try again". Bound the number of retries (one or
two), log every failure, and when retries run out, fail visibly (an error, a queue for a
human) instead of passing on a guess. Track the failure rate: a rising rate after a model
update is an early warning.

## XML for text-heavy output

JSON is awkward for long prose (every quote and newline must be escaped). When the output is
mostly text with a few parts, ask for XML-style tags instead and extract them:

```text
Write your analysis in <analysis> tags and the final recommendation in
<recommendation> tags.
```

The reasoning lesson in Intermediate builds a parser for exactly this.

## Further reading

- OpenAI, "Structured model outputs": <https://platform.openai.com/docs/guides/structured-outputs>
- Anthropic, "Increase output consistency": <https://docs.anthropic.com/en/docs/test-and-evaluate/strengthen-guardrails/increase-consistency>
- Google, "Structured output" for Gemini: <https://ai.google.dev/gemini-api/docs/structured-output>
- JSON Schema, "Understanding JSON Schema": <https://json-schema.org/understanding-json-schema/>
- Willard and Louf, "Efficient Guided Generation for Large Language Models" (2023), how constrained decoding works: <https://arxiv.org/abs/2307.09702>
