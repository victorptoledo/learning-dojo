import json

import pytest

from llm import END_TURN, MAX_TOKENS, TOOL_CALL, TextBlock, ToolCall, Usage
from stream import StreamError, assemble, parse_sse, stream_text


def sse(*events):
    """Render events as SSE lines, the way a provider sends them."""
    lines = []
    for event in events:
        lines.append(f"event: {event['type']}\n")
        lines.append(f"data: {json.dumps(event)}\n")
        lines.append("\n")
    return lines


START = {"type": "message_start", "usage": {"input_tokens": 25}}


def stop(reason=END_TURN, output=9):
    return {"type": "message_stop", "stop_reason": reason, "usage": {"output_tokens": output}}


def text_block(index, *pieces):
    events = [{"type": "block_start", "index": index, "block": {"type": "text"}}]
    events += [{"type": "text_delta", "index": index, "text": piece} for piece in pieces]
    events.append({"type": "block_stop", "index": index})
    return events


def tool_block(index, call_id, name, *fragments):
    block = {"type": "tool_call", "id": call_id, "name": name}
    events = [{"type": "block_start", "index": index, "block": block}]
    events += [{"type": "arguments_delta", "index": index, "json": f} for f in fragments]
    events.append({"type": "block_stop", "index": index})
    return events


# ---- parse_sse ---------------------------------------------------------------------------


def test_parse_handles_crlf_and_event_fields():
    lines = [line.replace("\n", "\r\n") for line in sse(START, stop())]
    assert list(parse_sse(lines)) == [START, stop()]


def test_parse_accepts_lines_without_line_ends():
    lines = ['data: {"type": "ping"}', "", "data:{\"type\": \"ping\"}", ""]
    assert list(parse_sse(lines)) == [{"type": "ping"}, {"type": "ping"}]


def test_parse_joins_multiple_data_lines():
    lines = ["data: {\"type\":", "data: \"ping\"}", ""]
    assert list(parse_sse(lines)) == [{"type": "ping"}]


def test_parse_ignores_comments_and_empty_events():
    lines = [": keep-alive", "", "", "id: 7", "retry: 1000", 'data: {"type": "ping"}', ""]
    assert list(parse_sse(lines)) == [{"type": "ping"}]


def test_parse_drops_an_incomplete_final_event():
    lines = ['data: {"type": "ping"}', "", 'data: {"type": "message_stop"}']
    assert list(parse_sse(lines)) == [{"type": "ping"}]


def test_parse_rejects_invalid_json():
    with pytest.raises(StreamError):
        list(parse_sse(["data: {not json", ""]))


def test_parse_is_lazy():
    def lines():
        yield 'data: {"type": "ping"}'
        yield ""
        raise AssertionError("parse_sse read past the first event")

    assert next(iter(parse_sse(lines()))) == {"type": "ping"}


# ---- assemble ------------------------------------------------------------------------------


def test_assemble_text_with_usage_and_stop_reason():
    events = [START, *text_block(0, "The answer", " is 42."), stop(MAX_TOKENS, 4)]
    response = assemble(events)
    assert response.content == (TextBlock("The answer is 42."),)
    assert response.stop_reason == MAX_TOKENS
    assert response.usage == Usage(25, 4)


def test_assemble_a_tool_call_from_fragments():
    events = [
        START,
        *text_block(0, "Let me check."),
        *tool_block(1, "call_1", "get_weather", '{"city": "Lis', 'bon", "days"', ": 3}"),
        stop(TOOL_CALL),
    ]
    response = assemble(events)
    assert response.content == (
        TextBlock("Let me check."),
        ToolCall("call_1", "get_weather", {"city": "Lisbon", "days": 3}),
    )
    assert response.stop_reason == TOOL_CALL


def test_assemble_tool_call_without_arguments():
    response = assemble([START, *tool_block(0, "call_9", "list_orders"), stop(TOOL_CALL)])
    assert response.content == (ToolCall("call_9", "list_orders", {}),)


def test_assemble_orders_blocks_by_index_even_when_interleaved():
    events = [
        START,
        {"type": "block_start", "index": 1, "block": {"type": "tool_call", "id": "c2", "name": "b"}},
        {"type": "block_start", "index": 0, "block": {"type": "tool_call", "id": "c1", "name": "a"}},
        {"type": "arguments_delta", "index": 1, "json": '{"x": '},
        {"type": "arguments_delta", "index": 0, "json": '{"y": 2}'},
        {"type": "arguments_delta", "index": 1, "json": "1}"},
        {"type": "block_stop", "index": 0},
        {"type": "block_stop", "index": 1},
        stop(TOOL_CALL),
    ]
    response = assemble(events)
    assert response.tool_calls == [ToolCall("c1", "a", {"y": 2}), ToolCall("c2", "b", {"x": 1})]


def test_assemble_ignores_pings_and_events_after_stop():
    events = [START, {"type": "ping"}, *text_block(0, "ok"), stop(), {"type": "ping"}, {"type": "bogus"}]
    assert assemble(events).text == "ok"


def test_assemble_raises_on_an_error_event():
    error = {"type": "error", "error": {"type": "overloaded", "message": "Overloaded, retry later"}}
    with pytest.raises(StreamError) as caught:
        assemble([START, *text_block(0, "par"), error])
    assert "overloaded" in str(caught.value)
    assert "Overloaded, retry later" in str(caught.value)


def test_assemble_raises_when_the_stream_ends_early():
    with pytest.raises(StreamError):
        assemble([START, *text_block(0, "half an ans")])


def test_assemble_raises_on_unknown_or_wrong_block():
    with pytest.raises(StreamError):
        assemble([START, {"type": "text_delta", "index": 3, "text": "x"}, stop()])
    wrong_kind = [START, *tool_block(0, "c", "t")[:1], {"type": "text_delta", "index": 0, "text": "x"}, stop()]
    with pytest.raises(StreamError):
        assemble(wrong_kind)


@pytest.mark.parametrize("fragments", [('{"city": ',), ('["Lisbon"]',)])
def test_assemble_rejects_bad_arguments(fragments):
    with pytest.raises(StreamError):
        assemble([START, *tool_block(0, "c", "t", *fragments), stop(TOOL_CALL)])


# ---- stream_text --------------------------------------------------------------------------------


def test_stream_text_calls_back_before_the_stream_ends():
    seen = []

    def events():
        yield START
        yield from text_block(0, "Hel", "lo")
        assert seen == ["Hel", "lo"], "on_text must be called as the text arrives"
        yield stop()

    response = stream_text(events(), seen.append)
    assert seen == ["Hel", "lo"]
    assert response.text == "Hello"


def test_whole_pipeline_from_lines():
    lines = sse(START, *text_block(0, "Two ", "blocks"), *tool_block(1, "c1", "search", '{"q": "rag"}'), stop(TOOL_CALL, 12))
    chunks = []
    response = stream_text(parse_sse(lines), chunks.append)
    assert chunks == ["Two ", "blocks"]
    assert response.content == (TextBlock("Two blocks"), ToolCall("c1", "search", {"q": "rag"}))
    assert response.usage == Usage(25, 12)
