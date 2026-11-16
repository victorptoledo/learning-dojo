"""Provider-neutral model types and a scripted fake model. Given: do not edit.

The shapes follow what the big providers have in common: a request with a system prompt,
messages and tools; a response with content blocks, a stop reason and token usage.
"""

from collections.abc import Callable, Iterable
from dataclasses import dataclass
from typing import Any, Protocol

END_TURN = "end_turn"
MAX_TOKENS = "max_tokens"
TOOL_CALL = "tool_call"
STOP_SEQUENCE = "stop_sequence"
REFUSAL = "refusal"


@dataclass(frozen=True)
class TextBlock:
    text: str


@dataclass(frozen=True)
class ToolCall:
    id: str
    name: str
    arguments: dict[str, Any]


@dataclass(frozen=True)
class ToolResult:
    call_id: str
    content: str
    is_error: bool = False


type Block = TextBlock | ToolCall | ToolResult


@dataclass(frozen=True)
class Message:
    role: str  # "user" or "assistant"
    content: str | tuple[Block, ...]


@dataclass(frozen=True)
class ToolSpec:
    name: str
    description: str
    parameters: dict[str, Any]  # a JSON Schema for the arguments


@dataclass(frozen=True)
class Usage:
    input_tokens: int = 0
    output_tokens: int = 0


@dataclass(frozen=True)
class Request:
    messages: tuple[Message, ...]
    system: str = ""
    tools: tuple[ToolSpec, ...] = ()
    max_tokens: int = 1024
    model: str = "fake-model"


@dataclass(frozen=True)
class Response:
    content: tuple[TextBlock | ToolCall, ...]
    stop_reason: str = END_TURN
    usage: Usage = Usage()

    @property
    def text(self) -> str:
        return "".join(block.text for block in self.content if isinstance(block, TextBlock))

    @property
    def tool_calls(self) -> list[ToolCall]:
        return [block for block in self.content if isinstance(block, ToolCall)]


class Model(Protocol):
    def complete(self, request: Request) -> Response: ...


type Script = Response | str | BaseException | Callable[[Request], Response]


class ScriptedModel:
    """A fake model: answers with the scripted items in order and records every request.

    An item can be a Response, a str (a text answer that ends the turn), an exception (raised
    instead of answering) or a function that receives the request and returns a Response.
    """

    def __init__(self, script: Iterable[Script]) -> None:
        self._script = list(script)
        self.requests: list[Request] = []

    def complete(self, request: Request) -> Response:
        self.requests.append(request)
        if not self._script:
            raise AssertionError("the scripted model was called more times than expected")
        item = self._script.pop(0)
        if isinstance(item, BaseException):
            raise item
        if isinstance(item, str):
            return Response((TextBlock(item),))
        if isinstance(item, Response):
            return item
        return item(request)
