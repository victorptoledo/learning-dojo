import dataclasses

import pytest

from gradebook import Grade, Gradebook, letter, parse_line

LINES = [
    "# student,course,score",
    "ada, math, 91",
    "ada, physics, 87",
    "alan, math, 78",
    "alan, math, 95",
    "grace, math, abc",
    "",
    "grace, physics, 99",
]


def test_grade_normalises_and_is_frozen():
    grade = Grade("  ada ", " MATH ", 90)
    assert (grade.student, grade.course) == ("ada", "math")
    with pytest.raises(dataclasses.FrozenInstanceError):
        grade.score = 10
    assert grade == Grade("ada", "math", 90)


@pytest.mark.parametrize(
    "student, course, score",
    [(" ", "math", 50), ("ada", "", 50), ("ada", "math", 101), ("ada", "math", -1), ("ada", "math", 9.5), ("ada", "math", True)],
)
def test_grade_rejects_invalid_values(student, course, score):
    with pytest.raises(ValueError):
        Grade(student, course, score)


@pytest.mark.parametrize("line", ["ada,math", "ada,math,90,extra", "ada,math,ninety", "ada,math,"])
def test_parse_line_rejects_bad_lines(line):
    with pytest.raises(ValueError):
        parse_line(line)


def test_letter_boundaries():
    assert [letter(s) for s in (100, 90, 89, 80, 79, 70, 69, 60, 59, 0)] == list("AABBCCDDFF")


def test_later_grade_replaces_the_earlier_one():
    book = Gradebook([Grade("alan", "math", 78), Grade("alan", "Math", 95)])
    assert len(book) == 1
    assert book.average("alan") == 95.0


def test_from_lines_skips_comments_and_blanks_and_records_rejected_lines():
    book = Gradebook.from_lines(LINES)
    assert book.rejected == [6]
    assert len(book) == 4
    assert book.students == ["ada", "alan", "grace"]


def test_from_lines_accepts_any_iterable():
    book = Gradebook.from_lines(line for line in ["x, y, 1", "x, z, 3"])
    assert book.average("x") == 2.0


def test_average():
    book = Gradebook.from_lines(LINES)
    assert book.average("ada") == pytest.approx(89.0)
    with pytest.raises(KeyError):
        book.average("nobody")


def test_course_report_orders_by_score_then_name():
    book = Gradebook([Grade("bo", "art", 80), Grade("al", "art", 80), Grade("cy", "art", 92), Grade("al", "math", 10)])
    report = book.course_report(" ART ")
    assert report == {"cy": 92, "al": 80, "bo": 80}
    assert list(report) == ["cy", "al", "bo"]
    assert book.course_report("history") == {}


def test_ranking():
    book = Gradebook.from_lines(LINES)
    assert book.ranking() == [("grace", 99.0), ("alan", 95.0), ("ada", 89.0)]


def test_ranking_breaks_ties_by_name():
    book = Gradebook([Grade("zoe", "a", 80), Grade("amy", "a", 80)])
    assert book.ranking() == [("amy", 80.0), ("zoe", 80.0)]


def test_honour_roll():
    book = Gradebook([
        Grade("ada", "a", 95), Grade("ada", "b", 85),
        Grade("bo", "a", 100), Grade("bo", "b", 100), Grade("bo", "c", 59),
        Grade("cy", "a", 84),
    ])
    assert book.honour_roll() == ["ada"]
    assert book.honour_roll(min_average=80) == ["ada", "cy"]


def test_empty_book():
    book = Gradebook()
    assert len(book) == 0
    assert book.students == []
    assert book.ranking() == []
    assert book.rejected == []
