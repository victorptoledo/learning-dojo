# Checkpoint 1: The Library

The library's tables are in `fixture/01_library.sql`:

```text
authors (id, name)
books   (id, title, author_id → authors, year)
members (id, name, joined)
loans   (id, book_id → books, member_id → members, borrowed_on, due_on, returned_on)
fees    (loan_id → loans, primary key; member_id → members; amount)
```

A loan is **open** while `returned_on` is NULL. It is **overdue** on a day when it is open
and its `due_on` is before that day (a loan due today is not overdue yet). "Today" is
**2026-11-15** in every file: write it as `DATE '2026-11-15'`.

Write one query in each of the first four files and two statements in the last:

| File | Columns | Rows and order |
| --- | --- | --- |
| `available_books.sql` | title | books with no open loan; alphabetical |
| `member_activity.sql` | name, number of loans, number of open loans | every member, also those with no loans; most loans first, then name |
| `overdue.sql` | member name, title, days overdue (an integer) | overdue loans today; most days first, then member name, then title |
| `prolific_authors.sql` | name, number of books, number of loans of their books | authors with at least 2 books (a book never borrowed counts as a book); most loans first, then name |
| `return_books.sql` | (statements) | member 3 brings back all their open loans today. Insert into `fees` one row per **overdue** loan of member 3, with an amount of 0.25 per day overdue; then set `returned_on` of all their open loans to today |

In PostgreSQL, subtracting two dates gives the number of days between them:
`DATE '2026-11-15' - DATE '2026-11-03'` is `12`.

Try your work with `docker run -it --rm --network none -v "${PWD}:/work" dojo-sql play`. Every
test of `return_books.sql` runs your statements on a fresh copy of the data and rolls them back
afterwards; in psql, wrap them in `BEGIN;` ... `ROLLBACK;` to try them more than once.

There are 11 tests: 1 in `tests/visible.sql` and 10 hidden ones.
