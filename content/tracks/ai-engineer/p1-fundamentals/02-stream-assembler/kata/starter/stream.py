"""Turn a server-sent events stream from a model API into a Response."""

from collections.abc import Callable, Iterable, Iterator
from typing import Any

from llm import Response


class StreamError(Exception):
    """The stream is malformed, incomplete, or the provider reported an error."""


def parse_sse(lines: Iterable[str]) -> Iterator[dict[str, Any]]:
    raise NotImplementedError("todo: write parse_sse()")


def assemble(events: Iterable[dict[str, Any]]) -> Response:
    raise NotImplementedError("todo: write assemble()")


def stream_text(events: Iterable[dict[str, Any]], on_text: Callable[[str], None]) -> Response:
    raise NotImplementedError("todo: write stream_text()")
