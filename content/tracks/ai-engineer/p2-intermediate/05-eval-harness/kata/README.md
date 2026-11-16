# Eval Harness

Write `evals.py`. `llm.py` (given, do not edit) has the model types and `ScriptedModel`.
`Case(id, input, expected, tags)`, `Score(value, reason)`, `CaseResult` and `Comparison` are
given dataclasses. A **scorer** is a function `(output: str, case: Case) -> Score` with a value
from 0.0 to 1.0.

## Scorers

- `exact_match(output, case)`: 1.0 when the output equals `case.expected` after normalising both
  (case-folded, whitespace collapsed to single spaces and stripped, trailing `.`, `!` and `?`
  removed), else 0.0.
- `contains_all(keywords)` returns a scorer whose value is the fraction of `keywords` found in the
  output (case-insensitive); the reason lists the missing ones, `"missing: a, b"` (in the given
  order), or is `""`. No keywords: 1.0.
- `judge(model, rubric)` returns a scorer that asks a judge model. It sends one
  `Request(messages=(Message("user", prompt),), system=rubric)` where `prompt` contains the
  case's input, the expected answer and the output, each on its own line as
  `Input: ...`, `Expected: ...`, `Output: ...`. The judge answers JSON
  `{"score": 1-5, "reason": "..."}`: the value is `(score - 1) / 4` and the reason is the judge's.
  A reply that is not JSON, or whose `score` is not an integer from 1 to 5, gives `Score(0.0,
  "invalid judge reply")`.

## `run_suite(system, cases, scorers, thresholds=None) -> Report`

`system` is the function under test (`input -> output`), `scorers` a dict of name to scorer,
`thresholds` a dict of name to the minimum value that passes (default 1.0 for every scorer).
For each case, run the system, apply every scorer and build `CaseResult(case_id, output, scores,
passed, tags)`: `passed` when every score reaches its threshold. If the system raises, the output
is `None`, every score is `Score(0.0, "error: <ExceptionType>: <message>")` and the case fails;
the run goes on.

`Report(results)` has:

- `pass_rate(tag=None)`: the fraction of passed results (only those with `tag` when given); 0.0
  when there are none;
- `mean(scorer)`: the mean value of that scorer over all results (0.0 when empty);
- `by_tag()`: a dict of every tag to its pass rate, sorted by tag;
- `failures()`: the ids of the failed cases, in order.

## `compare(baseline, candidate, tolerance=0.0, critical_tags=()) -> Comparison`

Over the case ids present in **both** reports (in the baseline's order):

- `regressions`: passed in the baseline, failed in the candidate;
- `fixes`: failed in the baseline, passed in the candidate;
- `delta`: candidate pass rate minus baseline pass rate (whole reports);
- `ok` is `False` with a reason in `reasons` when the pass rate dropped by more than `tolerance`
  (`"pass rate dropped by 0.250"`, three decimals) or when a regressed case has one of the
  `critical_tags` (`"critical regressions: t-3, t-7"`); otherwise `True` with no reasons.

Run the visible tests with `uv run pytest`. There are 22 tests: 2 in `tests/test_visible.py`
and 20 hidden ones, plus ruff on each module.
