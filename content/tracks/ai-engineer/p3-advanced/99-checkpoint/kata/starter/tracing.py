"""A tiny in-memory tracer in the shape of OpenTelemetry spans. Given: do not edit.

    with tracer.span("chat fake-model", **{"gen_ai.operation.name": "chat"}) as span:
        response = model.complete(request)
        span.set("gen_ai.usage.input_tokens", response.usage.input_tokens)

A span opened inside another one records the outer span's name as its parent. When an exception
leaves the `with` block, the span gets the status "error" and the attribute "error.type" (the
exception's class name), and the exception goes on.
"""

from collections.abc import Iterator
from contextlib import contextmanager
from dataclasses import dataclass, field
from typing import Any

from clock import Clock


@dataclass
class Span:
    name: str
    parent: str | None
    start: float
    attributes: dict[str, Any] = field(default_factory=dict)
    end: float | None = None
    status: str = "ok"

    def set(self, key: str, value: Any) -> None:
        self.attributes[key] = value


class Tracer:
    def __init__(self, clock: Clock) -> None:
        self._clock = clock
        self._open: list[Span] = []
        self.spans: list[Span] = []  # finished spans, in the order they ended

    @contextmanager
    def span(self, name: str, **attributes: Any) -> Iterator[Span]:
        parent = self._open[-1].name if self._open else None
        span = Span(name, parent, self._clock.now(), dict(attributes))
        self._open.append(span)
        try:
            yield span
        except BaseException as exc:
            span.status = "error"
            span.attributes.setdefault("error.type", type(exc).__name__)
            raise
        finally:
            span.end = self._clock.now()
            self._open.pop()
            self.spans.append(span)

    def named(self, prefix: str) -> list[Span]:
        return [span for span in self.spans if span.name.startswith(prefix)]
