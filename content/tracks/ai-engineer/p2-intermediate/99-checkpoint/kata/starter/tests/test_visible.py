from rag import citations


def test_finds_citations():
    assert citations("Yes [2]. See [1, 2] and [3].") == [2, 1, 3]


def test_no_citations():
    assert citations("No sources here.") == []
