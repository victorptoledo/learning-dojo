import json

import pytest

from extract import ExtractionError, extract, find_json, validate
from llm import MAX_TOKENS, REFUSAL, Response, ScriptedModel, TextBlock

TICKET = {
    "type": "object",
    "properties": {
        "category": {"type": "string", "enum": ["billing", "shipping", "other"]},
        "urgency": {"type": "integer", "minimum": 1, "maximum": 5},
        "order_id": {"type": ["string", "null"]},
        "tags": {"type": "array", "items": {"type": "string"}},
    },
    "required": ["category", "urgency", "order_id"],
    "additionalProperties": False,
}

GOOD = {"category": "billing", "urgency": 4, "order_id": "A-1234", "tags": ["refund"]}


# ---- find_json -----------------------------------------------------------------------------


def test_find_in_a_json_fence():
    reply = 'Sure! Here it is:\n```json\n{"a": 1}\n```\nAnything else?'
    assert find_json(reply) == {"a": 1}


def test_find_in_a_plain_fence():
    assert find_json('Result:\n```\n{"a": [1, 2]}\n```') == {"a": [1, 2]}


def test_find_the_first_balanced_object_in_prose():
    reply = 'The data is {"a": {"b": 2}} and also {"c": 3}.'
    assert find_json(reply) == {"a": {"b": 2}}


def test_find_ignores_braces_inside_strings():
    reply = 'Answer: {"note": "use {braces} and \\"quotes\\" }", "n": 1} done'
    assert find_json(reply) == {"note": 'use {braces} and "quotes" }', "n": 1}


def test_find_skips_values_that_are_not_objects():
    assert find_json('[1, 2] then {"ok": true}') == {"ok": True}


def test_find_skips_a_broken_candidate():
    assert find_json('{"broken": } but {"fine": 1}') == {"fine": 1}


@pytest.mark.parametrize("reply", ["no json here", "[1, 2, 3]", '{"unclosed": 1', ""])
def test_find_raises_when_there_is_no_object(reply):
    with pytest.raises(ExtractionError):
        find_json(reply)


# ---- validate ----------------------------------------------------------------------------


def test_validate_accepts_good_data():
    assert validate(GOOD, TICKET) == []
    assert validate({**GOOD, "order_id": None}, TICKET) == []


def test_validate_types_and_booleans():
    assert validate(True, {"type": "integer"}) == ["$: expected integer, got boolean"]
    assert validate(True, {"type": "number"}) == ["$: expected number, got boolean"]
    assert validate(3, {"type": "number"}) == []
    assert validate(2.5, {"type": "integer"}) == ["$: expected integer, got number"]
    assert validate([], {"type": ["string", "null"]}) == ["$: expected string or null, got array"]
    assert validate(None, {"type": "object"}) == ["$: expected object, got null"]


def test_validate_enum_and_range():
    assert validate("urgent", {"enum": ["low", "high"]}) == ['$: "urgent" is not one of ["low", "high"]']
    assert validate(0, {"type": "integer", "minimum": 1}) == ["$: 0 is less than the minimum 1"]
    assert validate(9, {"type": "integer", "maximum": 5}) == ["$: 9 is greater than the maximum 5"]


def test_validate_reports_nested_paths():
    data = {"category": "food", "urgency": "4", "tags": ["ok", 7], "color": "red"}
    assert sorted(validate(data, TICKET)) == sorted(
        [
            '$.category: "food" is not one of ["billing", "shipping", "other"]',
            "$.urgency: expected integer, got string",
            "$.tags[1]: expected string, got integer",
            "$: missing required property 'order_id'",
            "$: unexpected property 'color'",
        ]
    )


def test_validate_stops_at_a_wrong_type():
    assert validate("not an object", TICKET) == ["$: expected object, got string"]


def test_validate_without_additional_properties_rule_allows_extras():
    assert validate({"a": 1, "b": 2}, {"type": "object", "properties": {"a": {"type": "integer"}}}) == []


# ---- extract -------------------------------------------------------------------------------


def test_extract_first_try():
    model = ScriptedModel([f"```json\n{json.dumps(GOOD)}\n```"])
    assert extract(model, "I was charged twice for A-1234!", TICKET) == GOOD
    request = model.requests[0]
    assert json.dumps(TICKET) in request.system
    assert [(m.role, m.content) for m in request.messages] == [("user", "I was charged twice for A-1234!")]


def test_extract_repairs_with_the_errors():
    bad = '{"category": "billing", "urgency": "high", "order_id": null}'
    model = ScriptedModel([bad, json.dumps(GOOD)])
    assert extract(model, "email text", TICKET) == GOOD
    second = model.requests[1].messages
    assert [m.role for m in second] == ["user", "assistant", "user"]
    assert second[1].content == bad
    repair = second[2].content
    assert repair.startswith("Your reply did not match the schema:")
    assert "- $.urgency: expected integer, got string" in repair.splitlines()
    assert repair.rstrip().endswith("Reply with corrected JSON only.")


def test_extract_handles_prose_without_json():
    model = ScriptedModel(["I cannot tell from this email.", json.dumps(GOOD)])
    assert extract(model, "email", TICKET) == GOOD
    assert "- no JSON object found" in model.requests[1].messages[2].content.splitlines()


def test_extract_treats_truncation_as_a_problem():
    cut = Response((TextBlock('{"category": "billing", "urg'),), MAX_TOKENS)
    model = ScriptedModel([cut, json.dumps(GOOD)])
    assert extract(model, "email", TICKET) == GOOD
    assert "- the reply was cut off (max_tokens)" in model.requests[1].messages[2].content.splitlines()


def test_extract_gives_up_after_max_attempts():
    model = ScriptedModel(['{"category": "x"}', '{"category": "y"}'])
    with pytest.raises(ExtractionError) as caught:
        extract(model, "email", TICKET, max_attempts=2)
    assert caught.value.attempts == 2
    assert '$.category: "y" is not one of ["billing", "shipping", "other"]' in caught.value.errors
    assert len(model.requests) == 2
    assert len(model.requests[1].messages) == 3


def test_extract_history_grows_each_attempt():
    model = ScriptedModel(["nope", "still nope", json.dumps(GOOD)])
    extract(model, "email", TICKET)
    assert [len(r.messages) for r in model.requests] == [1, 3, 5]


def test_extract_does_not_retry_a_refusal():
    model = ScriptedModel([Response((TextBlock("I can't help with that."),), REFUSAL)])
    with pytest.raises(ExtractionError) as caught:
        extract(model, "email", TICKET)
    assert caught.value.attempts == 1
    assert len(model.requests) == 1


def test_extract_rejects_bad_attempt_limits():
    with pytest.raises(ValueError):
        extract(ScriptedModel([]), "email", TICKET, max_attempts=0)
