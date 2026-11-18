import assert from "node:assert/strict";
import { describe, test } from "node:test";
import { screen, within } from "@testing-library/react";
import { ALL_MOVIES, fakeApi, last, movie, page, start, statusText, titles } from "./movie-helpers.tsx";
import { expectAbsent } from "./movie-helpers.tsx";

describe("first load", () => {
  test("asks once for the first page of every movie", () => {
    const { api, calls } = fakeApi();
    start(api);
    assert.equal(calls.length, 1);
    assert.deepEqual(last(calls).query, ALL_MOVIES);
  });

  test("says it is loading while it waits", () => {
    const { api } = fakeApi();
    start(api);
    assert.equal(statusText(), "Loading movies");
    expectAbsent(screen.queryByRole("list", { name: "Movies" }));
    expectAbsent(screen.queryByRole("alert"));
  });

  test("lists the movies with a heading and their details", async () => {
    const { api, calls } = fakeApi();
    start(api);
    await last(calls).answer(
      page([
        movie(1, { title: "The Matrix", year: 1999, genres: ["Action", "Sci-Fi"], rating: 8.7 }),
        movie(2, { title: "Up", year: 2009, genres: ["Animation"], rating: 8 }),
      ]),
    );
    assert.deepEqual(titles(), ["The Matrix", "Up"]);
    const list = screen.getByRole("list", { name: "Movies" });
    const items = within(list).getAllByRole("listitem");
    assert.equal(items.length, 2);
    assert.ok(within(items[0] as HTMLElement).getByRole("heading", { level: 3, name: "The Matrix" }));
    assert.ok(within(items[0] as HTMLElement).getByText("1999 · Action, Sci-Fi · ★ 8.7"));
    assert.ok(within(items[1] as HTMLElement).getByText("2009 · Animation · ★ 8.0"));
  });

  test("the status counts what is shown out of every match", async () => {
    const { api, calls } = fakeApi();
    start(api);
    await last(calls).answer(page([movie(1), movie(2)], { total: 40, pageCount: 20 }));
    assert.equal(statusText(), "Showing 2 of 40 movies");
  });

  test("one movie is one movie", async () => {
    const { api, calls } = fakeApi();
    start(api);
    await last(calls).answer(page([movie(1)]));
    assert.equal(statusText(), "Showing 1 of 1 movie");
  });

  test("no movies at all is said, without an empty list", async () => {
    const { api, calls } = fakeApi();
    start(api);
    await last(calls).answer(page([]));
    assert.equal(statusText(), "No movies found");
    expectAbsent(screen.queryByRole("list", { name: "Movies" }));
  });
});

describe("errors", () => {
  test("a failed request shows an alert and nothing else", async () => {
    const { api, calls } = fakeApi();
    start(api);
    await last(calls).fail();
    const alert = screen.getByRole("alert");
    assert.ok(within(alert).getByText("Could not load movies."));
    assert.ok(within(alert).getByRole("button", { name: "Try again" }));
    assert.equal(statusText(), "");
    expectAbsent(screen.queryByRole("list", { name: "Movies" }));
  });

  test("Try again asks for the same page again and can succeed", async () => {
    const { api, calls } = fakeApi();
    const { user } = start(api);
    await last(calls).fail();
    await user.click(screen.getByRole("button", { name: "Try again" }));
    assert.equal(calls.length, 2);
    assert.deepEqual(last(calls).query, ALL_MOVIES);
    assert.equal(statusText(), "Loading movies");
    expectAbsent(screen.queryByRole("alert"));
    await last(calls).answer(page([movie(7)]));
    assert.deepEqual(titles(), ["Movie 7"]);
  });
});

describe("leaving", () => {
  test("unmounting aborts the request in flight", () => {
    const { api, calls } = fakeApi();
    const { unmount } = start(api);
    assert.equal(last(calls).signal.aborted, false);
    unmount();
    assert.equal(last(calls).signal.aborted, true);
  });

  test("an answer that arrives after unmounting is ignored", async () => {
    const { api, calls } = fakeApi();
    const { unmount } = start(api);
    unmount();
    await last(calls).answer(page([movie(1)]));
    expectAbsent(screen.queryByRole("list", { name: "Movies" }));
  });
});
