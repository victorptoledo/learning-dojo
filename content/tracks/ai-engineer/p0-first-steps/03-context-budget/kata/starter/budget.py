"""Fit a conversation into a context window and price the requests."""

from collections.abc import Sequence
from dataclasses import dataclass
from decimal import Decimal

from tokenizer import count_tokens  # noqa: F401 (your functions use it)

MESSAGE_OVERHEAD = 4


@dataclass(frozen=True)
class Message:
    role: str
    content: str


@dataclass(frozen=True)
class Price:
    input_per_million: Decimal
    output_per_million: Decimal


class ContextOverflowError(Exception):
    def __init__(self, needed: int, available: int) -> None:
        super().__init__(f"the newest message needs {needed} tokens, {available} are available")
        self.needed = needed
        self.available = available


def message_tokens(message: Message) -> int:
    raise NotImplementedError("todo: write message_tokens()")


def request_tokens(system: str, messages: Sequence[Message]) -> int:
    raise NotImplementedError("todo: write request_tokens()")


def fit_history(system: str, messages: Sequence[Message], window: int, reserve: int) -> list[Message]:
    raise NotImplementedError("todo: write fit_history()")


def estimate_cost(input_tokens: int, output_tokens: int, price: Price) -> Decimal:
    raise NotImplementedError("todo: write estimate_cost()")


def conversation_cost(system: str, turns: Sequence[tuple[str, str]], price: Price) -> Decimal:
    raise NotImplementedError("todo: write conversation_cost()")
