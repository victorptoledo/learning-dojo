# Sidequest: Movie search

A list of movies is the "hello world" of data fetching, and the easiest place to get it
subtly wrong: two answers that arrive in the wrong order, a spinner that never stops, a
"Load more" that adds the same movie twice, a Back button that forgets what you searched.
Build `MovieSearch.tsx`, a page that searches a movie API and gets all of that right.

The API is a prop, typed in `movies.ts` (keep those types as they are). The tests pass a fake
one whose answers they release one by one, in any order, or make fail, and then look at the
page only through roles, labels and visible text: they never see your state or your effects.

```tsx
import { MovieSearch } from "./MovieSearch.tsx";
import type { MovieApi } from "./movies.ts";

const api: MovieApi = {
  async search(query, signal) {
    const params = new URLSearchParams({ q: query.text, sort: query.sort, page: String(query.page) });
    if (query.genre) params.set("genre", query.genre);
    const response = await fetch(`/api/movies?${params}`, { signal });
    if (!response.ok) throw new Error(`HTTP ${response.status}`);
    return response.json();
  },
};

createRoot(document.getElementById("root")!).render(<MovieSearch api={api} genres={["Action", "Drama"]} />);
```

Plain React 19 and TypeScript, no data or router library: TanStack Query and React Router
solve these problems for you, and this sidequest is about knowing which problems they solve.
`npm test` compiles the project and runs the tests in `tests/`, which are yours too.
