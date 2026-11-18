Now the user chooses what to see. Above the list, a form with `role="search"`:

- a search field (`<input type="search">`) labelled `Search movies` and a `Search` button.
  Submitting (the button or Enter) searches for the trimmed text;
- a select labelled `Genre`: `All genres` (genre `null`), then one option per entry of the
  `genres` prop, in order;
- a select labelled `Sort by`: `Relevance`, `Newest` and `Top rated`, for the sorts
  `relevance`, `year` and `rating`.

Changing a select searches at once, with the text **last searched** (not what is typed in the
box and not submitted yet). Every search asks for page 1. Submitting the search that is
already shown does not ask again, unless it failed.

Then the races, which are the point of this stage:

- A new search **aborts** the request in flight (its signal is aborted).
- An API may answer anyway. Whatever order the answers arrive in, only the latest search is
  shown, and a late failure of an old search shows no error.
- While a new search loads, the movies already shown stay, the list gets
  `aria-busy="true"` and the status says `Loading movies`. When it fails, they go and the
  alert shows; `Try again` repeats the search that failed.
