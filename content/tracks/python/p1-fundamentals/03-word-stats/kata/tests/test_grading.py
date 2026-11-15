from words import first_seen, top_words, words, words_by_length

TEXT = "The cat and the hat. THE END, don't stop!"


def test_words_of_the_example():
    assert words(TEXT) == ["the", "cat", "and", "the", "hat", "the", "end", "don't", "stop"]


def test_words_in_any_alphabet_and_digits():
    assert words("Café naïve 2026 日本!") == ["café", "naïve", "2026", "日本"]


def test_apostrophes_only_inside_words_and_underscores_split():
    assert words("'quoted' rock'n'roll it's snake_case --") == [
        "quoted", "rock'n'roll", "it's", "snake", "case",
    ]


def test_casefold_not_just_lower():
    assert words("STRASSE Straße") == ["strasse", "strasse"]


def test_no_words():
    assert words("  ... !!! ") == []
    assert top_words("", 3) == []
    assert first_seen("") == []
    assert words_by_length("") == {}


def test_top_words_of_the_example():
    assert top_words(TEXT, 2) == [("the", 3), ("and", 1)]


def test_top_words_breaks_ties_alphabetically():
    assert top_words("b a c b a d", 3) == [("a", 2), ("b", 2), ("c", 1)]


def test_top_words_default_and_bounds():
    assert top_words("x y z w") == [("w", 1), ("x", 1), ("y", 1)]
    assert top_words("x y", 10) == [("x", 1), ("y", 1)]
    assert top_words("x y", 0) == []
    assert top_words("x y", -1) == []


def test_first_seen_keeps_the_first_occurrence():
    assert first_seen(TEXT) == ["the", "cat", "and", "hat", "end", "don't", "stop"]


def test_words_by_length_of_the_example():
    assert words_by_length(TEXT) == {
        3: ["and", "cat", "end", "hat", "the"],
        4: ["stop"],
        5: ["don't"],
    }


def test_words_by_length_keys_are_in_increasing_order():
    groups = words_by_length("elephant ox bee ox cat antelope")
    assert list(groups) == [2, 3, 8]
    assert groups[8] == ["antelope", "elephant"]
