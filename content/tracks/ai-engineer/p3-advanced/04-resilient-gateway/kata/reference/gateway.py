"""A model gateway: retries with backoff, a token bucket, an answer cache, fallback and costs."""

import dataclasses
import hashlib
import json
import random
from collections.abc import Callable
from dataclasses import dataclass

from clock import Clock, SystemClock
from llm import END_TURN, Model, Request, Response, TextBlock, ToolResult, Usage


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
    return rng() * min(cap, base * 2**attempt)


def estimate_tokens(request: Request) -> int:
    characters = len(request.system)
    for message in request.messages:
        if isinstance(message.content, str):
            characters += len(message.content)
            continue
        for block in message.content:
            if isinstance(block, TextBlock):
                characters += len(block.text)
            elif isinstance(block, ToolResult):
                characters += len(block.content)
    return characters // 4 + request.max_tokens


def cache_key(request: Request) -> str:
    payload = json.dumps(dataclasses.asdict(request), sort_keys=True, default=str)
    return hashlib.sha256(payload.encode("utf-8")).hexdigest()


def cost(usage: Usage, price: Price) -> float:
    return usage.input_tokens * price.input_per_million / 1e6 + usage.output_tokens * price.output_per_million / 1e6


class TokenBucket:
    def __init__(self, capacity: float, refill_per_second: float, clock: Clock) -> None:
        self.capacity = capacity
        self.rate = refill_per_second
        self.clock = clock
        self._tokens = float(capacity)
        self._updated = clock.now()

    def _refill(self) -> None:
        now = self.clock.now()
        self._tokens = min(self.capacity, self._tokens + (now - self._updated) * self.rate)
        self._updated = now

    def available(self) -> float:
        self._refill()
        return self._tokens

    def try_take(self, n: float) -> bool:
        self._refill()
        if self._tokens >= n:
            self._tokens -= n
            return True
        return False

    def take(self, n: float) -> None:
        if n > self.capacity:
            raise ValueError(f"cannot take {n} tokens from a bucket of {self.capacity}")
        self._refill()
        if self._tokens < n:
            self.clock.sleep((n - self._tokens) / self.rate)
            self._refill()
        self._tokens = max(0.0, self._tokens - n)


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
        self._cache: dict[str, tuple[Response, str]] = {}

    def complete(self, request: Request) -> GatewayResult:
        key = cache_key(request)
        if key in self._cache:
            response, route_name = self._cache[key]
            return GatewayResult(response, route_name, 0, 0.0, True)
        routes = [self.primary] if self.fallback is None else [self.primary, self.fallback]
        attempts = 0
        last: Exception | None = None
        for route in routes:
            for attempt in range(self.max_retries + 1):
                if self.bucket is not None:
                    self.bucket.take(estimate_tokens(request))
                attempts += 1
                try:
                    response = route.model.complete(request)
                except ModelAPIError as error:
                    if not error.retryable:
                        raise
                    last = error
                    if attempt < self.max_retries:
                        delay = backoff_delay(attempt, rng=self.rng)
                        retry_after = getattr(error, "retry_after", None)
                        if retry_after is not None:
                            delay = max(delay, retry_after)
                        self.clock.sleep(delay)
                    continue
                spent = cost(response.usage, route.price)
                self.spent[route.name] = self.spent.get(route.name, 0.0) + spent
                if response.stop_reason == END_TURN:
                    self._cache[key] = (response, route.name)
                return GatewayResult(response, route.name, attempts, spent, False)
        raise GatewayError(f"all routes failed after {attempts} attempts: {last}", attempts) from last
