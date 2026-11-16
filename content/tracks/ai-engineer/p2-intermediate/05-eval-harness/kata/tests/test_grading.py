import json

import pytest

from evals import Case, CaseResult, Report, Score, compare, contains_all, exact_match, judge, run_suite
from llm import ScriptedModel

CASES = [
    Case("t1", "I was charged twice", "billing", ("billing", "critical")),
    Case("t2", "Where is my parcel?", "shipping", ("shipping",)),
    Case("t3", "Card declined at checkout", "billing", ("billing",)),
    Case("t4", "App crashes on login", "technical", ("technical", "critical")),
]


def classifier(answers):
    return lambda text: answers[text]


# ---- scorers ----------------------------------------------------------------------------------


def test_exact_match_normalises_whitespace_and_case():
    case = Case("x", "", "Out of  stock")
    assert exact_match("  out of stock!  ", case).value == 1.0
    assert exact_match("OUT\nOF STOCK?", case).value == 1.0
    assert exact_match("out of stock soon", case).value == 0.0


def test_contains_all_fraction_and_reason():
    scorer = contains_all(["refund", "30 days", "receipt"])
    full = scorer("You get a REFUND within 30 days with a receipt.", CASES[0])
    assert full == Score(1.0, "")
    partial = scorer("A refund is possible.", CASES[0])
    assert partial.value == pytest.approx(1 / 3)
    assert partial.reason == "missing: 30 days, receipt"
    assert contains_all([])("anything", CASES[0]).value == 1.0


def test_judge_scales_the_score_and_keeps_the_reason():
    model = ScriptedModel([json.dumps({"score": 4, "reason": "mostly faithful"})])
    scorer = judge(model, "Rate faithfulness from 1 to 5.")
    assert scorer("The parcel arrives Monday.", CASES[1]) == Score(0.75, "mostly faithful")
    request = model.requests[0]
    assert request.system == "Rate faithfulness from 1 to 5."
    prompt = request.messages[0].content
    lines = prompt.splitlines()
    assert "Input: Where is my parcel?" in lines
    assert "Expected: shipping" in lines
    assert "Output: The parcel arrives Monday." in lines


@pytest.mark.parametrize("reply", ["Great answer!", '{"score": 6, "reason": "x"}', '{"score": "5"}', '{"score": true}', '{"reason": "no score"}'])
def test_judge_rejects_invalid_replies(reply):
    scorer = judge(ScriptedModel([reply]), "rubric")
    assert scorer("out", CASES[0]) == Score(0.0, "invalid judge reply")


def test_judge_extremes():
    scorer = judge(ScriptedModel(['{"score": 1, "reason": "wrong"}', '{"score": 5, "reason": "right"}']), "r")
    assert scorer("a", CASES[0]).value == 0.0
    assert scorer("b", CASES[0]).value == 1.0


# ---- run_suite ----------------------------------------------------------------------------------


def good_answers():
    return {"I was charged twice": "billing", "Where is my parcel?": "shipping", "Card declined at checkout": "Billing", "App crashes on login": "technical"}


def test_run_suite_all_pass():
    report = run_suite(classifier(good_answers()), CASES, {"exact": exact_match})
    assert report.pass_rate() == 1.0
    assert [r.case_id for r in report.results] == ["t1", "t2", "t3", "t4"]
    assert report.results[0].output == "billing"
    assert report.results[0].scores == {"exact": Score(1.0, "")}
    assert report.results[0].tags == ("billing", "critical")
    assert report.failures() == []


def test_run_suite_thresholds():
    keywords = contains_all(["parcel", "monday"])
    system = classifier({c.input: "your parcel" for c in CASES})
    strict = run_suite(system, CASES, {"kw": keywords})
    assert strict.pass_rate() == 0.0
    lenient = run_suite(system, CASES, {"kw": keywords}, thresholds={"kw": 0.5})
    assert lenient.pass_rate() == 1.0
    assert lenient.mean("kw") == pytest.approx(0.5)


def test_run_suite_survives_a_crashing_system():
    def flaky(text):
        if "crashes" in text:
            raise TimeoutError("model took too long")
        return good_answers()[text]

    report = run_suite(flaky, CASES, {"exact": exact_match, "kw": contains_all([])})
    crashed = report.results[3]
    assert crashed.output is None
    assert crashed.passed is False
    assert crashed.scores == {
        "exact": Score(0.0, "error: TimeoutError: model took too long"),
        "kw": Score(0.0, "error: TimeoutError: model took too long"),
    }
    assert report.pass_rate() == 0.75
    assert report.failures() == ["t4"]


def test_report_per_tag():
    answers = {**good_answers(), "Card declined at checkout": "technical"}
    report = run_suite(classifier(answers), CASES, {"exact": exact_match})
    assert report.pass_rate("billing") == 0.5
    assert report.pass_rate("critical") == 1.0
    assert report.pass_rate("missing-tag") == 0.0
    assert report.by_tag() == {"billing": 0.5, "critical": 1.0, "shipping": 1.0, "technical": 1.0}
    assert list(report.by_tag()) == ["billing", "critical", "shipping", "technical"]


def test_empty_report():
    report = Report([])
    assert report.pass_rate() == 0.0
    assert report.mean("exact") == 0.0
    assert report.by_tag() == {}


def test_mean_of_a_scorer():
    results = [
        CaseResult("a", "x", {"j": Score(0.5)}, False),
        CaseResult("b", "y", {"j": Score(1.0)}, True),
    ]
    assert Report(results).mean("j") == 0.75


# ---- compare --------------------------------------------------------------------------------------


def report_for(answers):
    return run_suite(classifier(answers), CASES, {"exact": exact_match})


def test_compare_identical_reports():
    baseline = report_for(good_answers())
    comparison = compare(baseline, report_for(good_answers()))
    assert comparison.regressions == []
    assert comparison.fixes == []
    assert comparison.delta == 0.0
    assert comparison.ok is True
    assert comparison.reasons == []


def test_compare_finds_regressions_and_fixes():
    baseline = report_for({**good_answers(), "Where is my parcel?": "billing"})
    candidate = report_for({**good_answers(), "Card declined at checkout": "shipping"})
    comparison = compare(baseline, candidate)
    assert comparison.regressions == ["t3"]
    assert comparison.fixes == ["t2"]
    assert comparison.delta == 0.0
    assert comparison.ok is True


def test_compare_blocks_a_drop_beyond_tolerance():
    baseline = report_for(good_answers())
    candidate = report_for({**good_answers(), "Where is my parcel?": "billing"})
    assert compare(baseline, candidate, tolerance=0.3).ok is True
    blocked = compare(baseline, candidate, tolerance=0.1)
    assert blocked.ok is False
    assert blocked.reasons == ["pass rate dropped by 0.250"]


def test_compare_blocks_critical_regressions_even_when_the_score_improves():
    baseline = report_for({**good_answers(), "Where is my parcel?": "x", "Card declined at checkout": "x"})
    candidate = report_for({**good_answers(), "App crashes on login": "billing"})
    comparison = compare(baseline, candidate, tolerance=0.0, critical_tags=["critical"])
    assert comparison.delta == pytest.approx(0.25)
    assert comparison.regressions == ["t4"]
    assert comparison.ok is False
    assert comparison.reasons == ["critical regressions: t4"]


def test_compare_only_uses_common_cases():
    baseline = report_for(good_answers())
    smaller = run_suite(classifier(good_answers()), CASES[:2], {"exact": exact_match})
    comparison = compare(baseline, smaller)
    assert comparison.regressions == []
    assert comparison.fixes == []
