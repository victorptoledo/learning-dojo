The real list has hundreds of tickets. Make the `Tickets` page useful and fast.

**Filters, in the URL.** Above the list:

- a search field (`<input type="search">`) labelled `Search tickets`: as you type, the list
  keeps the tickets whose title or requester contains the text, ignoring case and the
  spaces around it;
- a group named `Status` with four buttons, `All`, `Open`, `Pending` and `Closed`, each with
  `aria-pressed`; one is on at a time.

Both live in the URL (`/tickets?q=log&status=pending`, without the defaults), so a filtered
list can be shared and comes back with Back. They **replace** the history entry instead of
adding one per key, they are not a navigation (no focus move, no announcement: the focus
stays in the search field), and a link opens the list filtered. A status the app does not
know means `All`.

**A long list.** The page shows the first 100 matching tickets and says
`Showing <shown> of <matches> tickets`; a `Show more` button adds the next 100 until there
are no more. Changing a filter starts again from the first 100. With no match at all, the page
says `No tickets match` and shows no list.

**Render budget.** `renderLog.ts` is new in your folder: call ``trackRender(`row:<ticket id>`)``
once in every render of a ticket row (and do not change the file). The tests count:

| When | At most |
|---|---|
| the list appears | one render per row shown (rows past the first 100 are not rendered) |
| you type in the search field, or change the status | no render of a row that was shown before and still is; one for each row that appears |
| you press `Show more` | one render per row added |

`memo` on the row, stable props and a context that does not change on every key get you
there. `useDeferredValue` on the search text keeps typing smooth when the list is slow (the
tests do not measure it, your users will).
