Resellers buy dozens of tickets and sell them on. From now on a customer holds **at most 4
tickets per show**: their active seat bookings plus their standing places.

Change `book_seat` (in `booking.sql`) and `buy_standing` (in `standing.sql`): a purchase that
would take the customer over 4 tickets for that show returns `NULL` and sells nothing. Their
signatures stay the same, and so do the rules of the earlier stages; a cancelled booking no
longer counts.

**The races** (Ana already holds 3 tickets for show 1):

- Session A buys Ana a 4th ticket and stays open; session B tries to buy Ana another one, a
  seat or a standing place. B **waits** for A, then returns `NULL`: Ana ends with 4.
- Purchases of **different customers** still do not wait for each other (the stage 1 race of
  two seats runs again with your new code).

> [!TIP]
> The limit is a rule over one customer's rows, so Ana's purchases must take turns while
> Bia's go on. Lock the row that stands for "Ana" before counting her tickets. Locking the
> whole table, or the show, would serialize customers who have nothing to do with each other.
> When one function locks two rows (the customer, then the show), every function must lock
> them in the same order, or two sessions can deadlock.
