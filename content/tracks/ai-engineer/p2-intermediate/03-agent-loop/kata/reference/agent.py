"""A tool-calling agent loop with argument checks, error results and limits."""

import json
from collections.abc import Callable, Mapping, Sequence
from dataclasses import dataclass
from typing import Any

from llm import (
    END_TURN,
    MAX_TOKENS,
    REFUSAL,
    STOP_SEQUENCE,
    TOOL_CALL,
    Message,
    Model,
    Request,
    ToolCall,
    ToolResult,
    ToolSpec,
    Usage,
)


@dataclass(frozen=True)
class Tool:
    name: str
    description: str
    parameters: dict[str, Any]
    run: Callable[..., Any]

    @property
    def spec(self) -> ToolSpec:
        return ToolSpec(self.name, self.description, self.parameters)


@dataclass
class AgentResult:
    text: str
    messages: list[Message]
    steps: int
    usage: Usage
    stopped: str = "done"


class AgentError(Exception):
    def __init__(self, message: str, messages: Sequence[Message] = ()) -> None:
        super().__init__(message)
        self.messages = list(messages)


class StepLimitError(AgentError):
    pass


class BudgetExceededError(AgentError):
    pass


class RepeatedCallsError(AgentError):
    pass


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


def check_arguments(arguments: Mapping[str, Any], schema: Mapping[str, Any]) -> list[str]:
    properties = schema.get("properties", {})
    errors = [f"missing required argument '{name}'" for name in schema.get("required", []) if name not in arguments]
    for name, value in arguments.items():
        if name in properties:
            expected = properties[name].get("type")
            actual = _json_type(value)
            if expected and not (actual == expected or (expected == "number" and actual == "integer")):
                errors.append(f"argument '{name}': expected {expected}, got {actual}")
        elif schema.get("additionalProperties") is False:
            errors.append(f"unexpected argument '{name}'")
    return errors


def run_tool(tools: Mapping[str, Tool], call: ToolCall) -> ToolResult:
    tool = tools.get(call.name)
    if tool is None:
        available = ", ".join(sorted(tools))
        return ToolResult(call.id, f"unknown tool '{call.name}'. Available tools: {available}", True)
    errors = check_arguments(call.arguments, tool.parameters)
    if errors:
        return ToolResult(call.id, "invalid arguments: " + "; ".join(errors), True)
    try:
        value = tool.run(**call.arguments)
    except Exception as error:
        return ToolResult(call.id, f"{type(error).__name__}: {error}", True)
    return ToolResult(call.id, value if isinstance(value, str) else json.dumps(value))


def run_agent(
    model: Model,
    tools: Mapping[str, Tool],
    task: str,
    system: str = "",
    max_steps: int = 8,
    max_total_tokens: int | None = None,
) -> AgentResult:
    messages = [Message("user", task)]
    specs = tuple(tool.spec for tool in tools.values())
    usage = Usage()
    previous: list[tuple[str, dict[str, Any]]] | None = None
    for step in range(1, max_steps + 1):
        spent = usage.input_tokens + usage.output_tokens
        if step > 1 and max_total_tokens is not None and spent >= max_total_tokens:
            raise BudgetExceededError(f"token budget spent ({spent} tokens)", messages)
        response = model.complete(Request(tuple(messages), system=system, tools=specs))
        usage = Usage(
            usage.input_tokens + response.usage.input_tokens,
            usage.output_tokens + response.usage.output_tokens,
        )
        messages.append(Message("assistant", response.content))
        if response.stop_reason == TOOL_CALL and response.tool_calls:
            current = [(call.name, call.arguments) for call in response.tool_calls]
            if current == previous:
                raise RepeatedCallsError("the model repeated the same tool calls", messages)
            previous = current
            results = tuple(run_tool(tools, call) for call in response.tool_calls)
            messages.append(Message("user", results))
            continue
        if response.stop_reason in (END_TURN, STOP_SEQUENCE):
            return AgentResult(response.text, messages, step, usage, "done")
        if response.stop_reason == REFUSAL:
            return AgentResult(response.text, messages, step, usage, "refusal")
        if response.stop_reason == MAX_TOKENS:
            raise AgentError("the answer was cut off (max_tokens)", messages)
        raise AgentError(f"unexpected stop reason {response.stop_reason!r}", messages)
    raise StepLimitError(f"no final answer after {max_steps} steps", messages)
