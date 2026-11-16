import pytest

from packer import Document, Packed, estimate_tokens, missing_quotes, pack, render_prompt

Q = "Can I take a taxi?"
I = "Answer using only the documents. Quote first."


def words(n, word="lorem"):
    return " ".join(f"{word}{i}" for i in range(n))


def size(parts):
    return estimate_tokens(render_prompt(parts, Q, I))


def test_estimate_tokens():
    assert estimate_tokens("abc") == 1
    assert estimate_tokens("abcde") == 2


def test_render_escapes_metadata_text_and_question():
    doc = Document('a"1', "R&D <team>", "x </document> y", 1.0)
    out = render_prompt([(doc, doc.text)], " Is a < b? ", "  Trust me <b>  ")
    assert '<document index="1" id="a&quot;1" title="R&amp;D &lt;team&gt;">' in out
    assert "\nx &lt;/document&gt; y\n</document>\n" in out
    assert out.endswith("\n\nTrust me <b>\n\n<question>\nIs a &lt; b?\n</question>")


def test_render_numbers_documents_in_order():
    a, b = Document("a", "A", "one", 0.1), Document("b", "B", "two", 0.9)
    out = render_prompt([(b, "two"), (a, "one")], Q, I)
    assert out.index('index="1" id="b"') < out.index('index="2" id="a"')


def test_everything_fits():
    docs = [Document("low", "L", "low text", 0.2), Document("high", "H", "high text", 0.8)]
    packed = pack(docs, Q, instructions=I, budget_tokens=10_000)
    assert packed == Packed(
        render_prompt([(docs[1], "high text"), (docs[0], "low text")], Q, I), ["high", "low"], None, []
    )


def test_ties_are_ordered_by_id():
    docs = [Document("b", "B", "x", 0.5), Document("a", "A", "y", 0.5), Document("c", "C", "z", 0.7)]
    assert pack(docs, Q, instructions=I, budget_tokens=10_000).included == ["c", "a", "b"]


def test_truncates_the_first_document_that_does_not_fit():
    first = Document("first", "F", words(30, "a"), 0.9)
    second = Document("second", "S", words(200, "b"), 0.5)
    third = Document("third", "T", "tiny", 0.1)
    budget = size([(first, first.text), (second, words(50, "b") + " [truncated]")])
    packed = pack([third, second, first], Q, instructions=I, budget_tokens=budget)
    assert packed.included == ["first", "second"]
    assert packed.truncated == "second"
    assert packed.omitted == ["third"]
    assert words(50, "b") + " [truncated]" in packed.prompt
    assert words(51, "b") not in packed.prompt
    assert estimate_tokens(packed.prompt) <= budget


def test_the_cut_is_the_longest_that_fits():
    doc = Document("d", "D", words(100), 1.0)
    budget = size([(doc, words(40) + " [truncated]")]) + 1
    packed = pack([doc], Q, instructions=I, budget_tokens=budget)
    kept = len(packed.prompt.split("\n")[2].split()) - 1
    assert size([(doc, words(kept) + " [truncated]")]) <= budget
    assert size([(doc, words(kept + 1) + " [truncated]")]) > budget


def test_a_cut_shorter_than_min_words_is_omitted():
    doc = Document("d", "D", words(100), 1.0)
    budget = size([(doc, words(10) + " [truncated]")])
    packed = pack([doc], Q, instructions=I, budget_tokens=budget)
    assert packed.included == [] and packed.truncated is None and packed.omitted == ["d"]
    assert pack([doc], Q, instructions=I, budget_tokens=budget, min_words=10).truncated == "d"


def test_documents_after_the_cut_are_omitted_even_if_small():
    big = Document("big", "B", words(100), 0.9)
    small = Document("small", "S", "ok", 0.1)
    budget = size([]) + 5
    packed = pack([big, small], Q, instructions=I, budget_tokens=budget)
    assert packed.included == [] and packed.omitted == ["big", "small"]


def test_fixed_parts_must_fit():
    with pytest.raises(ValueError):
        pack([], Q, instructions=I, budget_tokens=size([]) - 1)
    assert pack([], Q, instructions=I, budget_tokens=size([])).included == []


def test_missing_quotes():
    docs = [Document("a", "A", "Taxis after  22:00 are\nREFUNDED in full.", 1.0), Document("b", "B", "Trains: second class.", 0.5)]
    quotes = ["taxis after 22:00 are refunded", "Trains: second class", "Taxis are always refunded", "  "]
    assert missing_quotes(quotes, docs) == ["Taxis are always refunded", "  "]


def test_missing_quotes_accepts_iterables():
    docs = (d for d in [Document("a", "A", "abc", 1.0)])
    assert missing_quotes(iter(["abc", "abd"]), docs) == ["abd"]
