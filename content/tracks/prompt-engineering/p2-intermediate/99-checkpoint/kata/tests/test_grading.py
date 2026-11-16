import pytest

from chain import BudgetExceeded, ChainError, Step, StepTrace, estimate_tokens, render_step, run_chain


class Recorded:
    """A fake model that returns recorded answers in order and keeps every call."""

    def __init__(self, *answers):
        self.answers = list(answers)
        self.calls = []

    def __call__(self, messages):
        self.calls.append(messages)
        return self.answers.pop(0)


def parse_number(text):
    text = text.strip()
    if not text.isdigit():
        raise ValueError(f"expected a whole number, got {text!r}")
    return text


def tokens(*texts):
    return sum((len(t) + 3) // 4 for t in texts)


def test_estimate_tokens():
    assert estimate_tokens("") == 0
    assert estimate_tokens("12345678") == 2
    assert estimate_tokens("123456789") == 3


def test_render_replaces_every_placeholder_and_escapes():
    step = Step("s", "{{input}} | <d>{{input}}</d>", str.strip)
    assert render_step(step, "</d> & x") == "&lt;/d&gt; &amp; x | <d>&lt;/d&gt; &amp; x</d>"
    assert render_step(step, 'say "hi"') == 'say "hi" | <d>say "hi"</d>'


def test_render_without_placeholder_raises():
    with pytest.raises(ValueError):
        render_step(Step("s", "no placeholder", str.strip), "x")


def test_outputs_flow_from_step_to_step_and_are_escaped():
    steps = [
        Step("extract", "Extract from <doc>{{input}}</doc>", str.strip),
        Step("count", "Count the items in <items>{{input}}</items>", parse_number),
    ]
    model = Recorded("  a<b, c  ", "2")
    result = run_chain(steps, "doc text", model, budget_tokens=1000)
    assert result.output == "2"
    assert model.calls[0] == [{"role": "user", "content": "Extract from <doc>doc text</doc>"}]
    assert model.calls[1] == [{"role": "user", "content": "Count the items in <items>a&lt;b, c</items>"}]
    assert result.trace == [StepTrace("extract", 1, "a<b, c"), StepTrace("count", 1, "2")]


def test_tokens_used_counts_prompts_and_answers():
    step = Step("count", "How many? {{input}}", parse_number)
    result = run_chain([step], "abc", Recorded("3"), budget_tokens=1000)
    assert result.tokens_used == tokens("How many? abc", "3")


def test_retry_sends_the_bad_answer_and_the_problem():
    step = Step("count", "Count: {{input}}", parse_number)
    model = Recorded("three", "3")
    result = run_chain([step], "a b c", model, budget_tokens=1000)
    assert result.output == "3"
    assert result.trace == [StepTrace("count", 2, "3")]
    retry = "Your answer could not be used: expected a whole number, got 'three'\nAnswer again."
    assert model.calls[1] == [
        {"role": "user", "content": "Count: a b c"},
        {"role": "assistant", "content": "three"},
        {"role": "user", "content": retry},
    ]
    assert result.tokens_used == tokens("Count: a b c", "three") + tokens("Count: a b c", "three", retry, "3")


def test_the_model_gets_a_copy_of_the_messages():
    step = Step("count", "Count: {{input}}", parse_number)
    model = Recorded("x", "1")
    run_chain([step], "a", model, budget_tokens=1000)
    assert len(model.calls[0]) == 1


def test_chain_error_after_max_attempts():
    steps = [Step("count", "Count: {{input}}", parse_number, max_attempts=3)]
    model = Recorded("one", "uno", "eins", "1")
    with pytest.raises(ChainError) as error:
        run_chain(steps, "a", model, budget_tokens=10_000)
    assert error.value.step == "count"
    assert error.value.problem == "expected a whole number, got 'eins'"
    assert str(error.value) == "step 'count': expected a whole number, got 'eins'"
    assert len(model.calls) == 3
    assert not isinstance(error.value, BudgetExceeded)


def test_budget_is_checked_before_each_call():
    steps = [Step("a", "{{input}}", str.strip), Step("b", "{{input}}", str.strip)]
    model = Recorded("x" * 40, "never used")
    budget = tokens("y" * 40, "x" * 40) + 5
    with pytest.raises(BudgetExceeded) as error:
        run_chain(steps, "y" * 40, model, budget_tokens=budget)
    assert error.value.step == "b"
    assert error.value.problem == f"budget of {budget} tokens exceeded"
    assert len(model.calls) == 1


def test_budget_exactly_reached_is_allowed():
    step = Step("a", "{{input}}", str.strip)
    budget = tokens("abcd", "efgh")
    assert run_chain([step], "abcd", Recorded("efgh"), budget_tokens=budget).tokens_used == budget


def test_a_retry_can_exceed_the_budget():
    step = Step("count", "Count: {{input}}", parse_number)
    model = Recorded("not a number at all", "1")
    budget = tokens("Count: a", "not a number at all") + 3
    with pytest.raises(BudgetExceeded):
        run_chain([step], "a", model, budget_tokens=budget)
    assert len(model.calls) == 1


def test_no_steps():
    with pytest.raises(ValueError):
        run_chain([], "x", Recorded(), budget_tokens=10)
