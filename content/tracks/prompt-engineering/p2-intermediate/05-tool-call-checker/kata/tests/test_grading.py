from tools import check_tool_definition, dispatch, error_result, validate_arguments

WEATHER = {
    "name": "get_weather",
    "description": "Current weather for one city. Use it when the user asks about weather now.",
    "input_schema": {
        "type": "object",
        "properties": {
            "city": {"type": "string", "description": "City name, for example Lisbon"},
            "units": {"type": "string", "enum": ["celsius", "fahrenheit"], "description": "Units"},
            "days": {"type": "integer", "minimum": 1, "maximum": 7, "description": "Forecast days"},
            "detailed": {"type": "boolean", "description": "Include wind and humidity"},
            "tags": {"type": "array", "items": {"type": "string"}, "description": "Labels"},
            "lat": {"type": "number", "description": "Latitude"},
        },
        "required": ["city", "units"],
    },
}
SCHEMA = WEATHER["input_schema"]


def test_a_good_definition_has_no_problems():
    assert check_tool_definition(WEATHER) == []


def test_definition_name_and_description():
    tool = dict(WEATHER, name="get weather!", description="  weather  ")
    assert check_tool_definition(tool) == [
        "name must be 1-64 letters, digits, '_' or '-'",
        "description must say what the tool does and when to use it (at least 20 characters)",
    ]
    assert check_tool_definition(dict(WEATHER, name="x" * 65))[0].startswith("name must be")
    assert check_tool_definition(dict(WEATHER, name="x" * 64)) == []


def test_definition_without_an_object_schema_stops_early():
    tool = {"name": "t", "description": "d" * 25, "input_schema": {"type": "string"}}
    assert check_tool_definition(tool) == ["input_schema must be an object schema"]
    assert check_tool_definition({"name": 3}) == [
        "name must be 1-64 letters, digits, '_' or '-'",
        "description must say what the tool does and when to use it (at least 20 characters)",
        "input_schema must be an object schema",
    ]


def test_definition_properties_and_required():
    tool = {
        "name": "t",
        "description": "Does a thing when asked to do a thing.",
        "input_schema": {
            "type": "object",
            "properties": {"a": {"type": "text"}, "b": {"type": "string", "description": " "}},
            "required": ["a", "c"],
        },
    }
    assert check_tool_definition(tool) == [
        "property 'a' has an unknown type",
        "property 'a' needs a description",
        "property 'b' needs a description",
        "required property 'c' is not defined",
    ]


def test_arguments_must_be_an_object():
    assert validate_arguments(SCHEMA, ["Lisbon"]) == ["arguments must be a JSON object"]
    assert validate_arguments(SCHEMA, None) == ["arguments must be a JSON object"]


def test_missing_then_unknown_in_order():
    assert validate_arguments(SCHEMA, {"zip": "1000", "town": "x"}) == [
        "missing required argument 'city'",
        "missing required argument 'units'",
        "unknown argument 'zip'",
        "unknown argument 'town'",
    ]


def test_types_and_bool_is_not_a_number():
    args = {"city": 5, "units": "celsius", "days": True, "detailed": "yes", "lat": False, "tags": "a"}
    assert validate_arguments(SCHEMA, args) == [
        "argument 'city' must be string",
        "argument 'days' must be integer",
        "argument 'detailed' must be boolean",
        "argument 'lat' must be number",
        "argument 'tags' must be array",
    ]
    assert validate_arguments(SCHEMA, {"city": "x", "units": "celsius", "lat": 38, "days": 2}) == []
    assert validate_arguments(SCHEMA, {"city": "x", "units": "celsius", "days": 2.0}) == [
        "argument 'days' must be integer"
    ]


def test_enum_range_and_items():
    args = {"city": "x", "units": "kelvin", "days": 9, "tags": ["a", 1, None]}
    assert validate_arguments(SCHEMA, args) == [
        "argument 'units' must be one of: celsius, fahrenheit",
        "argument 'days' must be at most 7",
        "argument 'tags' items must be string",
    ]
    assert validate_arguments(SCHEMA, {"city": "x", "units": "celsius", "days": 0}) == [
        "argument 'days' must be at least 1"
    ]


def test_error_result():
    assert error_result("call_1", ["a", "b"]) == {
        "type": "tool_result",
        "tool_use_id": "call_1",
        "is_error": True,
        "content": "Invalid arguments:\n- a\n- b\nFix the arguments and call the tool again.",
    }


def make_registry(calls):
    def get_weather(city, units, **extra):
        calls.append((city, units, extra))
        if city == "Atlantis":
            raise LookupError("no such city")
        return f"18 degrees {units} in {city}"

    return {"get_weather": (WEATHER, get_weather)}


def test_dispatch_success():
    calls = []
    result = dispatch({"id": "c1", "name": "get_weather", "input": {"city": "Porto", "units": "celsius"}}, make_registry(calls))
    assert result == {"type": "tool_result", "tool_use_id": "c1", "content": "18 degrees celsius in Porto"}
    assert calls == [("Porto", "celsius", {})]


def test_dispatch_unknown_tool():
    result = dispatch({"id": "c2", "name": "book_flight", "input": {}}, make_registry([]))
    assert result == error_result("c2", ["unknown tool 'book_flight'"])


def test_dispatch_invalid_arguments_never_runs_the_tool():
    calls = []
    result = dispatch({"id": "c3", "name": "get_weather", "input": {"city": "Porto", "units": "k"}}, make_registry(calls))
    assert result == error_result("c3", ["argument 'units' must be one of: celsius, fahrenheit"])
    assert calls == []


def test_dispatch_tool_failure():
    result = dispatch({"id": "c4", "name": "get_weather", "input": {"city": "Atlantis", "units": "celsius"}}, make_registry([]))
    assert result == {"type": "tool_result", "tool_use_id": "c4", "is_error": True, "content": "Tool failed: no such city"}
