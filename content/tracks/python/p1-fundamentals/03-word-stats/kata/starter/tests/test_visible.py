from words import words


def test_splits_and_folds_case():
    assert words("The cat and THE hat.") == ["the", "cat", "and", "the", "hat"]
