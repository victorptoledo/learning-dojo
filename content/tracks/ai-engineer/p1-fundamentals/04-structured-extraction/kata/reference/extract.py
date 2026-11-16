"""Find, validate and repair JSON in model replies."""

import json
import re
from collections.abc import Sequence
from typing import Any

from llm import MAX_TOKENS, REFUSAL, Message, Model, Request


class ExtractionError(Exception):
    def __init__(self, message: str, errors: Sequence[str] = (), attempts: int = 0) -> None:
        super().__init__(message)
        self.errors = list(errors)
        self.attempts = attempts


_FENCE = re.compile(r"```(?:json)?[ \t]*\n(.*?)```", re.DOTALL)
_DECODER = json.JSONDecoder()


def _parse_object(text: str) -> dict[str, Any] | None:
    try:
        value = json.loads(text)
    except json.JSONDecodeError:
        return None
    return value if isinstance(value, dict) else None


def find_json(text: str) -> dict[str, Any]:
    whole = _parse_object(text.strip())
    if whole is not None:
        return whole
    fence = _FENCE.search(text)
    if fence:
        fenced = _parse_object(fence.group(1).strip())
        if fenced is not None:
            return fenced
    for start, char in enumerate(text):
        if char != "{":
            continue
        try:
            value, _ = _DECODER.raw_decode(text, start)
        except json.JSONDecodeError:
            continue
        if isinstance(value, dict):
            return value
    raise ExtractionError("no JSON object found")


def _json_type(value: Any) -> str:
    if value is None:
        return "null"
    if isinstance(value, bool):
        return "boolean"
    if isinstance(value, int):
        return "integer"
    if isinstance(value, float):
        return "number"
    if isinstance(value, str):
        return "string"
    if isinstance(value, list):
        return "array"
    if isinstance(value, dict):
        return "object"
    return type(value).__name__


def _matches(value: Any, name: str) -> bool:
    actual = _json_type(value)
    return actual == name or (name == "number" and actual == "integer")


def validate(value: Any, schema: dict[str, Any], path: str = "$") -> list[str]:
    if "type" in schema:
        names = schema["type"] if isinstance(schema["type"], list) else [schema["type"]]
        if not any(_matches(value, name) for name in names):
            return [f"{path}: expected {' or '.join(names)}, got {_json_type(value)}"]
    errors: list[str] = []
    if "enum" in schema and value not in schema["enum"]:
        errors.append(f"{path}: {json.dumps(value)} is not one of {json.dumps(schema['enum'])}")
    if isinstance(value, (int, float)) and not isinstance(value, bool):
        if "minimum" in schema and value < schema["minimum"]:
            errors.append(f"{path}: {value} is less than the minimum {schema['minimum']}")
        if "maximum" in schema and value > schema["maximum"]:
            errors.append(f"{path}: {value} is greater than the maximum {schema['maximum']}")
    if isinstance(value, dict):
        properties = schema.get("properties", {})
        for name in schema.get("required", []):
            if name not in value:
                errors.append(f"{path}: missing required property '{name}'")
        for name, item in value.items():
            if name in properties:
                errors.extend(validate(item, properties[name], f"{path}.{name}"))
            elif schema.get("additionalProperties") is False:
                errors.append(f"{path}: unexpected property '{name}'")
    if isinstance(value, list) and "items" in schema:
        for index, item in enumerate(value):
            errors.extend(validate(item, schema["items"], f"{path}[{index}]"))
    return errors


def system_prompt(schema: dict[str, Any]) -> str:
    return (
        "Extract the requested data as one JSON object that matches this JSON Schema:\n"
        f"{json.dumps(schema)}\n"
        "Reply with the JSON object only, no prose."
    )


def repair_message(problems: Sequence[str]) -> str:
    lines = ["Your reply did not match the schema:", *(f"- {problem}" for problem in problems)]
    lines.append("Reply with corrected JSON only.")
    return "\n".join(lines)


def extract(model: Model, text: str, schema: dict[str, Any], max_attempts: int = 3) -> dict[str, Any]:
    if max_attempts < 1:
        raise ValueError("max_attempts must be at least 1")
    messages = [Message("user", text)]
    problems: list[str] = []
    for attempt in range(1, max_attempts + 1):
        response = model.complete(Request(tuple(messages), system=system_prompt(schema)))
        if response.stop_reason == REFUSAL:
            raise ExtractionError("the model refused to answer", [], attempt)
        if response.stop_reason == MAX_TOKENS:
            problems = ["the reply was cut off (max_tokens)"]
        else:
            try:
                data = find_json(response.text)
            except ExtractionError:
                problems = ["no JSON object found"]
            else:
                problems = validate(data, schema)
                if not problems:
                    return data
        messages += [Message("assistant", response.text), Message("user", repair_message(problems))]
    raise ExtractionError(f"no valid JSON after {max_attempts} attempts", problems, max_attempts)
