import math

import pytest

from chunking import Chunk, chunk_document, split_paragraphs
from vector_index import Hit, VectorIndex, cosine, normalize, rrf

# ---- chunking -------------------------------------------------------------------------------


def test_split_handles_blank_lines_with_spaces_and_extra_whitespace():
    text = "  First   line\n  continues here.\n   \n\n\nSecond\tparagraph.\n\n"
    assert split_paragraphs(text) == ["First line continues here.", "Second paragraph."]


def test_split_empty_text():
    assert split_paragraphs("") == []
    assert split_paragraphs("\n \n\n") == []


def test_chunk_packs_paragraphs_up_to_the_limit():
    text = "a b\n\nc d\n\ne f g\n\nh"
    assert chunk_document("d", text, max_words=4) == [
        Chunk("d", 0, "a b\n\nc d"),
        Chunk("d", 1, "e f g\n\nh"),
    ]


def test_chunk_example_from_the_statement():
    text = "a b c\n\nd e\n\nf g h i j k l"
    assert chunk_document("doc", text, max_words=5, overlap=2) == [
        Chunk("doc", 0, "a b c\n\nd e"),
        Chunk("doc", 1, "f g h i j"),
        Chunk("doc", 2, "i j k l"),
    ]


def test_chunk_long_paragraph_windows_without_overlap():
    words = " ".join(str(n) for n in range(10))
    chunks = chunk_document("d", words, max_words=4)
    assert [c.text for c in chunks] == ["0 1 2 3", "4 5 6 7", "8 9"]


def test_chunk_last_window_does_not_only_repeat():
    words = " ".join(str(n) for n in range(6))
    chunks = chunk_document("d", words, max_words=4, overlap=2)
    assert [c.text for c in chunks] == ["0 1 2 3", "2 3 4 5"]


def test_chunk_long_paragraph_is_not_packed_with_neighbours():
    text = "x\n\n1 2 3 4 5 6\n\ny"
    chunks = chunk_document("d", text, max_words=4, overlap=1)
    assert [c.text for c in chunks] == ["x", "1 2 3 4", "4 5 6", "y"]
    assert [c.index for c in chunks] == [0, 1, 2, 3]


def test_chunk_paragraph_of_exactly_max_words_is_packed_normally():
    assert [c.text for c in chunk_document("d", "a b c\n\nd", max_words=3)] == ["a b c", "d"]


def test_chunk_empty_document():
    assert chunk_document("d", "   ", max_words=3) == []


@pytest.mark.parametrize(("max_words", "overlap"), [(0, 0), (3, -1), (3, 3), (3, 5)])
def test_chunk_rejects_bad_sizes(max_words, overlap):
    with pytest.raises(ValueError):
        chunk_document("d", "a b c", max_words=max_words, overlap=overlap)


# ---- vectors --------------------------------------------------------------------------------


def test_normalize():
    assert normalize([3, 4]) == pytest.approx((0.6, 0.8))
    assert isinstance(normalize([1, 0]), tuple)
    with pytest.raises(ValueError):
        normalize([0, 0])


def test_cosine():
    assert cosine([1, 0], [1, 0]) == pytest.approx(1.0)
    assert cosine([1, 0], [0, 2]) == pytest.approx(0.0)
    assert cosine([1, 1], [-1, -1]) == pytest.approx(-1.0)
    assert cosine([1, 2, 3], [2, 4, 6]) == pytest.approx(1.0)
    assert cosine([1, 0], [1, 1]) == pytest.approx(1 / math.sqrt(2))
    with pytest.raises(ValueError):
        cosine([1, 0], [1, 0, 0])
    with pytest.raises(ValueError):
        cosine([0, 0], [1, 0])


def build():
    index = VectorIndex(3)
    index.add("refund", [0.9, 0.1, 0.0], {"lang": "en", "topic": "billing"})
    index.add("reembolso", [0.8, 0.2, 0.0], {"lang": "pt", "topic": "billing"})
    index.add("wifi", [0.0, 0.1, 0.9], {"lang": "en", "topic": "tech"})
    index.add("router", [0.1, 0.0, 0.8], {"lang": "en", "topic": "tech"})
    return index


def test_search_ranks_by_cosine_and_returns_scores():
    hits = build().search([1, 0, 0], k=2)
    assert [h.id for h in hits] == ["refund", "reembolso"]
    assert hits[0].score == pytest.approx(cosine([1, 0, 0], [0.9, 0.1, 0.0]))
    assert hits[0].metadata == {"lang": "en", "topic": "billing"}


def test_search_scores_do_not_depend_on_vector_length():
    index = VectorIndex(2)
    index.add("long", [10, 10])
    index.add("short", [0.1, 0])
    assert [h.id for h in index.search([1, 0.9])] == ["long", "short"]


def test_search_filters_by_metadata():
    hits = build().search([1, 0, 0], where={"lang": "en"})
    assert [h.id for h in hits] == ["refund", "router", "wifi"]
    assert build().search([1, 0, 0], where={"lang": "en", "topic": "tech"})[0].id == "router"
    assert build().search([1, 0, 0], where={"lang": "fr"}) == []


def test_search_min_score_and_k():
    hits = build().search([1, 0, 0], k=10, min_score=0.5)
    assert [h.id for h in hits] == ["refund", "reembolso"]
    assert len(build().search([1, 0, 0], k=10)) == 4


def test_search_breaks_ties_by_id():
    index = VectorIndex(2)
    for item_id in ["b", "c", "a"]:
        index.add(item_id, [1, 1])
    assert [h.id for h in index.search([1, 1])] == ["a", "b", "c"]


def test_add_replaces_and_remove_deletes():
    index = build()
    index.add("wifi", [1, 0, 0], {"lang": "en"})
    assert len(index) == 4
    assert index.search([1, 0, 0], k=1) == [Hit("wifi", pytest.approx(1.0), {"lang": "en"})]
    index.remove("wifi")
    assert "wifi" not in index
    assert "refund" in index
    assert len(index) == 3
    with pytest.raises(KeyError):
        index.remove("wifi")


def test_metadata_defaults_to_an_empty_dict():
    index = VectorIndex(2)
    index.add("a", [1, 0])
    assert index.search([1, 0])[0].metadata == {}


def test_index_checks_dimensions_and_zero_vectors():
    index = VectorIndex(3)
    with pytest.raises(ValueError):
        index.add("bad", [1, 0])
    with pytest.raises(ValueError):
        index.add("zero", [0, 0, 0])
    index.add("ok", [1, 0, 0])
    with pytest.raises(ValueError):
        index.search([1, 0])
    with pytest.raises(ValueError):
        index.search([0, 0, 0])
    with pytest.raises(ValueError):
        index.search([1, 0, 0], k=0)


def test_rrf_fuses_rankings():
    keyword = ["e4012", "router", "wifi"]
    vector = ["wifi", "router", "modem"]
    fused = rrf([keyword, vector])
    assert [doc for doc, _ in fused] == ["wifi", "router", "e4012", "modem"]
    scores = dict(fused)
    assert scores["router"] == pytest.approx(1 / 62 + 1 / 62)
    assert scores["wifi"] == pytest.approx(1 / 63 + 1 / 61)
    assert scores["modem"] == pytest.approx(1 / 63)


def test_rrf_custom_k_and_ties():
    fused = rrf([["b", "a"], ["a", "b"]], k=1)
    assert fused == [("a", pytest.approx(1 / 2 + 1 / 3)), ("b", pytest.approx(1 / 2 + 1 / 3))]
    assert rrf([]) == []
