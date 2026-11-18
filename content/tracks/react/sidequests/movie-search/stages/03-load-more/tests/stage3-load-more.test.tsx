import assert from "node:assert/strict";
import { describe, test } from "node:test";
import { screen, within } from "@testing-library/react";
import { ALL_MOVIES, fakeApi, last, movie, page, start, statusText, titles } from "./movie-helpers.tsx";
import { expectAbsent, expectFocus } from "./movie-helpers.tsx";

const loadMore = () => screen.queryByRole("button", { name: "Load more" }) as HTMLButtonElement | null;

function firstPage() {
  return page([movie(1), movie(2)], { total: 5, page: 1, pageCount: 3 });
}

describe("load more", () => {
  test("is offered only while there are more pages", async () => {
    const { api, calls } = fakeApi();
    start(api);
    expectAbsent(loadMore());
    await last(calls).answer(firstPage());
    assert.ok(loadMore());
    assert.equal(loadMore()?.disabled, false);
  });

  test("is not offered on the last page", async () => {
    const { api, calls } = fakeApi();
    start(api);
    await last(calls).answer(page([movie(1)], { total: 1, page: 1, pageCount: 1 }));
    expectAbsent(loadMore());
  });

  test("asks for the next page of the same search", async () => {
    const { api, calls } = fakeApi();
    const { user } = start(api, ["Drama"]);
    await last(calls).answer(firstPage());
    await user.selectOptions(screen.getByRole("combobox", { name: "Genre" }), "Drama");
    await last(calls).answer(firstPage());
    await user.click(loadMore() as HTMLButtonElement);
    assert.deepEqual(last(calls).query, { ...ALL_MOVIES, genre: "Drama", page: 2 });
  });

  test("keeps the list while the next page loads, and cannot be pressed twice", async () => {
    const { api, calls } = fakeApi();
    const { user } = start(api);
    await last(calls).answer(firstPage());
    await user.click(loadMore() as HTMLButtonElement);
    assert.equal(loadMore()?.disabled, true);
    assert.equal(statusText(), "Loading more movies");
    assert.deepEqual(titles(), ["Movie 1", "Movie 2"]);
    await user.click(loadMore() as HTMLButtonElement);
    assert.equal(calls.length, 2);
  });

  test("adds the next page below and counts again", async () => {
    const { api, calls } = fakeApi();
    const { user } = start(api);
    await last(calls).answer(firstPage());
    await user.click(loadMore() as HTMLButtonElement);
    await last(calls).answer(page([movie(3), movie(4)], { total: 5, page: 2, pageCount: 3 }));
    assert.deepEqual(titles(), ["Movie 1", "Movie 2", "Movie 3", "Movie 4"]);
    assert.equal(statusText(), "Showing 4 of 5 movies");
    assert.equal(loadMore()?.disabled, false);
  });

  test("moves the focus to the first movie it added", async () => {
    const { api, calls } = fakeApi();
    const { user } = start(api);
    await last(calls).answer(firstPage());
    await user.click(loadMore() as HTMLButtonElement);
    await last(calls).answer(page([movie(3), movie(4)], { total: 5, page: 2, pageCount: 3 }));
    const list = screen.getByRole("list", { name: "Movies" });
    expectFocus(within(list).getByRole("heading", { name: "Movie 3" }));
  });

  test("skips movies that are already shown", async () => {
    const { api, calls } = fakeApi();
    const { user } = start(api);
    await last(calls).answer(firstPage());
    await user.click(loadMore() as HTMLButtonElement);
    await last(calls).answer(page([movie(2), movie(3)], { total: 6, page: 2, pageCount: 3 }));
    assert.deepEqual(titles(), ["Movie 1", "Movie 2", "Movie 3"]);
    assert.equal(statusText(), "Showing 3 of 6 movies");
  });

  test("disappears after the last page", async () => {
    const { api, calls } = fakeApi();
    const { user } = start(api);
    await last(calls).answer(page([movie(1)], { total: 2, page: 1, pageCount: 2 }));
    await user.click(loadMore() as HTMLButtonElement);
    await last(calls).answer(page([movie(2)], { total: 2, page: 2, pageCount: 2 }));
    expectAbsent(loadMore());
    assert.equal(statusText(), "Showing 2 of 2 movies");
  });

  test("a failed page keeps the list and can be tried again", async () => {
    const { api, calls } = fakeApi();
    const { user } = start(api);
    await last(calls).answer(firstPage());
    await user.click(loadMore() as HTMLButtonElement);
    await last(calls).fail();
    const alert = screen.getByRole("alert");
    assert.ok(within(alert).getByText("Could not load more movies."));
    assert.deepEqual(titles(), ["Movie 1", "Movie 2"]);
    await user.click(within(alert).getByRole("button", { name: "Try again" }));
    assert.deepEqual(last(calls).query, { ...ALL_MOVIES, page: 2 });
    expectAbsent(screen.queryByRole("alert"));
    await last(calls).answer(page([movie(3)], { total: 5, page: 2, pageCount: 3 }));
    assert.deepEqual(titles(), ["Movie 1", "Movie 2", "Movie 3"]);
  });

  test("a new search drops the pages and the page in flight", async () => {
    const { api, calls } = fakeApi();
    const { user } = start(api);
    await last(calls).answer(firstPage());
    await user.click(loadMore() as HTMLButtonElement);
    const pageTwo = last(calls);
    await user.type(screen.getByRole("searchbox", { name: "Search movies" }), "up{Enter}");
    assert.equal(pageTwo.signal.aborted, true);
    assert.deepEqual(last(calls).query, { ...ALL_MOVIES, text: "up" });
    await last(calls).answer(page([movie(9, { title: "Up" })], { total: 3, page: 1, pageCount: 2 }));
    await pageTwo.answer(page([movie(3)], { total: 5, page: 2, pageCount: 3 }));
    assert.deepEqual(titles(), ["Up"]);
    assert.equal(statusText(), "Showing 1 of 3 movies");
    await user.click(loadMore() as HTMLButtonElement);
    assert.deepEqual(last(calls).query, { ...ALL_MOVIES, text: "up", page: 2 });
  });
});
