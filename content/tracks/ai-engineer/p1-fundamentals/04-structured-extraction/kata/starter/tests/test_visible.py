from extract import find_json, validate


def test_finds_a_bare_object():
    assert find_json('{"category": "billing"}') == {"category": "billing"}


def test_reports_a_wrong_type():
    assert validate("high", {"type": "integer"}) == ["$: expected integer, got string"]
