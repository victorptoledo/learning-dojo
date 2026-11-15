# Gradebook

Write `gradebook.py`: grades parsed from text lines, validated, stored and reported.

## `Grade` and `parse_line`

`Grade(student, course, score)` is a **frozen** dataclass. When it is created, `student` and
`course` are stripped and `course` is turned to lower case; a blank student or course, or a
score that is not an `int` from 0 to 100, raises `ValueError` (a `bool` is not a score).

`parse_line(line) -> Grade` reads one line `student,course,score`, with optional spaces
around each field:

```python
parse_line("Ada Lovelace, Math , 91")   # Grade(student='Ada Lovelace', course='math', score=91)
```

A line with a number of fields other than three, or a score that is not an integer, raises
`ValueError`.

## `Gradebook`

| Member | Behaviour |
| --- | --- |
| `Gradebook(grades=())` | A book with those grades |
| `add(grade)` | Stores a grade. A student has **one** grade per course: a later grade for the same student and course replaces the earlier one |
| `Gradebook.from_lines(lines)` | A class method: a book from an iterable of lines. Blank lines and lines starting with `#` are skipped; lines that do not parse are skipped too, and their line numbers (counting from 1) are kept in `book.rejected`, a list |
| `len(book)` | The number of grades stored |
| `students` | A property: the students' names, sorted |
| `average(student)` | The mean score of that student, a `float`; `KeyError` for an unknown student |
| `course_report(course)` | A `dict` of student to score for that course (the course name is normalised like a grade's), ordered by score descending, then name |
| `ranking()` | A list of `(student, average)` tuples, best average first, ties by name |
| `honour_roll(min_average=85.0)` | The sorted names of students whose average is at least `min_average` and who have no `F` |

`letter(score) -> str` is a module function: `A` from 90, `B` from 80, `C` from 70, `D` from
60, otherwise `F`.

```python
book = Gradebook.from_lines([
    "# student,course,score",
    "ada, math, 91",
    "ada, physics, 87",
    "alan, math, 78",
    "alan, math, 95",       # replaces 78
    "grace, math, abc",     # rejected: line 6
    "",
    "grace, physics, 99",
])
book.rejected            # [6]
book.average("ada")      # 89.0
book.ranking()           # [('grace', 99.0), ('alan', 95.0), ('ada', 89.0)]
book.course_report("MATH")   # {'alan': 95, 'ada': 91}
```

Run the visible tests with `uv run pytest`. There are 22 tests: 1 in
`tests/test_visible.py` and 21 hidden ones, plus ruff on `gradebook.py`.
