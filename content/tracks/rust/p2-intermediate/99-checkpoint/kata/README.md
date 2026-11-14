# Checkpoint 2: Grade Book

Read a grade book from text, report problems precisely, and rank the students.

## Input format

One student per line: a name, a colon, and comma-separated scores.

```text
# class 3B
Ana: 90, 85, 100
Bruno: 70, 65

Carla: 88
```

- Blank lines and lines starting with `#` are skipped.
- Names and scores are trimmed. A score is a whole number from 0 to 100.
- Line numbers start at 1 and count every line, skipped ones included.

## What to implement in `src/lib.rs`

1. `parse_grade_book(text) -> Result<Vec<Student>, GradeError>`, keeping the input order.
   The **first** problem found stops parsing:
   - `GradeError::MissingColon { line }`: the line has no `:`.
   - `GradeError::MissingName { line }`: the name is empty.
   - `GradeError::NoScores { line }`: nothing after the colon.
   - `GradeError::BadScore { line, value }`: a score is not a number from 0 to 100; `value` is
     the trimmed text.
2. `Display` for `GradeError`, with exactly these messages:
   - `line 2: missing ':'`
   - `line 2: missing name`
   - `line 2: no scores`
   - `line 2: 'abc' is not a score from 0 to 100`
3. `Student::average(&self) -> f64` and `Student::letter(&self) -> char`:
   `A` for an average of 90 or more, `B` 80+, `C` 70+, `D` 60+, `F` below.
4. The `Report` trait for `Student`: `report()` returns `"Ana: 91.7 (A)"`, the average with one
   decimal.
5. `ranking(students, min_average) -> Vec<&str>`: the names of students whose average is at
   least `min_average`, best average first, alphabetical on a tie.

> This kata reviews the whole phase: an error enum with `Display` and `std::error::Error`,
> `?`, `Option` to `Result`, a trait, iterator chains and sorting with closures.
