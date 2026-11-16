import pytest

from agent import (
    AgentError,
    BudgetExceededError,
    RepeatedCallsError,
    StepLimitError,
    Tool,
    check_arguments,
    run_agent,
    run_tool,
)
from llm import (
    END_TURN,
    MAX_TOKENS,
    REFUSAL,
    TOOL_CALL,
    Response,
    ScriptedModel,
    TextBlock,
    ToolCall,
    ToolResult,
    Usage,
)

WEATHER_SCHEMA = {
    "type": "object",
    "properties": {"city": {"type": "string"}, "days": {"type": "integer"}},
    "required": ["city"],
    "additionalProperties": False,
}
ORDERS = {"A-1": "shipped", "A-2": "processing"}


def weather(city, days=1):
    return f"{city}: sunny for {days} day(s)"


def order_status(order_id):
    return {"order_id": order_id, "status": ORDERS[order_id]}


TOOLS = {
    "get_weather": Tool("get_weather", "Weather forecast for a city", WEATHER_SCHEMA, weather),
    "order_status": Tool(
        "order_status",
        "Status of an order by id",
        {"type": "object", "properties": {"order_id": {"type": "string"}}, "required": ["order_id"]},
        order_status,
    ),
}


def calls(*items, usage=Usage(100, 20)):
    content = tuple(ToolCall(f"c{i}", name, args) for i, (name, args) in enumerate(items, start=1))
    return Response(content, TOOL_CALL, usage)


def final(text, usage=Usage(150, 30)):
    return Response((TextBlock(text),), END_TURN, usage)


# ---- check_arguments ------------------------------------------------------------------------


def test_check_accepts_good_arguments():
    assert check_arguments({"city": "Lisbon", "days": 3}, WEATHER_SCHEMA) == []


def test_check_reports_types_missing_and_unexpected():
    errors = check_arguments({"days": "3", "units": "C"}, WEATHER_SCHEMA)
    assert sorted(errors) == sorted(
        [
            "missing required argument 'city'",
            "argument 'days': expected integer, got string",
            "unexpected argument 'units'",
        ]
    )


def test_check_booleans_and_numbers():
    schema = {"type": "object", "properties": {"n": {"type": "number"}, "flag": {"type": "boolean"}}}
    assert check_arguments({"n": True}, schema) == ["argument 'n': expected number, got boolean"]
    assert check_arguments({"n": 3, "flag": False}, schema) == []
    assert check_arguments({"flag": 1}, schema) == ["argument 'flag': expected boolean, got integer"]
    assert check_arguments({"extra": [1]}, schema) == []


# ---- run_tool -------------------------------------------------------------------------------


def test_run_tool_unknown_tool():
    result = run_tool(TOOLS, ToolCall("c9", "delete_everything", {}))
    assert result == ToolResult("c9", "unknown tool 'delete_everything'. Available tools: get_weather, order_status", True)


def test_run_tool_invalid_arguments_do_not_call_the_tool():
    called = []
    tool = Tool("t", "", WEATHER_SCHEMA, lambda **kwargs: called.append(kwargs))
    result = run_tool({"t": tool}, ToolCall("c1", "t", {"city": 5}))
    assert result.is_error
    assert result.call_id == "c1"
    assert result.content == "invalid arguments: argument 'city': expected string, got integer"
    assert called == []


def test_run_tool_turns_exceptions_into_error_results():
    result = run_tool(TOOLS, ToolCall("c2", "order_status", {"order_id": "A-9"}))
    assert result == ToolResult("c2", "KeyError: 'A-9'", True)


def test_run_tool_serialises_non_string_results():
    result = run_tool(TOOLS, ToolCall("c3", "order_status", {"order_id": "A-1"}))
    assert result == ToolResult("c3", '{"order_id": "A-1", "status": "shipped"}', False)


# ---- run_agent -------------------------------------------------------------------------------


def test_agent_answers_without_tools():
    model = ScriptedModel([final("Hello!")])
    result = run_agent(model, TOOLS, "Hi")
    assert result.text == "Hello!"
    assert result.steps == 1
    assert result.stopped == "done"
    assert [m.role for m in result.messages] == ["user", "assistant"]


def test_agent_sends_tool_specs_and_system():
    model = ScriptedModel([final("ok")])
    run_agent(model, TOOLS, "Hi", system="Be brief.")
    request = model.requests[0]
    assert request.system == "Be brief."
    assert [spec.name for spec in request.tools] == ["get_weather", "order_status"]
    assert request.tools[0].parameters == WEATHER_SCHEMA


def test_agent_runs_a_tool_and_returns_the_answer():
    model = ScriptedModel([calls(("get_weather", {"city": "Porto", "days": 2})), final("Sunny in Porto.")])
    result = run_agent(model, TOOLS, "Weather in Porto?")
    assert result.text == "Sunny in Porto."
    assert result.steps == 2
    second = model.requests[1].messages
    assert [m.role for m in second] == ["user", "assistant", "user"]
    assert second[1].content == calls(("get_weather", {"city": "Porto", "days": 2})).content
    assert second[2].content == (ToolResult("c1", "Porto: sunny for 2 day(s)"),)


def test_agent_answers_parallel_calls_in_one_message():
    model = ScriptedModel(
        [
            calls(("order_status", {"order_id": "A-1"}), ("order_status", {"order_id": "A-2"}), ("nope", {})),
            final("A-1 shipped, A-2 processing."),
        ]
    )
    result = run_agent(model, TOOLS, "Where are my orders?")
    results = result.messages[2].content
    assert [r.call_id for r in results] == ["c1", "c2", "c3"]
    assert [r.is_error for r in results] == [False, False, True]
    assert len(result.messages) == 4


def test_agent_lets_the_model_recover_from_an_error():
    model = ScriptedModel(
        [
            calls(("order_status", {"order_id": "A-9"})),
            calls(("order_status", {"order_id": "A-1"})),
            final("Your order A-1 has shipped."),
        ]
    )
    result = run_agent(model, TOOLS, "Status of my order?")
    assert result.messages[2].content == (ToolResult("c1", "KeyError: 'A-9'", True),)
    assert result.steps == 3


def test_agent_adds_up_usage():
    model = ScriptedModel([calls(("get_weather", {"city": "Faro"}), usage=Usage(10, 2)), final("Sunny", usage=Usage(20, 3))])
    assert run_agent(model, TOOLS, "Faro?").usage == Usage(30, 5)


def test_agent_stop_sequence_and_refusal():
    stop_seq = Response((TextBlock("Answer: 42"),), "stop_sequence")
    assert run_agent(ScriptedModel([stop_seq]), TOOLS, "q").stopped == "done"
    refusal = Response((TextBlock("I can't help with that."),), REFUSAL)
    result = run_agent(ScriptedModel([refusal]), TOOLS, "q")
    assert result.stopped == "refusal"
    assert result.text == "I can't help with that."


def test_agent_raises_on_a_cut_off_answer():
    cut = Response((TextBlock("The answer is"),), MAX_TOKENS)
    with pytest.raises(AgentError):
        run_agent(ScriptedModel([cut]), TOOLS, "q")


def test_agent_step_limit():
    script = [calls(("get_weather", {"city": f"City{n}"})) for n in range(3)]
    model = ScriptedModel(script)
    with pytest.raises(StepLimitError) as caught:
        run_agent(model, TOOLS, "Weather everywhere", max_steps=3)
    assert len(model.requests) == 3
    assert len(caught.value.messages) == 7
    assert isinstance(caught.value, AgentError)


def test_agent_token_budget_is_checked_before_the_next_request():
    script = [calls(("get_weather", {"city": f"C{n}"}), usage=Usage(400, 100)) for n in range(5)]
    model = ScriptedModel(script)
    with pytest.raises(BudgetExceededError) as caught:
        run_agent(model, TOOLS, "go", max_total_tokens=1_000)
    assert len(model.requests) == 2
    assert caught.value.messages[-1].role == "user"


def test_agent_budget_allows_a_final_answer_within_the_step():
    model = ScriptedModel([final("done", usage=Usage(5_000, 100))])
    assert run_agent(model, TOOLS, "go", max_total_tokens=10).text == "done"


def test_agent_detects_repeated_calls():
    same = ("get_weather", {"city": "Porto"})
    model = ScriptedModel([calls(same), calls(same), final("never")])
    ran = []
    tools = {**TOOLS, "get_weather": Tool("get_weather", "", WEATHER_SCHEMA, lambda city: ran.append(city) or "sunny")}
    with pytest.raises(RepeatedCallsError):
        run_agent(model, tools, "Porto?")
    assert ran == ["Porto"]


def test_agent_allows_the_same_call_after_a_different_one():
    a = ("get_weather", {"city": "Porto"})
    b = ("get_weather", {"city": "Faro"})
    model = ScriptedModel([calls(a), calls(b), calls(a), final("ok")])
    assert run_agent(model, TOOLS, "compare").steps == 4
