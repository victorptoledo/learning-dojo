from agent import Tool, check_arguments, run_tool
from llm import ToolCall, ToolResult

SCHEMA = {"type": "object", "properties": {"city": {"type": "string"}}, "required": ["city"]}


def test_checks_a_missing_argument():
    assert check_arguments({}, SCHEMA) == ["missing required argument 'city'"]


def test_runs_a_tool():
    tool = Tool("weather", "Weather for a city", SCHEMA, lambda city: f"Sunny in {city}")
    assert run_tool({"weather": tool}, ToolCall("c1", "weather", {"city": "Porto"})) == ToolResult("c1", "Sunny in Porto")
