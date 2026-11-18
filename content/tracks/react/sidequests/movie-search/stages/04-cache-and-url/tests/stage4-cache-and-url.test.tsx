import assert from "node:assert/strict";
import { describe, test } from "node:test";
import { render, screen, waitFor } from "@testing-library/react";
import { userEvent } from "@testing-library/user-event";
import { MovieSearch } from "../MovieSearch.tsx";
import { ALL_MOVIES, fakeApi, last, movie, page, start, statusText, titles, type User } from "./movie-helpers.tsx";

const GENRES = ["Action", "Drama", "Horror"];
const searchBox = () => screen.getByRole("searchbox", { name: "Search movies" }) as HTMLInputElement;
const genreSelect = () => screen.getByRole("combobox", { name: "Genre" }) as HTMLSelectElement;
const sortSelect = () => screen.getByRole("combobox", { name: "Sort by" }) as HTMLSelectElement;
const loadMore = () => screen.getByRole("button", { name: "Load more" });

async function searchFor(user: User, text: string): Promise<void> {
  await user.clear(searchBox());
  if (text !== "") {
    await user.type(searchBox(), text);
  }
  await user.click(screen.getByRole("button", { name: "Search" }));
}

/** Starts the page on a URL, as when a shared link is opened. */
function openAt(url: string, api: ReturnType<typeof fakeApi>["api"]) {
  window.history.replaceState(null, "", url);
  const user = userEvent.setup();
  render(<MovieSearch api={api} genres={GENRES} />);
  return user;
}

describe("cache", () => {
  test("going back to a search already answered shows it at once, without asking", async () => {
    const { api, calls } = fakeApi();
    const { user } = start(api, GENRES);
    await last(calls).answer(page([movie(1), movie(2)]));
    await user.selectOptions(genreSelect(), "Drama");
    await last(calls).answer(page([movie(2)]));
    await user.selectOptions(genreSelect(), "All genres");
    assert.equal(calls.length, 2);
    assert.deepEqual(titles(), ["Movie 1", "Movie 2"]);
    assert.equal(statusText(), "Showing 2 of 2 movies");
  });

  test("a cached search also cancels the one in flight", async () => {
    const { api, calls } = fakeApi();
    const { user } = start(api, GENRES);
    await last(calls).answer(page([movie(1)]));
    await user.selectOptions(genreSelect(), "Drama");
    const drama = last(calls);
    await user.selectOptions(genreSelect(), "All genres");
    assert.equal(drama.signal.aborted, true);
    await drama.answer(page([movie(5)]));
    assert.deepEqual(titles(), ["Movie 1"]);
  });

  test("failures are not cached", async () => {
    const { api, calls } = fakeApi();
    const { user } = start(api, GENRES);
    await last(calls).answer(page([movie(1)]));
    await searchFor(user, "up");
    await last(calls).fail();
    await searchFor(user, "");
    await searchFor(user, "up");
    assert.equal(calls.length, 3);
    assert.deepEqual(last(calls).query, { ...ALL_MOVIES, text: "up" });
  });

  test("pages loaded with Load more come from the cache too", async () => {
    const { api, calls } = fakeApi();
    const { user } = start(api, GENRES);
    await last(calls).answer(page([movie(1)], { total: 2, page: 1, pageCount: 2 }));
    await user.click(loadMore());
    await last(calls).answer(page([movie(2)], { total: 2, page: 2, pageCount: 2 }));
    await user.selectOptions(sortSelect(), "Newest");
    await last(calls).answer(page([movie(2), movie(1)]));
    await user.selectOptions(sortSelect(), "Relevance");
    assert.deepEqual(titles(), ["Movie 1"]);
    await user.click(loadMore());
    assert.deepEqual(titles(), ["Movie 1", "Movie 2"]);
    assert.equal(calls.length, 3);
  });

  test("Refresh asks again for the search shown, keeping it until the answer", async () => {
    const { api, calls } = fakeApi();
    const { user } = start(api, GENRES);
    await last(calls).answer(page([movie(1)], { total: 2, page: 1, pageCount: 2 }));
    await user.click(loadMore());
    await last(calls).answer(page([movie(2)], { total: 2, page: 2, pageCount: 2 }));
    await user.click(screen.getByRole("button", { name: "Refresh" }));
    assert.equal(calls.length, 3);
    assert.deepEqual(last(calls).query, ALL_MOVIES);
    assert.equal(screen.getByRole("list", { name: "Movies" }).getAttribute("aria-busy"), "true");
    await last(calls).answer(page([movie(7)], { total: 3, page: 1, pageCount: 2 }));
    assert.deepEqual(titles(), ["Movie 7"]);
    await user.click(loadMore());
    assert.equal(calls.length, 4, "the old page 2 was dropped with the refresh");
  });
});

describe("the search in the URL", () => {
  test("each search is written to the URL, without the defaults", async () => {
    const { api } = fakeApi();
    const { user } = start(api, GENRES);
    assert.equal(window.location.search, "");
    await searchFor(user, "night of");
    assert.equal(new URLSearchParams(window.location.search).get("q"), "night of");
    await user.selectOptions(genreSelect(), "Horror");
    await user.selectOptions(sortSelect(), "Newest");
    const params = new URLSearchParams(window.location.search);
    assert.deepEqual([...params.keys()].sort(), ["genre", "q", "sort"]);
    assert.equal(params.get("genre"), "Horror");
    assert.equal(params.get("sort"), "year");
    await user.selectOptions(genreSelect(), "All genres");
    await user.selectOptions(sortSelect(), "Relevance");
    await searchFor(user, "");
    assert.equal(window.location.search, "");
  });

  test("a link opens the page on its search", () => {
    const { api, calls } = fakeApi();
    openAt("/?q=alien&genre=Horror&sort=rating", api);
    assert.equal(calls.length, 1);
    assert.deepEqual(last(calls).query, { text: "alien", genre: "Horror", sort: "rating", page: 1 });
    assert.equal(searchBox().value, "alien");
    assert.equal(genreSelect().selectedOptions[0]?.textContent, "Horror");
    assert.equal(sortSelect().selectedOptions[0]?.textContent, "Top rated");
  });

  test("values the page does not know are ignored", () => {
    const { api, calls } = fakeApi();
    openAt("/?q=%20up%20&genre=Western&sort=price", api);
    assert.deepEqual(last(calls).query, { ...ALL_MOVIES, text: "up" });
    assert.equal(genreSelect().selectedOptions[0]?.textContent, "All genres");
    assert.equal(sortSelect().selectedOptions[0]?.textContent, "Relevance");
  });

  test("Load more does not change the URL or the history", async () => {
    const { api, calls } = fakeApi();
    const { user } = start(api, GENRES);
    await last(calls).answer(page([movie(1)], { total: 2, page: 1, pageCount: 2 }));
    const before = window.history.length;
    await user.click(loadMore());
    assert.equal(window.location.search, "");
    assert.equal(window.history.length, before);
  });

  test("the browser's Back button returns to the previous search", async () => {
    const { api, calls } = fakeApi();
    const { user } = start(api, GENRES);
    await last(calls).answer(page([movie(1)]));
    await searchFor(user, "alien");
    await last(calls).answer(page([movie(2, { title: "Alien" })]));
    await user.selectOptions(genreSelect(), "Horror");
    await last(calls).answer(page([movie(3, { title: "Alien: Horror" })]));

    window.history.back();
    await waitFor(() => assert.equal(genreSelect().selectedOptions[0]?.textContent, "All genres"));
    assert.deepEqual(titles(), ["Alien"]);
    assert.equal(searchBox().value, "alien");

    window.history.back();
    await waitFor(() => assert.equal(searchBox().value, ""));
    assert.deepEqual(titles(), ["Movie 1"]);
    assert.equal(calls.length, 3, "both searches came from the cache");

    window.history.forward();
    await waitFor(() => assert.equal(searchBox().value, "alien"));
    assert.deepEqual(titles(), ["Alien"]);
  });
});
