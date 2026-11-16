from evals import Case, exact_match


def test_exact_match_ignores_case_and_trailing_punctuation():
    assert exact_match("Billing.", Case("t1", "charged twice", "billing")).value == 1.0


def test_exact_match_fails_on_a_different_answer():
    assert exact_match("shipping", Case("t1", "charged twice", "billing")).value == 0.0
