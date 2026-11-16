# Library Loans

`dojo.library` lends books. Its packages look right (`domain`, `application`, `adapters`), but
the arrows do not: the loan policy reads a static `InMemoryLoans.INSTANCE`, the use cases create
their adapters and print to the console, and everything asks `LocalDate.now()`, so no test can
say "it is the 1st of March". You will **write the rule first** and then make the code obey it.

## Part 1: the rule as a fitness function

Write `dojo.fitness.HexagonalRules.violations(graph, root)` over the graph of
`ClassGraph.scan(root)`. It returns every forbidden use as `"<from> -> <to>"`, sorted, without
duplicates:

1. a class in `root.domain` uses a class in `root.application`, in `root.adapters`, or a class
   directly in `root` (the composition root);
2. a class in `root.application` uses a class in `root.adapters` or directly in `root`;
3. a class in `root.domain` or `root.application` uses `java.lang.System` (the console, the
   system clock, environment variables: all of them belong outside).

"In a package" includes subpackages; "directly in `root`" means the class's package **is**
`root`. Adapters and the composition root may use anything.

## Part 2: the hexagon

| Type | What it does |
| --- | --- |
| port `domain.Books` | `Optional<Book> find(String isbn)` |
| port `domain.Loans` | `List<Loan> byMember(String memberId)`, `Optional<Loan> byIsbn(String isbn)`, `void add(Loan loan)`, `void remove(String isbn)` |
| port `domain.Notifier` | `void send(String memberId, String message)` |
| `domain.LoanPolicy` | `LoanPolicy()`; `void checkCanBorrow(List<Loan> current, LocalDate today)`: a member with an overdue loan is refused with `"member has an overdue loan"`, then a member with 3 loans with `"member has 3 loans"` (both `LoanRefusedException`); `LocalDate dueDate(LocalDate borrowedOn)` is 14 days later |
| `application.BorrowBook` | `BorrowBook(Books, Loans, Notifier, Clock)`; `Loan borrow(String memberId, String isbn)` |
| `application.ReturnBook` | `ReturnBook(Loans, Notifier, Clock)`; `long giveBack(String isbn)` returns the late fee in cents |
| `adapters.InMemoryBooks` | implements `Books`; `add(Book)` stays |
| `adapters.InMemoryLoans` | implements `Loans`, with **no** static instance |
| `adapters.ConsoleNotifier` | implements `Notifier` by printing |
| `dojo.library.App` | `static Library create(Clock clock, Notifier notifier)`: one `InMemoryBooks`, one `InMemoryLoans`, both use cases |

`Book`, `Loan` (with `isOverdue` and `lateFeeCents`), `LoanRefusedException` and the `Library`
record keep their shape. The rules, today taken from the clock:

- `borrow`: an unknown book throws `IllegalArgumentException("unknown book " + isbn)`; a book
  already lent throws `LoanRefusedException("already on loan: " + isbn)`; then the policy decides.
  The loan runs from today to `dueDate(today)`, is added, and the member gets
  `"Due back on " + dueOn` (ISO date).
- `giveBack`: a book that is not lent throws `IllegalArgumentException("not on loan: " + isbn)`.
  The loan is removed; the fee is 25 cents per day after the due date, at most 1000; if it is not
  zero the member gets `"Late fee: " + fee + " cents"`.

## What the grading tests check

- `HexagonalRules` on small graphs, for each of the three rules and the cases that are allowed.
- The library's own graph: the three rules hold, the scan sees the library, and `InMemoryLoans`
  has no static field of its own type.
- Every borrowing and return rule, with a clock the tests move and a notifier they read.

The visible tests describe the target design, so they do not compile until the ports exist.
Keep your tests in the package `dojo`.
