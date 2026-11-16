from chunking import split_paragraphs
from vector_index import VectorIndex


def test_splits_paragraphs():
    assert split_paragraphs("one\ntwo\n\nthree") == ["one two", "three"]


def test_finds_the_nearest_vector():
    index = VectorIndex(2)
    index.add("east", [1, 0])
    index.add("north", [0, 1])
    assert [hit.id for hit in index.search([0.9, 0.1], k=1)] == ["east"]
