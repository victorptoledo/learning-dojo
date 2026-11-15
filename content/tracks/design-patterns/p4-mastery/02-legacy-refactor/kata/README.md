# Tame the God Class

`LibraryService` is the library's whole back office, grown over eight years: books, members,
loans, fines, notifications and a report in one class. It works. It also has most of the
smells in the catalogue: long methods, a member type passed around as a string and switched
on in three places, arrays used as records (`String[] {title, "in"}`, `Object[] {member,
due}`), duplicated lookups and money formatting done inline.

Your job is the everyday job of a senior developer: make it changeable **without changing
what it does**. The visible characterisation tests pass now; keep them green after every
small step. The grading tests check much more behaviour than the visible ones, so do not
"fix" anything you find odd: keep it.

## The rules

The public API of `LibraryService` stays exactly as it is (`addBook`, `addMember`, `borrow`,
`giveBack`, `payFine`, `fines`, `overdueReport`, `outbox`), with the same results, the same
exceptions and the same messages. Inside, you are free, with these constraints:

1. **Replace the type code with a type.** Add `enum MemberType { STUDENT, STAFF, GUEST }`
   with `static MemberType parse(String text)` (the exact lower-case names; anything else
   throws `IllegalArgumentException("unknown member type: " + text)`), and the rules that
   vary by type as methods: `int maxLoans()`, `int loanDays()`, `long lateFeeCents(long
   daysLate)`. Today's rules: student 3 loans, 14 days, 10 cents a day capped at 500; staff
   10 loans, 28 days, no fee; guest 1 loan, 7 days, 50 cents a day. The string `"student"`
   may appear at most once in all your sources.
2. **No long methods.** No method or constructor body in any class has more than 15
   non-blank lines.
3. **No arrays as records** (`String[]`, `Object[]`) and no `instanceof`, anywhere.
4. **A smaller coordinator.** `LibraryService` has at most 110 non-blank lines (comments
   count as blank), compares no strings with literals and formats no money itself.

A possible shape, not required: records `Book`, `Member` and `Loan` (with `daysLate(today)`),
a `Money.format(cents)` helper and an `OverdueReport` that renders the report from the loans.

## What the grading tests check

- Registration, limits, loan periods, fees and the student cap, the fine limit (more than
  5.00 owed blocks borrowing; exactly 5.00 does not), payments and the sorted overdue report,
  with every message.
- `MemberType` as described in rule 1.
- Rules 2 to 4, by reading your sources.

> Tip: work in the order of Fowler's catalogue. *Replace Type Code with Subclasses* (here an
> enum), *Replace Conditional with Polymorphism* for the three type switches, *Introduce
> Parameter Object* / *Replace Array with Object* for the arrays, *Extract Function* for the
> long methods and *Move Function* for the report.
