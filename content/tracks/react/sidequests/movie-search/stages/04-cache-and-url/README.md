Two things users expect from any list on the web: going back is instant, and a link opens
the same search.

**A cache per search.** Keep every answer, per text, genre, sort and page, for as long as
the component lives (each `<MovieSearch>` has its own cache).

- A search whose first page is cached is shown at once: no request, no `Loading movies`.
  It still aborts a request in flight, whose late answer is ignored.
- `Load more` takes the next page from the cache too, when it is there.
- Failures are not cached: searching again asks again.
- A `Refresh` button in the search form asks for the search shown again, page 1, and forgets
  its cached pages. The movies stay, busy, until the answer arrives.

**The search in the URL.** The search shown lives in the address bar, so it can be shared:

- Each new search adds a history entry (`history.pushState`) with `q`, `genre` and `sort` in
  the query string, leaving out the defaults (empty text, all genres, relevance): `/` for
  the first page, `/?q=alien&genre=Horror&sort=year` for a full one. `Load more` does not
  change the URL.
- On mount the search is read from the URL, and the form shows it. A genre that is not in
  `genres` or a sort the page does not know counts as the default; the text is trimmed.
- The browser's Back and Forward buttons (`popstate`) bring back the search of that entry,
  from the cache when it is there, and the form follows.
