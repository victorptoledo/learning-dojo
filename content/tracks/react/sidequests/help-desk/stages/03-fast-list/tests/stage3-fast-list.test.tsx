import assert from "node:assert/strict";
import { beforeEach, describe, test } from "node:test";
import { screen, within } from "@testing-library/react";
import { renderCount, renderTotal, resetRenders } from "../renderLog.ts";
import type { Status, Ticket } from "../helpdesk-types.ts";
import { expectAbsent, expectFocus, fakeApi, follow, heading, listedTitles, path, settle, start, ticket, type User } from "./desk-helpers.tsx";

const PEOPLE = ["Ana", "Bia", "Caio", "Dora", "Elza"];
const STATUS_CYCLE: Status[] = ["open", "pending", "closed"];

/** 250 tickets: requester and status cycle, Ticket 250 is the newest. */
function manyTickets(): Ticket[] {
  return Array.from({ length: 250 }, (_, index) =>
    ticket(index + 1, {
      requester: PEOPLE[index % PEOPLE.length] ?? "Ana",
      status: STATUS_CYCLE[index % STATUS_CYCLE.length] ?? "open",
    }),
  );
}

const searchBox = () => screen.getByRole("searchbox", { name: "Search tickets" }) as HTMLInputElement;
const statusButton = (name: string) =>
  within(screen.getByRole("group", { name: "Status" })).getByRole("button", { name });
const counter = () => (screen.getByText(/^Showing \d+ of \d+ tickets$/).textContent ?? "").trim();
const idsShown = () =>
  within(screen.getByRole("list", { name: "Tickets" }))
    .getAllByRole("link")
    .map((link) => link.getAttribute("href")?.replace("/tickets/", "") ?? "");

async function typeSearch(user: User, text: string): Promise<void> {
  await user.type(searchBox(), text);
  await settle();
}

beforeEach(() => resetRenders());

describe("filters", () => {
  test("the search box filters by title or requester, ignoring case", async () => {
    const { api } = fakeApi();
    const user = await start("/tickets", api);
    await typeSearch(user, "VPN");
    assert.deepEqual(listedTitles(), ["VPN is slow"]);
    await user.clear(searchBox());
    await typeSearch(user, "caio");
    assert.deepEqual(listedTitles(), ["Cannot log in"]);
  });

  test("the search goes to the URL, replacing the entry and keeping the focus", async () => {
    const { api } = fakeApi();
    const user = await start("/tickets", api);
    const entries = window.history.length;
    await typeSearch(user, "log in");
    assert.equal(new URLSearchParams(window.location.search).get("q"), "log in");
    assert.equal(window.history.length, entries);
    expectFocus(searchBox());
    assert.equal(searchBox().value, "log in");
  });

  test("the status buttons say which one is on and filter", async () => {
    const { api } = fakeApi();
    const user = await start("/tickets", api);
    assert.equal(statusButton("All").getAttribute("aria-pressed"), "true");
    await user.click(statusButton("Open"));
    await settle();
    assert.equal(statusButton("Open").getAttribute("aria-pressed"), "true");
    assert.equal(statusButton("All").getAttribute("aria-pressed"), "false");
    assert.deepEqual(listedTitles(), ["New laptop", "VPN is slow"]);
    assert.equal(path(), "/tickets?status=open");
    await user.click(statusButton("All"));
    await settle();
    assert.equal(path(), "/tickets");
    assert.equal(listedTitles().length, 4);
  });

  test("both filters together, read back from a link", async () => {
    const { api } = fakeApi();
    await start("/tickets?q=a&status=pending", api);
    assert.equal(searchBox().value, "a");
    assert.equal(statusButton("Pending").getAttribute("aria-pressed"), "true");
    assert.deepEqual(listedTitles(), ["Cannot log in"]);
  });

  test("a status the app does not know means all", async () => {
    const { api } = fakeApi();
    await start("/tickets?status=lost", api);
    assert.equal(statusButton("All").getAttribute("aria-pressed"), "true");
    assert.equal(listedTitles().length, 4);
  });

  test("no match says so, without an empty list", async () => {
    const { api } = fakeApi();
    const user = await start("/tickets", api);
    await typeSearch(user, "zzz");
    assert.ok(screen.getByText("No tickets match"));
    expectAbsent(screen.queryByRole("list", { name: "Tickets" }));
  });

  test("a ticket opened from a filtered list comes back to the filtered list", async () => {
    const { api } = fakeApi();
    const user = await start("/tickets?status=open", api);
    await follow(user, "VPN is slow");
    assert.equal(heading(), "VPN is slow");
    window.history.back();
    await settle();
    await settle();
    assert.equal(path(), "/tickets?status=open");
    assert.deepEqual(listedTitles(), ["New laptop", "VPN is slow"]);
  });
});

describe("a long list", () => {
  test("shows the first 100 tickets, newest first, and says how many there are", async () => {
    const { api } = fakeApi(manyTickets());
    await start("/tickets", api);
    assert.equal(counter(), "Showing 100 of 250 tickets");
    const ids = idsShown();
    assert.equal(ids.length, 100);
    assert.equal(ids[0], "t250");
    assert.equal(ids[99], "t151");
  });

  test("Show more adds 100 at a time, until there are no more", async () => {
    const { api } = fakeApi(manyTickets());
    const user = await start("/tickets", api);
    await user.click(screen.getByRole("button", { name: "Show more" }));
    assert.equal(counter(), "Showing 200 of 250 tickets");
    await user.click(screen.getByRole("button", { name: "Show more" }));
    assert.equal(counter(), "Showing 250 of 250 tickets");
    expectAbsent(screen.queryByRole("button", { name: "Show more" }));
  });

  test("changing a filter starts again from the first 100", async () => {
    const { api } = fakeApi(manyTickets());
    const user = await start("/tickets", api);
    await user.click(screen.getByRole("button", { name: "Show more" }));
    await user.click(statusButton("Open"));
    await settle();
    assert.equal(counter(), "Showing 84 of 84 tickets");
    await user.click(statusButton("All"));
    await settle();
    assert.equal(counter(), "Showing 100 of 250 tickets");
  });
});

describe("render budget (trackRender in every ticket row)", () => {
  test("each row renders once when the list appears", async () => {
    const { api } = fakeApi(manyTickets());
    await start("/tickets", api);
    assert.equal(renderTotal("row:"), 100);
    assert.equal(renderCount("row:t250"), 1);
    assert.equal(renderCount("row:t150"), 0, "rows past the first 100 are not rendered");
  });

  test("typing renders no row that stays on screen", async () => {
    const { api } = fakeApi(manyTickets());
    const user = await start("/tickets", api);
    const before = new Set(idsShown());
    resetRenders();
    await typeSearch(user, "a");
    assert.equal(renderTotal("row:"), 0, "every row matches 'a': nothing should render");
    await typeSearch(user, "n");
    const after = idsShown();
    assert.ok(after.length > 0);
    for (const id of after) {
      const expected = before.has(id) ? 0 : 1;
      assert.equal(renderCount(`row:${id}`), expected, `${id} was ${before.has(id) ? "already" : "not"} shown`);
    }
  });

  test("a status filter renders no row that stays on screen", async () => {
    const { api } = fakeApi(manyTickets());
    const user = await start("/tickets?status=open", api);
    resetRenders();
    await typeSearch(user, "Ticket");
    assert.equal(renderTotal("row:"), 0);
  });

  test("Show more renders only the rows it adds", async () => {
    const { api } = fakeApi(manyTickets());
    const user = await start("/tickets", api);
    resetRenders();
    await user.click(screen.getByRole("button", { name: "Show more" }));
    assert.equal(renderTotal("row:"), 100);
    assert.equal(renderCount("row:t250"), 0);
  });
});
