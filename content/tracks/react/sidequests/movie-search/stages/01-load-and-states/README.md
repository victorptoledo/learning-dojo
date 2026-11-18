Start with the first page. Export `MovieSearch` from `MovieSearch.tsx`; it takes `api` (and
`genres`, used from stage 2).

- When it mounts, it calls `api.search` **once** with
  `{ text: "", genre: null, sort: "relevance", page: 1 }` and a signal.
- An element with `role="status"` (a live region, so screen readers announce it) always says
  where things stand:
  - `Loading movies` while the request is in flight;
  - `Showing <shown> of <total> movies` once it is answered (`Showing 1 of 1 movie` for a
    single one): `total` comes from the answer and counts every match, not only this page;
  - `No movies found` when there are none: then no list is shown at all;
  - nothing (empty) when the request failed.
- The movies are a list named `Movies` (`<ul aria-label="Movies">`), one item per movie, in
  the API's order, each with the title as a level 3 heading and one line with
  `<year> · <genres joined by ", "> · ★ <rating with one decimal>`
  (`1999 · Action, Sci-Fi · ★ 8.7`).
- A failed request shows an `alert` with `Could not load movies.` and a `Try again` button,
  which asks for the same page again (and shows `Loading movies` meanwhile).
- Unmounting aborts the request in flight, and an answer that still arrives is ignored.
