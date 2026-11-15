"""A gradebook: validated grades parsed from text, with averages and reports."""

from collections import defaultdict
from collections.abc import Iterable
from dataclasses import dataclass
from statistics import fmean


@dataclass(frozen=True, slots=True)
class Grade:
    student: str
    course: str
    score: int

    def __post_init__(self) -> None:
        student = self.student.strip()
        course = self.course.strip().lower()
        if not student or not course:
            raise ValueError("a grade needs a student and a course")
        if type(self.score) is not int or not 0 <= self.score <= 100:
            raise ValueError(f"invalid score: {self.score!r}")
        object.__setattr__(self, "student", student)
        object.__setattr__(self, "course", course)


def parse_line(line: str) -> Grade:
    """Parse `student,course,score` into a Grade."""
    parts = [part.strip() for part in line.split(",")]
    if len(parts) != 3:
        raise ValueError(f"expected 3 fields: {line!r}")
    student, course, score = parts
    try:
        value = int(score)
    except ValueError as error:
        raise ValueError(f"score is not an integer: {line!r}") from error
    return Grade(student, course, value)


def letter(score: int) -> str:
    """The letter for a score: A from 90, B from 80, C from 70, D from 60, else F."""
    for minimum, grade in ((90, "A"), (80, "B"), (70, "C"), (60, "D")):
        if score >= minimum:
            return grade
    return "F"


class Gradebook:
    def __init__(self, grades: Iterable[Grade] = ()) -> None:
        self._grades: dict[tuple[str, str], Grade] = {}
        self.rejected: list[int] = []
        for grade in grades:
            self.add(grade)

    def add(self, grade: Grade) -> None:
        self._grades[(grade.student, grade.course)] = grade

    @classmethod
    def from_lines(cls, lines: Iterable[str]) -> "Gradebook":
        book = cls()
        for number, line in enumerate(lines, start=1):
            text = line.strip()
            if not text or text.startswith("#"):
                continue
            try:
                book.add(parse_line(text))
            except ValueError:
                book.rejected.append(number)
        return book

    def __len__(self) -> int:
        return len(self._grades)

    @property
    def students(self) -> list[str]:
        return sorted({grade.student for grade in self._grades.values()})

    def average(self, student: str) -> float:
        scores = self._scores_by_student().get(student)
        if not scores:
            raise KeyError(student)
        return fmean(scores)

    def course_report(self, course: str) -> dict[str, int]:
        wanted = course.strip().lower()
        grades = [grade for grade in self._grades.values() if grade.course == wanted]
        grades.sort(key=lambda grade: (-grade.score, grade.student))
        return {grade.student: grade.score for grade in grades}

    def ranking(self) -> list[tuple[str, float]]:
        averages = [(student, fmean(scores)) for student, scores in self._scores_by_student().items()]
        return sorted(averages, key=lambda pair: (-pair[1], pair[0]))

    def honour_roll(self, min_average: float = 85.0) -> list[str]:
        return sorted(
            student
            for student, scores in self._scores_by_student().items()
            if fmean(scores) >= min_average and all(letter(score) != "F" for score in scores)
        )

    def _scores_by_student(self) -> dict[str, list[int]]:
        scores: defaultdict[str, list[int]] = defaultdict(list)
        for grade in self._grades.values():
            scores[grade.student].append(grade.score)
        return scores
