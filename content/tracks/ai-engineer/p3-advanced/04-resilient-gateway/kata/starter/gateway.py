"""A model gateway: retries with backoff, a token bucket, an answer cache, fallback and costs."""

import random
from collections.abc import Callable
from dataclasses import dataclass

from clock import Clock, SystemClock
from llm import Model, Request, Response, Usage


class ModelAPIError(Exception):
    retryable = False


class RateLimitError(ModelAPIError):
    retryable = True

    def __init__(self, message: str = "rate limited", retry_after: float | None = None) -> None:
        super().__init__(message)
        self.retry_after = retry_after


class OverloadedError(ModelAPIError):
    retryable = True


class ServerError(ModelAPIError):
    retryable = True


class APITimeoutError(ModelAPIError):
    retryable = True


class BadRequestError(ModelAPIError):
    retryable = False


@dataclass(frozen=True)
class Price:
    input_per_million: float
    output_per_million: float


@dataclass(frozen=True)
class Route:
    name: str
    model: Model
    price: Price


@dataclass(frozen=True)
class GatewayResult:
    response: Response
    route: str
    attempts: int
    cost: float
    cached: bool = False


class GatewayError(Exception):
    def __init__(self, message: str, attempts: int) -> None:
        super().__init__(message)
        self.attempts = attempts


def backoff_delay(attempt: int, base: float = 0.5, cap: float = 8.0, rng: Callable[[], float] = random.random) -> float:
    raise NotImplementedError("todo: write backoff_delay()")


def estimate_tokens(request: Request) -> int:
    raise NotImplementedError("todo: write estimate_tokens()")


def cache_key(request: Request) -> str:
    raise NotImplementedError("todo: write cache_key()")


def cost(usage: Usage, price: Price) -> float:
    raise NotImplementedError("todo: write cost()")


class TokenBucket:
    def __init__(self, capacity: float, refill_per_second: float, clock: Clock) -> None:
        raise NotImplementedError("todo: write TokenBucket")

    def available(self) -> float:
        raise NotImplementedError("todo: write TokenBucket.available()")

    def try_take(self, n: float) -> bool:
        raise NotImplementedError("todo: write TokenBucket.try_take()")

    def take(self, n: float) -> None:
        raise NotImplementedError("todo: write TokenBucket.take()")


class Gateway:
    def __init__(
        self,
        primary: Route,
        fallback: Route | None = None,
        clock: Clock | None = None,
        rng: Callable[[], float] = random.random,
        max_retries: int = 3,
        bucket: TokenBucket | None = None,
    ) -> None:
        self.primary = primary
        self.fallback = fallback
        self.clock = clock or SystemClock()
        self.rng = rng
        self.max_retries = max_retries
        self.bucket = bucket
        self.spent: dict[str, float] = {}

    def complete(self, request: Request) -> GatewayResult:
        raise NotImplementedError("todo: write Gateway.complete()")
