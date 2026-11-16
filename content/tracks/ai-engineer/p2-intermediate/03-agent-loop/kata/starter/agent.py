"""A tool-calling agent loop with argument checks, error results and limits."""

from collections.abc import Callable, Mapping, Sequence
from dataclasses import dataclass
from typing import Any

from llm import Message, Model, ToolCall, ToolResult, ToolSpec, Usage


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


def check_arguments(arguments: Mapping[str, Any], schema: Mapping[str, Any]) -> list[str]:
    raise NotImplementedError("todo: write check_arguments()")


def run_tool(tools: Mapping[str, Tool], call: ToolCall) -> ToolResult:
    raise NotImplementedError("todo: write run_tool()")


def run_agent(
    model: Model,
    tools: Mapping[str, Tool],
    task: str,
    system: str = "",
    max_steps: int = 8,
    max_total_tokens: int | None = None,
) -> AgentResult:
    raise NotImplementedError("todo: write run_agent()")
