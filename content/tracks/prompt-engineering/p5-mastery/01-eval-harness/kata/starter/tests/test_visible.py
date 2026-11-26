from evals import Case, grade, load_cases


def test_load_one_case():
    cases = load_cases('{"id": "c1", "input": "2+2", "expected": "4", "grader": "exact"}')
    assert cases == [Case("c1", "2+2", "4", "exact", ())]


def test_exact_grader_normalises():
    assert grade(Case("c1", "q", "Lisbon", "exact"), "  lisbon ")
