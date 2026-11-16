from triage import parse_response, validate


def test_a_valid_answer_has_no_problems():
    assert validate({"category": "bug", "priority": "high", "summary": "App crashes"}) == []


def test_parse_a_plain_answer():
    answer = parse_response('{"category": "bug", "priority": "low", "summary": " Typo "}')
    assert answer == {"category": "bug", "priority": "low", "summary": "Typo"}
