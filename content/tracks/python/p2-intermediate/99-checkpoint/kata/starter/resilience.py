"""Small resilience tools: retries, a circuit breaker and a timer."""

import time
from collections.abc import Callable, Iterator
from contextlib import contextmanager
from dataclasses import dataclass
from types import TracebackType


class ResilienceError(Exception):
    """Base class for the errors of this module."""


class RetryExhausted(ResilienceError):
    def __init__(self, name: str, attempts: int) -> None:
        super().__init__(f"{name} failed after {attempts} attempts")
        self.attempts = attempts


class CircuitOpen(ResilienceError):
    """The breaker is open: the call was not attempted."""


def retry[**P, R](
    times: int = 3,
    *,
    on: type[BaseException] | tuple[type[BaseException], ...] = Exception,
    delay: float = 0.0,
    backoff: float = 2.0,
    sleep: Callable[[float], object] = time.sleep,
) -> Callable[[Callable[P, R]], Callable[P, R]]:
    """Retry a function up to `times` attempts when it raises one of `on`."""
    raise NotImplementedError("todo: write retry()")


class CircuitBreaker:
    """Stops calling a failing dependency for a while, then tries it again once."""

    def __init__(self, threshold: int = 3, reset_after: float = 30.0, *,
                 clock: Callable[[], float] = time.monotonic) -> None:
        raise NotImplementedError("todo: write CircuitBreaker()")

    @property
    def state(self) -> str:
        raise NotImplementedError("todo: write CircuitBreaker.state")

    def __enter__(self) -> "CircuitBreaker":
        raise NotImplementedError("todo: write CircuitBreaker.__enter__")

    def __exit__(self, exc_type: type[BaseException] | None, exc: BaseException | None,
                 traceback: TracebackType | None) -> bool:
        raise NotImplementedError("todo: write CircuitBreaker.__exit__")

    def __call__[**P, R](self, func: Callable[P, R]) -> Callable[P, R]:
        raise NotImplementedError("todo: make the breaker work as a decorator")


@dataclass
class Timing:
    elapsed: float | None = None


@contextmanager
def timer(clock: Callable[[], float] = time.perf_counter) -> Iterator[Timing]:
    """Measure the block; `elapsed` is set when it ends, even if it raises."""
    raise NotImplementedError("todo: write timer()")
    yield Timing()
