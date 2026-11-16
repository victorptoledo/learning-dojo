from tools import validate_arguments

SCHEMA = {
    "type": "object",
    "properties": {"city": {"type": "string", "description": "City name"}},
    "required": ["city"],
}


def test_valid_arguments():
    assert validate_arguments(SCHEMA, {"city": "Lisbon"}) == []


def test_missing_argument():
    assert validate_arguments(SCHEMA, {}) == ["missing required argument 'city'"]
