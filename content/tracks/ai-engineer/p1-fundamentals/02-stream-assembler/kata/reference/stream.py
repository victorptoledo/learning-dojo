"""Turn a server-sent events stream from a model API into a Response."""

import json
from collections.abc import Callable, Iterable, Iterator
from typing import Any

from llm import Response, TextBlock, ToolCall, Usage


class StreamError(Exception):
    """The stream is malformed, incomplete, or the provider reported an error."""


def _decode(data: str) -> dict[str, Any]:
    try:
        return json.loads(data)
    except json.JSONDecodeError as error:
        raise StreamError(f"invalid event data: {data!r}") from error


def parse_sse(lines: Iterable[str]) -> Iterator[dict[str, Any]]:
    data: list[str] = []
    for raw in lines:
        line = raw.rstrip("\r\n")
        if line == "":
            if data:
                yield _decode("\n".join(data))
                data = []
        elif line.startswith(":"):
            continue
        elif line.startswith("data:"):
            value = line[len("data:") :]
            data.append(value[1:] if value.startswith(" ") else value)
    # data left here belongs to an incomplete event: dropped


def assemble(events: Iterable[dict[str, Any]]) -> Response:
    blocks: dict[int, dict[str, Any]] = {}
    input_tokens = 0
    for event in events:
        kind = event.get("type")
        if kind == "message_start":
            input_tokens = event.get("usage", {}).get("input_tokens", 0)
        elif kind == "block_start":
            blocks[event["index"]] = {**event["block"], "pieces": []}
        elif kind in ("text_delta", "arguments_delta"):
            block = blocks.get(event["index"])
            expected = "text" if kind == "text_delta" else "tool_call"
            if block is None or block["type"] != expected:
                raise StreamError(f"{kind} for unknown or wrong block {event['index']}")
            block["pieces"].append(event["text"] if kind == "text_delta" else event["json"])
        elif kind == "error":
            error = event.get("error", {})
            raise StreamError(f"provider error {error.get('type')}: {error.get('message')}")
        elif kind == "message_stop":
            content = tuple(_finish(blocks[index]) for index in sorted(blocks))
            usage = Usage(input_tokens, event.get("usage", {}).get("output_tokens", 0))
            return Response(content, event["stop_reason"], usage)
    raise StreamError("the stream ended before message_stop")


def _finish(block: dict[str, Any]) -> TextBlock | ToolCall:
    joined = "".join(block["pieces"])
    if block["type"] == "text":
        return TextBlock(joined)
    try:
        arguments = json.loads(joined) if joined else {}
    except json.JSONDecodeError as error:
        raise StreamError(f"tool call {block['id']}: arguments are not valid JSON") from error
    if not isinstance(arguments, dict):
        raise StreamError(f"tool call {block['id']}: arguments must be a JSON object")
    return ToolCall(block["id"], block["name"], arguments)


def stream_text(events: Iterable[dict[str, Any]], on_text: Callable[[str], None]) -> Response:
    def tap() -> Iterator[dict[str, Any]]:
        for event in events:
            if event.get("type") == "text_delta":
                on_text(event["text"])
            yield event

    return assemble(tap())
