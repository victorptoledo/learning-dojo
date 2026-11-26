import assert from "node:assert/strict";
import { beforeEach, describe, test } from "node:test";
import { act, render, screen, within } from "@testing-library/react";
import { userEvent } from "@testing-library/user-event";
import { IssueTracker } from "../IssueTracker.tsx";
import { matchesQuery, openCount, renameIssue, toggleIssue, type Issue, type IssueState } from "../issues.ts";
import { renderCount, renderTotal, resetRenders } from "../renderLog.ts";
import { createStore, type Store } from "../store.ts";

function sample(): IssueState {
  return Object.freeze({
    issues: Object.freeze(
      [
        { id: "a", title: "Login fails on Safari", done: false },
        { id: "b", title: "Typo on the home page", done: true },
        { id: "c", title: "Login button is grey", done: false },
        { id: "d", title: "Slow search", done: false },
      ].map((issue) => Object.freeze(issue)),
    ),
  });
}

/** A short description of an element for failure messages (never the element itself). */
function nameOf(element: Element | null): string {
  if (!element || element === document.body) {
    return "nothing (the page itself)";
  }
  const label = element.getAttribute("aria-label") ?? element.textContent ?? "";
  return `<${element.tagName.toLowerCase()}> "${label.trim().slice(0, 60)}"`;
}

// Compare elements with this, not assert.equal: a failing assert.equal prints the whole document.
function expectFocus(element: Element): void {
  if (document.activeElement !== element) {
    assert.fail(`the focus should be on ${nameOf(element)}, but it is on ${nameOf(document.activeElement)}`);
  }
}

type Started = { store: Store<IssueState>; user: ReturnType<typeof userEvent.setup> };

function start(state: IssueState = sample(), url = "/issues"): Started {
  window.history.replaceState(null, "", url);
  const store = createStore(state);
  const user = userEvent.setup();
  render(<IssueTracker store={store} />);
  return { store, user };
}

function titles(): string[] {
  const list = screen.queryByRole("list", { name: "Issues" });
  if (!list) {
    return [];
  }
  return within(list)
    .getAllByRole("button")
    .map((button) => (button.textContent ?? "").trim());
}

const status = () => (screen.getByRole("status").textContent ?? "").replace(/\s+/g, " ").trim();
const query = () => new URLSearchParams(window.location.search).get("q");
const filterInput = () => screen.getByRole("textbox", { name: "Filter" }) as HTMLInputElement;
const titleInput = () => screen.getByRole("textbox", { name: "Title" }) as HTMLInputElement;
const checkbox = (title: string) => screen.getByRole("checkbox", { name: `Done: ${title}` }) as HTMLInputElement;

beforeEach(() => resetRenders());

describe("issues.ts", () => {
  test("toggle and rename keep unchanged issues and return the same state when nothing changes", () => {
    const state = sample();
    const toggled = toggleIssue(state, "b");
    assert.equal(toggled.issues[1]?.done, false);
    assert.equal(toggled.issues[0], state.issues[0], "an unchanged issue is the same object");
    assert.equal(toggleIssue(state, "zzz"), state);

    const renamed = renameIssue(state, "d", "  Search is slow  ");
    assert.equal(renamed.issues[3]?.title, "Search is slow");
    assert.equal(renamed.issues[2], state.issues[2], "an unchanged issue is the same object");
    assert.equal(renameIssue(state, "d", "   "), state, "an empty title changes nothing");
    assert.equal(renameIssue(state, "d", " Slow search "), state, "the same title changes nothing");
    assert.equal(renameIssue(state, "zzz", "New"), state);
  });

  test("matchesQuery ignores case and spaces around the query; openCount", () => {
    const issue: Issue = { id: "a", title: "Login fails on Safari", done: false };
    assert.equal(matchesQuery(issue, "  safari "), true);
    assert.equal(matchesQuery(issue, ""), true);
    assert.equal(matchesQuery(issue, "chrome"), false);
    assert.equal(openCount(sample().issues), 3);
  });
});

describe("store.ts", () => {
  test("setState with a value or an updater notifies every listener; unsubscribe stops it", () => {
    const store = createStore({ count: 0 });
    let first = 0;
    let second = 0;
    const stop = store.subscribe(() => {
      first += 1;
    });
    store.subscribe(() => {
      second += 1;
    });
    store.setState({ count: 1 });
    store.setState((current) => ({ count: current.count + 1 }));
    assert.deepEqual(store.getState(), { count: 2 });
    assert.deepEqual([first, second], [2, 2]);
    stop();
    store.setState({ count: 3 });
    assert.deepEqual([first, second], [2, 3]);
  });

  test("setting the same state object notifies nobody", () => {
    const initial = { count: 0 };
    const store = createStore(initial);
    let calls = 0;
    store.subscribe(() => {
      calls += 1;
    });
    store.setState(initial);
    store.setState((current) => current);
    assert.equal(calls, 0);
  });
});

describe("the tracker", () => {
  test("lists the issues with a status of how many are open", () => {
    start();
    assert.deepEqual(titles(), ["Login fails on Safari", "Typo on the home page", "Login button is grey", "Slow search"]);
    assert.equal(status(), "3 of 4 open");
    assert.equal(checkbox("Typo on the home page").checked, true);
    assert.ok(screen.getByText("Select an issue to edit it"));
    assert.equal(renderTotal("row:"), 4, "each row renders once");
  });

  test("ticking an issue updates the store and the status, and renders only that row", async () => {
    const { store, user } = start();
    resetRenders();
    await user.click(checkbox("Slow search"));
    assert.equal(store.getState().issues[3]?.done, true);
    assert.equal(checkbox("Slow search").checked, true);
    assert.equal(status(), "2 of 4 open");
    assert.equal(renderTotal("row:"), 1, "only the ticked row renders again");
    assert.equal(renderCount("row:d"), 1);
  });

  test("a change made to the store from outside shows up, and renders only that row", () => {
    const { store } = start();
    resetRenders();
    act(() => store.setState((state) => renameIssue(state, "a", "Login fails everywhere")));
    assert.ok(screen.getByRole("button", { name: "Login fails everywhere" }));
    assert.equal(renderTotal("row:"), 1);
    assert.equal(renderCount("row:a"), 1);
  });

  test("the filter narrows the list, keeps the query in the address bar and renders no row again", async () => {
    const { user } = start();
    const before = window.history.length;
    resetRenders();
    await user.type(filterInput(), " LOGIN ");
    assert.deepEqual(titles(), ["Login fails on Safari", "Login button is grey"]);
    assert.equal(query(), "LOGIN");
    assert.equal(window.history.length, before, "typing replaces the entry instead of adding Back steps");
    assert.equal(renderTotal("row:"), 0, "rows that stay visible do not render again");
    assert.equal(status(), "3 of 4 open", "the status counts every issue, not only the visible ones");

    await user.clear(filterInput());
    await user.type(filterInput(), "nothing like this");
    assert.ok(screen.getByText("No issues match"));
    await user.clear(filterInput());
    assert.equal(query(), null, "an empty filter leaves no q in the address");
    assert.equal(titles().length, 4);
  });

  test("opening a link with ?q= starts filtered", () => {
    start(sample(), "/issues?q=search");
    assert.equal(filterInput().value, "search");
    assert.deepEqual(titles(), ["Slow search"]);
  });

  test("selecting an issue presses its button, opens the editor and renders two rows at most", async () => {
    const { user } = start();
    await user.click(screen.getByRole("button", { name: "Slow search" }));
    assert.equal(screen.getByRole("button", { name: "Slow search" }).getAttribute("aria-pressed"), "true");
    assert.equal(titleInput().value, "Slow search");
    resetRenders();
    await user.click(screen.getByRole("button", { name: "Login button is grey" }));
    assert.equal(screen.getByRole("button", { name: "Slow search" }).getAttribute("aria-pressed"), "false");
    assert.equal(screen.getByRole("button", { name: "Login button is grey" }).getAttribute("aria-pressed"), "true");
    assert.equal(renderTotal("row:"), 2, "the row that was selected and the one that is now");
    resetRenders();
    await user.type(titleInput(), "!");
    assert.equal(renderTotal("row:"), 0, "typing in the editor renders no row");
  });

  test("the draft belongs to one issue: switching issues starts from the new title", async () => {
    const { store, user } = start();
    await user.click(screen.getByRole("button", { name: "Slow search" }));
    await user.clear(titleInput());
    await user.type(titleInput(), "Half-typed draft");
    await user.click(screen.getByRole("button", { name: "Login button is grey" }));
    assert.equal(titleInput().value, "Login button is grey");
    await user.click(screen.getByRole("button", { name: "Slow search" }));
    assert.equal(titleInput().value, "Slow search", "the unsaved draft was dropped");
    assert.equal(store.getState().issues[3]?.title, "Slow search");
  });

  test("saving renames the issue in the store and in its row only", async () => {
    const { store, user } = start();
    await user.click(screen.getByRole("button", { name: "Slow search" }));
    await user.clear(titleInput());
    await user.type(titleInput(), "  Search takes 5 s ");
    resetRenders();
    await user.click(screen.getByRole("button", { name: "Save" }));
    assert.equal(store.getState().issues[3]?.title, "Search takes 5 s");
    assert.ok(screen.getByRole("button", { name: "Search takes 5 s" }));
    assert.ok(checkbox("Search takes 5 s"));
    assert.equal(renderTotal("row:"), 1);
  });

  test("an empty title is not saved: the field says why and keeps the focus", async () => {
    const { store, user } = start();
    await user.click(screen.getByRole("button", { name: "Slow search" }));
    await user.clear(titleInput());
    await user.type(titleInput(), "   ");
    await user.click(screen.getByRole("button", { name: "Save" }));
    assert.equal(store.getState().issues[3]?.title, "Slow search");
    assert.equal(titleInput().getAttribute("aria-invalid"), "true");
    const described = titleInput().getAttribute("aria-describedby") ?? "";
    assert.equal(document.getElementById(described)?.textContent?.trim(), "Title cannot be empty");
    assert.ok(screen.getByRole("alert"));
    expectFocus(titleInput());
  });

  test("when the store reorders the issues, the focus stays on the same issue's checkbox", async () => {
    const { store, user } = start();
    await user.click(checkbox("Typo on the home page"));
    expectFocus(checkbox("Typo on the home page"));
    act(() => store.setState((state) => ({ ...state, issues: [...state.issues].reverse() })));
    assert.deepEqual(titles(), ["Slow search", "Login button is grey", "Typo on the home page", "Login fails on Safari"]);
    expectFocus(checkbox("Typo on the home page"));
  });
});
