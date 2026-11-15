"""Word statistics over free text."""

import re
from collections import Counter, defaultdict

# Letters or digits in any alphabet ([^\W_] is \w without the underscore), with
# apostrophes allowed only between two of them.
WORD = re.compile(r"[^\W_]+(?:'[^\W_]+)*")


def words(text: str) -> list[str]:
    """Every word of text, in order, case-folded."""
    return WORD.findall(text.casefold())


def top_words(text: str, n: int = 3) -> list[tuple[str, int]]:
    """The n most frequent words as (word, count): most frequent first, ties alphabetical."""
    if n <= 0:
        return []
    counts = Counter(words(text))
    ranked = sorted(counts.items(), key=lambda item: (-item[1], item[0]))
    return ranked[:n]


def first_seen(text: str) -> list[str]:
    """Each different word once, in the order it first appears."""
    return list(dict.fromkeys(words(text)))


def words_by_length(text: str) -> dict[int, list[str]]:
    """Word length -> the different words of that length, sorted; keys in increasing order."""
    groups: defaultdict[int, set[str]] = defaultdict(set)
    for word in words(text):
        groups[len(word)].add(word)
    return {length: sorted(groups[length]) for length in sorted(groups)}
