import pytest

from evals import Case, Report, compare, grade, load_cases, may_ship, run

CASES_JSONL = """
{"id": "capital", "input": "Capital of Portugal?", "expected": "Lisbon", "grader": "exact", "tags": ["en", "facts"]}
{"id": "capital-pt", "input": "Capital de Portugal?", "expected": "Lisboa", "grader": "contains", "tags": ["pt", "facts"]}

{"id": "date", "input": "Due date?", "expected": "\\\\d{4}-\\\\d{2}-\\\\d{2}", "grader": "regex", "tags": ["en"]}
{"id": "json", "input": "Extract", "expected": "name, total", "grader": "json_keys", "tags": ["pt"]}
"""

BASELINE = {
    "capital": "Lisbon",
    "capital-pt": "A capital é Lisboa.",
    "date": "It is due on 2026-03-01.",
    "json": '{"name": "Ana"}',
}
CANDIDATE = {
    "capital": "lisbon ",
    "capital-pt": "É Porto.",
    "date": "Due 2026-03-01",
    "json": '{"name": "Ana", "total": 12.5}',
}


def cases():
    return load_cases(CASES_JSONL)


def test_load_cases_skips_blank_lines_and_keeps_tags():
    loaded = cases()
    assert [c.id for c in loaded] == ["capital", "capital-pt", "date", "json"]
    assert loaded[0] == Case("capital", "Capital of Portugal?", "Lisbon", "exact", ("en", "facts"))
    assert loaded[2].expected == "\\d{4}-\\d{2}-\\d{2}"


@pytest.mark.parametrize(
    ("text", "message"),
    [
        ('{"id": "a", "input": "", "expected": "", "grader": "exact"}\nnot json', "line 2: invalid JSON"),
        ('["a list"]', "line 1: invalid JSON"),
        ('\n{"id": "a", "expected": "", "grader": "exact"}', "line 2: missing field 'input'"),
        ('{"id": "", "input": "", "expected": "", "grader": "exact"}', "line 1: missing field 'id'"),
        ('{"id": "a", "input": "", "expected": 4, "grader": "exact"}', "line 1: missing field 'expected'"),
        ('{"id": "a", "input": "", "expected": "", "grader": "judge"}', "line 1: unknown grader 'judge'"),
        ('{"id": "a", "input": "", "expected": "", "grader": "exact", "tags": "en"}', "line 1: missing field 'tags'"),
        (
            '{"id": "a", "input": "", "expected": "", "grader": "exact"}\n\n{"id": "a", "input": "", "expected": "", "grader": "regex"}',
            "line 3: duplicate id 'a'",
        ),
    ],
)
def test_load_cases_errors(text, message):
    with pytest.raises(ValueError) as error:
        load_cases(text)
    assert str(error.value).startswith(message)


def test_exact_and_contains():
    assert grade(Case("a", "", "New  York", "exact"), " new york ")
    assert not grade(Case("a", "", "York", "exact"), "New York")
    assert grade(Case("a", "", "Lisboa", "contains"), "A capital é  LISBOA.")
    assert not grade(Case("a", "", "Lisboa", "contains"), "Porto")


def test_regex_is_case_insensitive_and_multiline():
    assert grade(Case("a", "", "^answer:.*42", "regex"), "ANSWER: it is\n42")
    assert not grade(Case("a", "", "^42$", "regex"), "420")


def test_json_keys():
    case = Case("a", "", " name ,total ", "json_keys")
    assert grade(case, ' {"name": "x", "total": 1, "extra": 2} ')
    assert not grade(case, '{"name": "x"}')
    assert not grade(case, '["name", "total"]')
    assert not grade(case, "```json\n{\"name\": 1, \"total\": 2}\n```")


def test_run_report():
    report = run(cases(), BASELINE)
    assert report.passed == ["capital", "capital-pt", "date"]
    assert report.failed == ["json"]
    assert report.by_tag == {"en": (2, 2), "facts": (2, 2), "pt": (1, 2)}
    assert list(report.by_tag) == ["en", "facts", "pt"]
    assert report.pass_rate == 0.75


def test_missing_response_fails():
    report = run(cases(), {"capital": "Lisbon"})
    assert report.passed == ["capital"]
    assert report.failed == ["capital-pt", "date", "json"]


def test_compare():
    comparison = compare(run(cases(), BASELINE), run(cases(), CANDIDATE))
    assert comparison.regressions == ["capital-pt"]
    assert comparison.fixes == ["json"]
    assert comparison.delta == 0.0


def test_may_ship_blocks_a_critical_regression_and_a_tag_drop():
    baseline, candidate = run(cases(), BASELINE), run(cases(), CANDIDATE)
    assert may_ship(baseline, candidate, critical=["capital-pt", "date"]) == [
        "critical case 'capital-pt' regressed",
        "tag 'facts' dropped from 100% to 50%",
    ]
    assert may_ship(baseline, candidate, max_tag_drop=0.5) == []


def test_may_ship_reports_an_overall_drop():
    baseline = Report(["a", "b", "c"], [], {})
    candidate = Report(["a"], ["b", "c"], {})
    assert may_ship(baseline, candidate) == ["pass rate dropped by 66.7 points"]


def test_may_ship_an_improvement():
    baseline = Report(["a"], ["b"], {"x": (1, 2)})
    candidate = Report(["a", "b"], [], {"x": (2, 2), "new": (0, 1)})
    assert may_ship(baseline, candidate) == []
