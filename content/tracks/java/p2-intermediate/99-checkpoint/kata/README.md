# Checkpoint 2: Library Loans

A small library lends books to members. The records (`Book`, `Member`, `Loan`), the
`Identified<ID>` interface and the exceptions are written. You write the generic repository
and the library's rules.

## 1. `InMemoryRepository<ID, T extends Identified<ID>>`

- `save(item)`: stores it under `item.id()`, **replacing** an item with the same id (which
  keeps its original position), and returns it.
- `findById(id)`: `Optional` of the item, empty when there is none.
- `findAll()` and `findAll(filter)`: the items in **first-saved order**, as unmodifiable lists.
- `delete(id)`: removes it; `true` if something was removed. `count()`: how many there are.

## 2. `Library`

The library gets today's date from a `Supplier<LocalDate>`; loans last `LOAN_DAYS` (14) days.

- `lend(isbn, memberId)`: returns the new `Loan`, due 14 days from today. It refuses with:

  | When | Exception (already written) | Message |
  | --- | --- | --- |
  | unknown book | `LibraryException.NotFound("book", isbn)` | `book 999 not found` |
  | unknown member | `LibraryException.NotFound("member", id)` | `member m9 not found` |
  | the book is lent | `LibraryException.NotAvailable(isbn)` | `book 111 is already lent` |
  | the member holds `maxLoans` books | `LibraryException.LoanLimitReached(id, held)` | `member m2 already has 1 loans` |

  Check in that order: book, member, availability, limit.
- `giveBack(isbn)`: ends the loan; `NotFound("loan", isbn)` when that book is not lent.
- `available()`: the books not lent, **sorted by title**.
- `overdue()`: the loans whose due date is **before** today (due today is not overdue),
  sorted by due date, then isbn.
- `titlesByMember()`: member id → titles of the books they hold, ids sorted and titles sorted.

> This kata reviews the whole phase: a generic class with a bounded type parameter, a
> `Predicate<? super T>` parameter, `Optional.orElseThrow`, a sealed exception hierarchy, and
> streams with `groupingBy`, `mapping` and sorting.
