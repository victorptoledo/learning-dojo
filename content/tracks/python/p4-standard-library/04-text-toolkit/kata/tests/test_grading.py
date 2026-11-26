import time
from datetime import timedelta

import pytest

from textkit import highlight, parse_duration, search_key, slugify

# ---- search_key ---------------------------------------------------------------------


def test_search_key_joins_composed_and_combining_accents():
    assert search_key("José") == search_key("José")


def test_search_key_folds_case_width_and_ligatures():
    assert search_key("ＡＤＭＩＮ") == "admin"
    assert search_key("Straße") == search_key("STRASSE") == "strasse"
    assert search_key("ﬁle") == "file"


def test_search_key_collapses_whitespace_but_keeps_accents():
    assert search_key("  São \t Paulo\n") == "são paulo"


# ---- slugify ------------------------------------------------------------------------


def test_slugify_strips_accents_and_joins_words():
    assert slugify("Ação & Reação — 2ª edição") == "acao-reacao-2a-edicao"
    assert slugify("  Hello,   World!!  ") == "hello-world"


def test_slugify_handles_sharp_s_ligatures_and_full_width():
    assert slugify("Straße") == "strasse"
    assert slugify("ﬁnal ２０２６") == "final-2026"


def test_slugify_handles_combining_accents():
    assert slugify("Café Crème") == "cafe-creme"


def test_slugify_cuts_on_a_word_boundary():
    assert slugify("hello world again", max_length=12) == "hello-world"
    assert slugify("hello world again", max_length=11) == "hello-world"
    assert slugify("hello world again", max_length=14) == "hello-world"
    assert slugify("hello world again", max_length=17) == "hello-world-again"
    assert slugify("abcdefghij", max_length=4) == "abcd"


def test_slugify_refuses_text_without_letters_or_digits():
    for title in ("", "   ", "!!!", "\U0001f600\U0001f600"):
        with pytest.raises(ValueError):
            slugify(title)
    with pytest.raises(ValueError):
        slugify("ok", max_length=0)


# ---- highlight ----------------------------------------------------------------------


def test_highlight_whole_words_only_ignoring_case():
    assert highlight("Cat, cats and CAT.", ["cat"]) == "[Cat], cats and [CAT]."


def test_highlight_treats_terms_literally():
    text = "I write C++ and C#, not C or a.b"
    assert highlight(text, ["c++", "c#", "a.b"]) == "I write [C++] and [C#], not C or [a.b]"
    assert highlight("axb", ["a.b"]) == "axb"


def test_highlight_prefers_the_longest_term():
    assert highlight("New York is new", ["new", "new york"]) == "[New York] is [new]"


def test_highlight_works_on_accented_words_and_custom_marks():
    assert highlight("SÃO JOÃO e são", ["são"], before="<b>", after="</b>") == (
        "<b>SÃO</b> JOÃO e <b>são</b>"
    )
    assert highlight("cafés", ["café"]) == "cafés"


def test_highlight_ignores_empty_terms():
    assert highlight("nothing here", []) == "nothing here"
    assert highlight("nothing here", ["", "  "]) == "nothing here"


def test_highlight_is_fast_on_long_text():
    text = "lorem ipsum dolor sit amet " * 20_000
    terms = [f"word{n}" for n in range(300)] + ["dolor"]
    start = time.perf_counter()
    result = highlight(text, terms)
    assert time.perf_counter() - start < 1.0
    assert result.count("[dolor]") == 20_000


# ---- parse_duration -----------------------------------------------------------------


@pytest.mark.parametrize(
    ("text", "expected"),
    [
        ("45s", timedelta(seconds=45)),
        ("1h 30m", timedelta(hours=1, minutes=30)),
        ("2h15m", timedelta(hours=2, minutes=15)),
        ("1d 2h 3m 4s", timedelta(days=1, hours=2, minutes=3, seconds=4)),
        ("  90 M  ", timedelta(minutes=90)),
        ("1D 1S", timedelta(days=1, seconds=1)),
    ],
)
def test_parse_duration_reads_any_subset_of_units_in_order(text, expected):
    assert parse_duration(text) == expected


@pytest.mark.parametrize(
    "text", ["", "   ", "10", "1h 1h", "30m 1h", "1.5h", "-2m", "1 hour", "1h x", "٣h", "1w"]
)
def test_parse_duration_rejects_anything_else(text):
    with pytest.raises(ValueError):
        parse_duration(text)


def test_parse_duration_fails_fast_on_hostile_input():
    start = time.perf_counter()
    for hostile in ("1" * 50_000 + "x", " " * 50_000 + "x", "1 " * 20_000 + "x"):
        with pytest.raises(ValueError):
            parse_duration(hostile)
    assert time.perf_counter() - start < 1.0
