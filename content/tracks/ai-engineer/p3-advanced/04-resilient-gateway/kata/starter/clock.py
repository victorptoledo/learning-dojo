"""Time, injected so that retry and rate-limit logic can be tested. Given: do not edit."""

import time
from typing import Protocol


class Clock(Protocol):
    def now(self) -> float: ...

    def sleep(self, seconds: float) -> None: ...


class SystemClock:
    def now(self) -> float:
        return time.monotonic()

    def sleep(self, seconds: float) -> None:
        time.sleep(seconds)


class FakeClock:
    """A clock for tests: sleep() moves time forward at once and records the delay."""

    def __init__(self, start: float = 0.0) -> None:
        self.time = start
        self.sleeps: list[float] = []

    def now(self) -> float:
        return self.time

    def sleep(self, seconds: float) -> None:
        if seconds < 0:
            raise ValueError("cannot sleep a negative time")
        self.sleeps.append(seconds)
        self.time += seconds

    def advance(self, seconds: float) -> None:
        self.time += seconds
