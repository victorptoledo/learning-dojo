"""Small resilience tools: retries, a circuit breaker and a timer."""

import functools
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
    if times < 1:
        raise ValueError("times must be at least 1")

    def decorator(func: Callable[P, R]) -> Callable[P, R]:
        @functools.wraps(func)
        def wrapper(*args: P.args, **kwargs: P.kwargs) -> R:
            for attempt in range(1, times + 1):
                try:
                    return func(*args, **kwargs)
                except on as error:
                    if attempt == times:
                        raise RetryExhausted(func.__name__, times) from error
                    sleep(delay * backoff ** (attempt - 1))
            raise AssertionError("unreachable")

        return wrapper

    return decorator


class CircuitBreaker:
    """Stops calling a failing dependency for a while, then tries it again once."""

    def __init__(self, threshold: int = 3, reset_after: float = 30.0, *,
                 clock: Callable[[], float] = time.monotonic) -> None:
        if threshold < 1:
            raise ValueError("threshold must be at least 1")
        self.threshold = threshold
        self.reset_after = reset_after
        self.clock = clock
        self.failures = 0
        self._opened_at: float | None = None

    @property
    def state(self) -> str:
        if self._opened_at is None:
            return "closed"
        if self.clock() - self._opened_at >= self.reset_after:
            return "half-open"
        return "open"

    def __enter__(self) -> "CircuitBreaker":
        if self.state == "open":
            raise CircuitOpen("the circuit is open")
        return self

    def __exit__(self, exc_type: type[BaseException] | None, exc: BaseException | None,
                 traceback: TracebackType | None) -> bool:
        if exc_type is None:
            self.failures = 0
            self._opened_at = None
        elif issubclass(exc_type, Exception):
            self.failures += 1
            if self._opened_at is not None or self.failures >= self.threshold:
                self._opened_at = self.clock()
        return False

    def __call__[**P, R](self, func: Callable[P, R]) -> Callable[P, R]:
        @functools.wraps(func)
        def wrapper(*args: P.args, **kwargs: P.kwargs) -> R:
            with self:
                return func(*args, **kwargs)

        return wrapper


@dataclass
class Timing:
    elapsed: float | None = None


@contextmanager
def timer(clock: Callable[[], float] = time.perf_counter) -> Iterator[Timing]:
    """Measure the block; `elapsed` is set when it ends, even if it raises."""
    timing = Timing()
    start = clock()
    try:
        yield timing
    finally:
        timing.elapsed = clock() - start
