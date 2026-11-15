"""Word statistics over free text."""


def words(text: str) -> list[str]:
    """Every word of text, in order, case-folded."""
    raise NotImplementedError("todo: write words()")


def top_words(text: str, n: int = 3) -> list[tuple[str, int]]:
    """The n most frequent words as (word, count): most frequent first, ties alphabetical."""
    raise NotImplementedError("todo: write top_words()")


def first_seen(text: str) -> list[str]:
    """Each different word once, in the order it first appears."""
    raise NotImplementedError("todo: write first_seen()")


def words_by_length(text: str) -> dict[int, list[str]]:
    """Word length -> the different words of that length, sorted; keys in increasing order."""
    raise NotImplementedError("todo: write words_by_length()")
