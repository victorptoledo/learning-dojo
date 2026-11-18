// Your own tests: `npm test` compiles the project and runs every tests/*.test.tsx. Add more as you go.
import assert from "node:assert/strict";
import { test } from "node:test";
import { act, render, screen } from "@testing-library/react";
import { MovieSearch } from "../MovieSearch.tsx";
import type { MovieApi, MoviePage } from "../movies.ts";

test("shows the movies the API answers", async () => {
  let answer: (page: MoviePage) => void = () => {};
  const api: MovieApi = { search: () => new Promise((resolve) => (answer = resolve)) };
  render(<MovieSearch api={api} />);
  await act(async () =>
    answer({ movies: [{ id: "1", title: "Up", year: 2009, genres: ["Animation"], rating: 8 }], total: 1, page: 1, pageCount: 1 }),
  );
  assert.ok(screen.getByRole("heading", { name: "Up" }));
});
