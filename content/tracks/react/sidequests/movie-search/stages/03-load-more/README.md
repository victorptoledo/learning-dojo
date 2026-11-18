Twenty movies are rarely enough. When the page shown is not the last one
(`page < pageCount`), a `Load more` button follows the list.

- It asks for the next page of the **same** search. While it loads, the button is disabled
  (so a double click asks once), the list stays and the status says `Loading more movies`.
- The answer's movies are added below. A movie whose `id` is already shown is skipped
  (the data moved between two pages), and the status counts the movies shown out of the
  answer's `total`: `Showing 4 of 5 movies`.
- The focus moves to the heading of the first movie added, so a keyboard user carries on
  reading from there instead of going back to the top. (A heading takes focus with
  `tabIndex={-1}`.)
- After the last page the button is gone.
- A failed page keeps the list and shows an `alert` with `Could not load more movies.` and
  a `Try again` button that asks for that page again.
- A new search aborts a page in flight, drops the pages loaded so far and starts again from
  page 1; an old page that still arrives is ignored.
