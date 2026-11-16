"""Parse chain-of-thought responses: tags, final answers and self-consistency votes."""

import re
from collections import Counter
from collections.abc import Iterable

ANSWER_LINE = re.compile(r"^\s*answer\s*:\s*(.*)$", re.IGNORECASE)
NUMBER = re.compile(r"-?(?:\d{1,3}(?:,\d{3})+|\d+)(?:\.\d*)?")


def extract_tag(text: str, tag: str) -> str | None:
    """The stripped content of the last complete <tag>...</tag> pair, or None."""
    name = re.escape(tag)
    found = re.findall(rf"<{name}>(.*?)</{name}>", text, re.DOTALL)
    return found[-1].strip() if found else None


def strip_reasoning(text: str) -> str:
    """The text without <thinking> blocks, closed or truncated."""
    text = re.sub(r"<thinking>.*?</thinking>", "", text, flags=re.DOTALL)
    start = text.find("<thinking>")
    if start != -1:
        text = text[:start]
    text = re.sub(r"\n{3,}", "\n\n", text)
    return text.strip()


def final_answer(text: str) -> str:
    """The <answer> content, or the last 'Answer:' line outside the reasoning."""
    tagged = extract_tag(text, "answer")
    if tagged:
        return tagged
    for line in reversed(strip_reasoning(text).splitlines()):
        match = ANSWER_LINE.match(line)
        if match:
            return match.group(1).strip()
    raise ValueError("no final answer in the response")


def normalize(answer: str) -> str:
    """A canonical form of an answer, so that equal answers compare equal."""
    text = " ".join(answer.strip().casefold().split())
    text = text.removesuffix(".")
    if NUMBER.fullmatch(text):
        text = text.replace(",", "")
        if "." in text:
            text = text.rstrip("0").rstrip(".")
    return text


def majority_vote(samples: Iterable[str]) -> tuple[str, float]:
    """The most common normalised final answer and its share of the valid samples."""
    answers = []
    for sample in samples:
        try:
            answers.append(normalize(final_answer(sample)))
        except ValueError:
            continue
    if not answers:
        raise ValueError("no sample has a final answer")
    counts = Counter(answers)
    best = max(counts.values())
    winner = next(answer for answer in answers if counts[answer] == best)
    return winner, best / len(answers)
