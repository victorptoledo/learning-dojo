"""A gradebook: validated grades parsed from text, with averages and reports."""

from collections.abc import Iterable
from dataclasses import dataclass


@dataclass(frozen=True, slots=True)
class Grade:
    student: str
    course: str
    score: int

    def __post_init__(self) -> None:
        raise NotImplementedError("todo: validate and normalise the grade")


def parse_line(line: str) -> Grade:
    """Parse `student,course,score` into a Grade."""
    raise NotImplementedError("todo: write parse_line()")


def letter(score: int) -> str:
    """The letter for a score: A from 90, B from 80, C from 70, D from 60, else F."""
    raise NotImplementedError("todo: write letter()")


class Gradebook:
    def __init__(self, grades: Iterable[Grade] = ()) -> None:
        raise NotImplementedError("todo: write Gradebook()")

    def add(self, grade: Grade) -> None:
        raise NotImplementedError("todo: write Gradebook.add()")

    @classmethod
    def from_lines(cls, lines: Iterable[str]) -> "Gradebook":
        raise NotImplementedError("todo: write Gradebook.from_lines()")

    def __len__(self) -> int:
        raise NotImplementedError("todo: write len(book)")

    @property
    def students(self) -> list[str]:
        raise NotImplementedError("todo: write Gradebook.students")

    def average(self, student: str) -> float:
        raise NotImplementedError("todo: write Gradebook.average()")

    def course_report(self, course: str) -> dict[str, int]:
        raise NotImplementedError("todo: write Gradebook.course_report()")

    def ranking(self) -> list[tuple[str, float]]:
        raise NotImplementedError("todo: write Gradebook.ranking()")

    def honour_roll(self, min_average: float = 85.0) -> list[str]:
        raise NotImplementedError("todo: write Gradebook.honour_roll()")
