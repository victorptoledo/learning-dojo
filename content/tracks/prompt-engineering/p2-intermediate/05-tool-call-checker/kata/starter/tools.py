"""Check tool definitions, validate tool calls and dispatch them."""

from collections.abc import Callable, Mapping


def check_tool_definition(tool: dict) -> list[str]:
    """Problems with a tool definition, in the documented order."""
    raise NotImplementedError("todo: write check_tool_definition()")


def validate_arguments(schema: dict, args: object) -> list[str]:
    """Problems with a call's arguments, in the documented order."""
    raise NotImplementedError("todo: write validate_arguments()")


def error_result(call_id: str, problems: list[str]) -> dict:
    """A tool result that lists the problems so the model can fix its call."""
    raise NotImplementedError("todo: write error_result()")


def dispatch(call: dict, registry: Mapping[str, tuple[dict, Callable[..., object]]]) -> dict:
    """Validate and run one tool call, returning a tool result."""
    raise NotImplementedError("todo: write dispatch()")
