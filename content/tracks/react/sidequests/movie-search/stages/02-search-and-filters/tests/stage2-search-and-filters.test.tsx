import assert from "node:assert/strict";
import { describe, test } from "node:test";
import { screen, within } from "@testing-library/react";
import { ALL_MOVIES, fakeApi, last, movie, page, start, statusText, titles, type User } from "./movie-helpers.tsx";
import { expectAbsent } from "./movie-helpers.tsx";

const GENRES = ["Action", "Drama", "Horror"];
const searchBox = () => screen.getByRole("searchbox", { name: "Search movies" });
const genreSelect = () => screen.getByRole("combobox", { name: "Genre" }) as HTMLSelectElement;
const sortSelect = () => screen.getByRole("combobox", { name: "Sort by" }) as HTMLSelectElement;
const optionsOf = (select: HTMLSelectElement) =>
  within(select)
    .getAllByRole("option")
    .map((option) => option.textContent?.trim());

async function searchFor(user: User, text: string): Promise<void> {
  await user.clear(searchBox());
  if (text !== "") {
    await user.type(searchBox(), text);
  }
  await user.click(screen.getByRole("button", { name: "Search" }));
}

describe("the search form", () => {
  test("has a search box, a genre and a sort order", () => {
    const { api } = fakeApi();
    start(api, GENRES);
    assert.ok(screen.getByRole("search"));
    assert.ok(searchBox());
    assert.deepEqual(optionsOf(genreSelect()), ["All genres", ...GENRES]);
    assert.deepEqual(optionsOf(sortSelect()), ["Relevance", "Newest", "Top rated"]);
  });

  test("searches for the trimmed text with the Search button", async () => {
    const { api, calls } = fakeApi();
    const { user } = start(api, GENRES);
    await last(calls).answer(page([movie(1)]));
    await searchFor(user, "  the matrix ");
    assert.equal(calls.length, 2);
    assert.deepEqual(last(calls).query, { ...ALL_MOVIES, text: "the matrix" });
  });

  test("Enter in the search box searches too", async () => {
    const { api, calls } = fakeApi();
    const { user } = start(api, GENRES);
    await user.type(searchBox(), "alien{Enter}");
    assert.deepEqual(last(calls).query, { ...ALL_MOVIES, text: "alien" });
  });

  test("a genre searches at once, with the text last searched", async () => {
    const { api, calls } = fakeApi();
    const { user } = start(api, GENRES);
    await searchFor(user, "night");
    await user.type(searchBox(), " of the living dead");
    await user.selectOptions(genreSelect(), "Horror");
    assert.deepEqual(last(calls).query, { ...ALL_MOVIES, text: "night", genre: "Horror" });
    await user.selectOptions(genreSelect(), "All genres");
    assert.deepEqual(last(calls).query, { ...ALL_MOVIES, text: "night", genre: null });
  });

  test("each sort order asks for its own sort", async () => {
    const { api, calls } = fakeApi();
    const { user } = start(api, GENRES);
    await user.selectOptions(sortSelect(), "Newest");
    assert.deepEqual(last(calls).query, { ...ALL_MOVIES, sort: "year" });
    await user.selectOptions(sortSelect(), "Top rated");
    assert.deepEqual(last(calls).query, { ...ALL_MOVIES, sort: "rating" });
    await user.selectOptions(genreSelect(), "Drama");
    assert.deepEqual(last(calls).query, { ...ALL_MOVIES, sort: "rating", genre: "Drama" });
  });

  test("searching again for what is shown does not ask again", async () => {
    const { api, calls } = fakeApi();
    const { user } = start(api, GENRES);
    await searchFor(user, "up");
    await last(calls).answer(page([movie(1)]));
    await searchFor(user, " up ");
    assert.equal(calls.length, 2);
  });
});

describe("races", () => {
  test("a new search aborts the one in flight", async () => {
    const { api, calls } = fakeApi();
    const { user } = start(api, GENRES);
    await searchFor(user, "alien");
    assert.equal(calls[0]?.signal.aborted, true);
    assert.equal(last(calls).signal.aborted, false);
  });

  test("only the latest search is shown, whatever order the answers come in", async () => {
    const { api, calls } = fakeApi();
    const { user } = start(api, GENRES);
    await searchFor(user, "alien");
    await searchFor(user, "aliens");
    await calls[2]?.answer(page([movie(3, { title: "Aliens" })]));
    await calls[1]?.answer(page([movie(2, { title: "Alien" })]));
    await calls[0]?.answer(page([movie(1)]));
    assert.deepEqual(titles(), ["Aliens"]);
    assert.equal(statusText(), "Showing 1 of 1 movie");
  });

  test("a late failure of an old search is ignored", async () => {
    const { api, calls } = fakeApi();
    const { user } = start(api, GENRES);
    await searchFor(user, "up");
    await calls[1]?.answer(page([movie(1, { title: "Up" })]));
    await calls[0]?.fail();
    expectAbsent(screen.queryByRole("alert"));
    assert.deepEqual(titles(), ["Up"]);
  });

  test("the old results stay, marked busy, until the new ones arrive", async () => {
    const { api, calls } = fakeApi();
    const { user } = start(api, GENRES);
    await last(calls).answer(page([movie(1), movie(2)]));
    await user.selectOptions(genreSelect(), "Drama");
    assert.deepEqual(titles(), ["Movie 1", "Movie 2"]);
    assert.equal(screen.getByRole("list", { name: "Movies" }).getAttribute("aria-busy"), "true");
    assert.equal(statusText(), "Loading movies");
    await last(calls).answer(page([movie(2)]));
    assert.deepEqual(titles(), ["Movie 2"]);
    assert.notEqual(screen.getByRole("list", { name: "Movies" }).getAttribute("aria-busy"), "true");
  });

  test("Try again repeats the search that failed", async () => {
    const { api, calls } = fakeApi();
    const { user } = start(api, GENRES);
    await last(calls).answer(page([movie(1)]));
    await user.selectOptions(genreSelect(), "Action");
    await last(calls).fail();
    assert.equal(titles().length, 0);
    await user.click(screen.getByRole("button", { name: "Try again" }));
    assert.deepEqual(last(calls).query, { ...ALL_MOVIES, genre: "Action" });
    await last(calls).answer(page([movie(4)]));
    assert.deepEqual(titles(), ["Movie 4"]);
  });

  test("after a failure, searching again for the same text asks again", async () => {
    const { api, calls } = fakeApi();
    const { user } = start(api, GENRES);
    await last(calls).fail();
    await searchFor(user, "");
    assert.equal(calls.length, 2);
    assert.deepEqual(last(calls).query, ALL_MOVIES);
  });
});
