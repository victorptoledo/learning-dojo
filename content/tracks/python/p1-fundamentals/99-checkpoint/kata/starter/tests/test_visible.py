from gradebook import parse_line


def test_parses_a_line():
    grade = parse_line("Ada Lovelace, Math , 91")
    assert (grade.student, grade.course, grade.score) == ("Ada Lovelace", "math", 91)
