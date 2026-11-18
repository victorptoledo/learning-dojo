The list has only a summary of each ticket. The ticket page now loads the whole ticket with
`api.getTicket(id)`, the React 19 way.

- **`use` and Suspense.** The page reads the ticket with `use(promise)`. A Suspense boundary
  shows `Loading ticket` while a page opened directly (or retried) waits. The promise must be
  the same for the same id on every render, so keep one per id (a cache): a ticket seen
  before shows at once, without calling `getTicket` again. The ticket page does not wait for
  the ticket list.
- **The page** shows what stage 1 showed (now from the ticket), and a level 2 heading
  `Messages` with an ordered list named `Messages`: one item per message, in order, with its
  author and its body. Without messages it says `No messages yet` instead.
- **Errors** are caught by an error boundary. A `NotFoundError` gives the `Ticket not found`
  page. Any other error shows an `alert` with `Could not load this ticket.` and a `Try again`
  button that asks for the ticket again (a new promise: the failed one is forgotten).
- **Transitions.** Moving to another page is a transition (`startTransition`): when the next
  page has to wait, the current one **stays on screen** instead of flashing a loading state,
  and `<main>` has `aria-busy="true"` until the new page shows. Pages that need nothing new
  change at once. Filters (stage 3) are not transitions, so the search field follows every
  key.
