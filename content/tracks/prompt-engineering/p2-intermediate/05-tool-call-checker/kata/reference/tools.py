"""Check tool definitions, validate tool calls and dispatch them."""

import re
from collections.abc import Callable, Mapping

NAME = re.compile(r"[A-Za-z0-9_-]{1,64}")
TYPES = ("string", "integer", "number", "boolean", "array", "object")


def _has_type(value: object, type_name: str) -> bool:
    if isinstance(value, bool):
        return type_name == "boolean"
    match type_name:
        case "string":
            return isinstance(value, str)
        case "integer":
            return isinstance(value, int)
        case "number":
            return isinstance(value, (int, float))
        case "array":
            return isinstance(value, list)
        case "object":
            return isinstance(value, dict)
    return False


def check_tool_definition(tool: dict) -> list[str]:
    """Problems with a tool definition, in the documented order."""
    problems = []
    name = tool.get("name")
    if not isinstance(name, str) or not NAME.fullmatch(name):
        problems.append("name must be 1-64 letters, digits, '_' or '-'")
    description = tool.get("description")
    if not isinstance(description, str) or len(description.strip()) < 20:
        problems.append(
            "description must say what the tool does and when to use it (at least 20 characters)"
        )
    schema = tool.get("input_schema")
    if not isinstance(schema, dict) or schema.get("type") != "object":
        problems.append("input_schema must be an object schema")
        return problems
    properties = schema.get("properties", {})
    for prop_name, prop in properties.items():
        if prop.get("type") not in TYPES:
            problems.append(f"property '{prop_name}' has an unknown type")
        text = prop.get("description")
        if not isinstance(text, str) or not text.strip():
            problems.append(f"property '{prop_name}' needs a description")
    for required in schema.get("required", []):
        if required not in properties:
            problems.append(f"required property '{required}' is not defined")
    return problems


def _check_value(key: str, spec: dict, value: object) -> list[str]:
    type_name = spec.get("type")
    if not _has_type(value, type_name):
        return [f"argument '{key}' must be {type_name}"]
    problems = []
    if "enum" in spec and value not in spec["enum"]:
        allowed = ", ".join(str(option) for option in spec["enum"])
        problems.append(f"argument '{key}' must be one of: {allowed}")
    if "minimum" in spec and value < spec["minimum"]:
        problems.append(f"argument '{key}' must be at least {spec['minimum']}")
    if "maximum" in spec and value > spec["maximum"]:
        problems.append(f"argument '{key}' must be at most {spec['maximum']}")
    item_type = spec.get("items", {}).get("type")
    if type_name == "array" and item_type and not all(_has_type(i, item_type) for i in value):
        problems.append(f"argument '{key}' items must be {item_type}")
    return problems


def validate_arguments(schema: dict, args: object) -> list[str]:
    """Problems with a call's arguments, in the documented order."""
    if not isinstance(args, dict):
        return ["arguments must be a JSON object"]
    properties = schema.get("properties", {})
    problems = [
        f"missing required argument '{name}'"
        for name in schema.get("required", [])
        if name not in args
    ]
    for key, value in args.items():
        if key not in properties:
            problems.append(f"unknown argument '{key}'")
        else:
            problems += _check_value(key, properties[key], value)
    return problems


def error_result(call_id: str, problems: list[str]) -> dict:
    """A tool result that lists the problems so the model can fix its call."""
    lines = "\n".join(f"- {problem}" for problem in problems)
    return {
        "type": "tool_result",
        "tool_use_id": call_id,
        "is_error": True,
        "content": f"Invalid arguments:\n{lines}\nFix the arguments and call the tool again.",
    }


def dispatch(call: dict, registry: Mapping[str, tuple[dict, Callable[..., object]]]) -> dict:
    """Validate and run one tool call, returning a tool result."""
    name, call_id = call["name"], call["id"]
    if name not in registry:
        return error_result(call_id, [f"unknown tool '{name}'"])
    tool, function = registry[name]
    problems = validate_arguments(tool["input_schema"], call["input"])
    if problems:
        return error_result(call_id, problems)
    try:
        result = function(**call["input"])
    except Exception as error:
        return {
            "type": "tool_result",
            "tool_use_id": call_id,
            "is_error": True,
            "content": f"Tool failed: {error}",
        }
    return {"type": "tool_result", "tool_use_id": call_id, "content": str(result)}
