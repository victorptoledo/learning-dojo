from chain import Step, render_step, run_chain


def test_render_escapes_the_value():
    step = Step("s", "<text>{{input}}</text>", str.strip)
    assert render_step(step, "a < b") == "<text>a &lt; b</text>"


def test_one_step_chain():
    step = Step("upper", "Upper-case: {{input}}", str.strip)
    result = run_chain([step], "hi", lambda messages: " HI ", budget_tokens=100)
    assert result.output == "HI"
