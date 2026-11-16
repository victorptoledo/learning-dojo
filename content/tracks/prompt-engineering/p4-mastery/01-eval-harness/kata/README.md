# Eval Harness

A team recorded the responses of two versions of a prompt (the baseline and a candidate) on the
same evaluation set. You write the harness that decides whether the candidate may ship.
`evals.py` defines the dataclasses:

```python
GRADERS = ("exact", "contains", "regex", "json_keys")

@dataclass(frozen=True)
class Case:
    id: str
    input: str
    expected: str
    grader: str
    tags: tuple[str, ...] = ()

@dataclass(frozen=True)
class Report:
    passed: list[str]                       # case ids, in case order
    failed: list[str]
    by_tag: dict[str, tuple[int, int]]      # tag -> (passed, total), keys sorted

    @property
    def pass_rate(self) -> float: ...       # given: passed / total, 0.0 for no cases

@dataclass(frozen=True)
class Comparison:
    regressions: list[str]
    fixes: list[str]
    delta: float
```

| Function | Does |
| --- | --- |
| `load_cases(text) -> list[Case]` | Parses JSON Lines, below |
| `grade(case, response) -> bool` | Applies the case's grader |
| `run(cases, responses) -> Report` | Grades every case; `responses` maps case id to the recorded response, and a missing response fails |
| `compare(baseline, candidate) -> Comparison` | Regressions, fixes and the change in pass rate |
| `may_ship(baseline, candidate, *, critical=(), max_tag_drop=0.0) -> list[str]` | The reasons that block shipping; `[]` means it may ship |

**`load_cases`.** One JSON object per line; blank lines are skipped. Fields: `id` (non-empty
string), `input` and `expected` (strings), `grader` (one of `GRADERS`) and optional `tags` (a
list of strings, stored as a tuple). Raise `ValueError` with a message that starts with
`line N:` (the real line number) for: invalid JSON or not an object (`line N: invalid JSON`),
a missing or wrongly typed field (`line N: missing field 'NAME'`), an unknown grader
(`line N: unknown grader 'G'`) and a repeated id (`line N: duplicate id 'ID'`).

**Graders.** *Normalised* means stripped, `casefold`ed and with whitespace collapsed.

| Grader | Passes when |
| --- | --- |
| `exact` | the normalised response equals the normalised expected value |
| `contains` | the normalised expected value occurs in the normalised response |
| `regex` | `re.search(expected, response, re.IGNORECASE \| re.DOTALL)` finds a match |
| `json_keys` | the stripped response parses as a JSON **object** that has every key in `expected` (a comma-separated list, spaces around keys ignored) |

**`compare`.** `regressions`: ids that passed in the baseline and failed in the candidate, in
the order of `candidate.failed`. `fixes`: ids that failed in the baseline and passed in the
candidate, in the order of `candidate.passed`. `delta`: candidate pass rate minus baseline.

**`may_ship`** returns, in this order:

1. `critical case 'ID' regressed` for each regression whose id is in `critical`;
2. `pass rate dropped by X points` when `delta < 0`, with `X` = `-delta * 100` formatted with
   one decimal (`:.1f`);
3. for each tag in both reports (sorted), when its rate drops by **more** than `max_tag_drop`
   (a fraction): `tag 'T' dropped from B to C`, with the rates formatted as percentages with
   no decimals (`:.0%`).

Run the visible tests with `uv run pytest`. There are 20 tests: 2 in `tests/test_visible.py`
and 18 hidden ones, plus ruff on `evals.py`.
