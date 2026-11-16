from llm import TextBlock
from stream import assemble, parse_sse


def test_parses_one_event():
    lines = ['data: {"type": "ping"}', ""]
    assert list(parse_sse(lines)) == [{"type": "ping"}]


def test_assembles_a_text_answer():
    events = [
        {"type": "message_start", "usage": {"input_tokens": 10}},
        {"type": "block_start", "index": 0, "block": {"type": "text"}},
        {"type": "text_delta", "index": 0, "text": "Hello"},
        {"type": "text_delta", "index": 0, "text": ", world"},
        {"type": "block_stop", "index": 0},
        {"type": "message_stop", "stop_reason": "end_turn", "usage": {"output_tokens": 3}},
    ]
    response = assemble(events)
    assert response.content == (TextBlock("Hello, world"),)
