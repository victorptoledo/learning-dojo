# Checkpoint 2: Prompt Chain

Write a small prompt-chain runner in `chain.py`. The dataclasses and exceptions are given:

```python
@dataclass(frozen=True)
class Step:
    name: str
    template: str                     # uses {{input}} for the previous output
    parse: Callable[[str], str]       # raises ValueError when the answer is unusable
    max_attempts: int = 2

@dataclass(frozen=True)
class StepTrace:
    name: str
    attempts: int
    output: str

@dataclass(frozen=True)
class ChainResult:
    output: str
    trace: list[StepTrace]
    tokens_used: int

class ChainError(Exception): ...       # .step and .problem; str is "step 'NAME': PROBLEM"
class BudgetExceeded(ChainError): ...
```

| Function | Does |
| --- | --- |
| `estimate_tokens(text) -> int` | `ceil(len(text) / 4)` |
| `render_step(step, value) -> str` | The template with every `{{input}}` replaced by `html.escape(value, quote=False)`. A template without `{{input}}` raises `ValueError`. |
| `run_chain(steps, input_text, model, *, budget_tokens) -> ChainResult` | Runs the steps in order, as below |

`model` is a function that takes a list of messages (`{"role": ..., "content": ...}`) and
returns the answer text. The tests pass recorded answers.

**`run_chain`.** No steps: `ValueError`. The first step's value is `input_text`; each step's
value is the parsed output of the step before. For each step:

1. Start the step's messages with one user turn: `render_step(step, value)`.
2. Before **every** call, its cost is the sum of `estimate_tokens(content)` over **all** the
   messages being sent (the API is stateless). If `tokens_used + cost > budget_tokens`, raise
   `BudgetExceeded(step.name, "budget of N tokens exceeded")` without calling. Otherwise add
   the cost, call `model` with a **copy** of the list, and add `estimate_tokens(answer)`.
3. Pass the answer to `step.parse`. If it returns, that is the step's output. If it raises
   `ValueError`, append `{"role": "assistant", "content": answer}` and
   `{"role": "user", "content": "Your answer could not be used: PROBLEM\nAnswer again."}`
   (PROBLEM is `str(error)`) and try again, up to `step.max_attempts` calls in all. When the
   attempts run out, raise `ChainError(step.name, PROBLEM)` with the last problem.
4. Record `StepTrace(step.name, attempts used, output)`.

Return `ChainResult(last output, trace, tokens_used)`.

Run the visible tests with `uv run pytest`. There are 14 tests: 2 in `tests/test_visible.py`
and 12 hidden ones, plus ruff on `chain.py`.
