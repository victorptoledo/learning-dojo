"""Fit a conversation into a context window and price the requests."""

from collections.abc import Sequence
from dataclasses import dataclass
from decimal import ROUND_HALF_UP, Decimal

from tokenizer import count_tokens

MESSAGE_OVERHEAD = 4
MICRO = Decimal("0.000001")


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
    return count_tokens(message.content) + MESSAGE_OVERHEAD


def request_tokens(system: str, messages: Sequence[Message]) -> int:
    return count_tokens(system) + MESSAGE_OVERHEAD + sum(message_tokens(m) for m in messages)


def fit_history(system: str, messages: Sequence[Message], window: int, reserve: int) -> list[Message]:
    if reserve < 0 or reserve >= window:
        raise ValueError("reserve must be at least 0 and smaller than the window")
    if not messages:
        return []
    budget = window - reserve - (count_tokens(system) + MESSAGE_OVERHEAD)
    kept: list[Message] = []
    used = 0
    for message in reversed(messages):
        cost = message_tokens(message)
        if used + cost > budget:
            break
        kept.append(message)
        used += cost
    if not kept:
        raise ContextOverflowError(message_tokens(messages[-1]), max(budget, 0))
    kept.reverse()
    while kept and kept[0].role == "assistant":
        kept.pop(0)
    return kept


def estimate_cost(input_tokens: int, output_tokens: int, price: Price) -> Decimal:
    if input_tokens < 0 or output_tokens < 0:
        raise ValueError("token counts cannot be negative")
    total = input_tokens * price.input_per_million + output_tokens * price.output_per_million
    return (total / Decimal(1_000_000)).quantize(MICRO, rounding=ROUND_HALF_UP)


def conversation_cost(system: str, turns: Sequence[tuple[str, str]], price: Price) -> Decimal:
    history: list[Message] = []
    total = Decimal("0")
    for user_text, assistant_text in turns:
        request = [*history, Message("user", user_text)]
        total += estimate_cost(request_tokens(system, request), count_tokens(assistant_text), price)
        history = [*request, Message("assistant", assistant_text)]
    return total
