import assert from "node:assert/strict";
import { describe, test } from "node:test";
import { screen, within } from "@testing-library/react";
import {
  expectAbsent,
  fakeApi,
  field,
  follow,
  goBack,
  heading,
  listedTitles,
  nav,
  path,
  start,
} from "./desk-helpers.tsx";

const navLink = (name: string) => within(nav()).getByRole("link", { name });
const current = () =>
  within(nav())
    .getAllByRole("link")
    .filter((link) => link.getAttribute("aria-current") === "page")
    .map((link) => link.textContent?.trim());

describe("pages", () => {
  test("/ is the overview, with the tickets counted by status", async () => {
    const { api } = fakeApi();
    await start("/", api);
    assert.equal(heading(), "Overview");
    const counts = within(screen.getByRole("list", { name: "Tickets by status" }))
      .getAllByRole("listitem")
      .map((item) => item.textContent?.trim());
    assert.deepEqual(counts, ["Open: 2", "Pending: 1", "Closed: 1"]);
  });

  test("the list is loading, then counted", async () => {
    const { api, listCalls } = fakeApi(undefined, { manual: ["list"] });
    await start("/", api);
    assert.equal(heading(), "Overview");
    assert.ok(screen.getByText("Loading tickets"));
    await listCalls[0]?.answer([]);
    assert.ok(screen.getByText("Open: 0"));
    expectAbsent(screen.queryByText("Loading tickets"));
  });

  test("a list that fails to load can be tried again", async () => {
    const { api, listCalls } = fakeApi(undefined, { manual: ["list"] });
    const user = await start("/tickets", api);
    await listCalls[0]?.fail();
    const alert = screen.getByRole("alert");
    assert.ok(within(alert).getByText("Could not load the tickets."));
    await user.click(within(alert).getByRole("button", { name: "Try again" }));
    assert.equal(listCalls.length, 2);
    await listCalls[1]?.answer([]);
    expectAbsent(screen.queryByRole("alert"));
  });

  test("/tickets lists every ticket, newest first, as links to their page", async () => {
    const { api } = fakeApi();
    await start("/tickets", api);
    assert.equal(heading(), "Tickets");
    assert.deepEqual(listedTitles(), ["New laptop", "VPN is slow", "Cannot log in", "Printer on fire"]);
    const list = screen.getByRole("list", { name: "Tickets" });
    assert.equal(within(list).getByRole("link", { name: "Cannot log in" }).getAttribute("href"), "/tickets/t2");
    assert.ok(within(list).getByText("Closed · High · Bia"));
    assert.ok(within(list).getByText("Pending · Normal · Caio"));
  });

  test("/tickets/<id> shows the ticket", async () => {
    const { api } = fakeApi();
    await start("/tickets/t2", api);
    assert.equal(heading(), "Cannot log in");
    assert.equal(field("Status"), "Pending");
    assert.equal(field("Priority"), "Normal");
    assert.equal(field("Requester"), "Caio");
    assert.equal(screen.getByRole("link", { name: "All tickets" }).getAttribute("href"), "/tickets");
  });

  test("an unknown ticket has its own page", async () => {
    const { api } = fakeApi();
    const user = await start("/tickets/t99", api);
    assert.equal(heading(), "Ticket not found");
    await follow(user, "All tickets");
    assert.equal(heading(), "Tickets");
  });

  test("any other path is a page not found with a way back", async () => {
    const { api } = fakeApi();
    const user = await start("/settings/profile", api);
    assert.equal(heading(), "Page not found");
    await follow(user, "Go to the overview");
    assert.equal(path(), "/");
    assert.equal(heading(), "Overview");
  });
});

describe("navigation", () => {
  test("the main navigation links to the overview and the tickets", async () => {
    const { api } = fakeApi();
    await start("/", api);
    assert.equal(navLink("Overview").getAttribute("href"), "/");
    assert.equal(navLink("Tickets").getAttribute("href"), "/tickets");
  });

  test("the link of the current section is marked", async () => {
    const { api } = fakeApi();
    const user = await start("/", api);
    assert.deepEqual(current(), ["Overview"]);
    await follow(user, "Tickets", nav());
    assert.deepEqual(current(), ["Tickets"]);
    await follow(user, "VPN is slow");
    assert.deepEqual(current(), ["Tickets"], "a ticket is in the Tickets section");
  });

  test("no section is marked on a page not found", async () => {
    const { api } = fakeApi();
    await start("/nowhere", api);
    assert.deepEqual(current(), []);
  });

  test("links change the page and the address without reloading", async () => {
    const { api, listedTimes } = fakeApi();
    const user = await start("/", api);
    await follow(user, "Tickets", nav());
    assert.equal(path(), "/tickets");
    assert.equal(heading(), "Tickets");
    await follow(user, "Printer on fire");
    assert.equal(path(), "/tickets/t1");
    assert.equal(heading(), "Printer on fire");
    await follow(user, "Overview", nav());
    assert.equal(heading(), "Overview");
    assert.equal(listedTimes(), 1, "the list is loaded once for every page");
  });

  test("Back and Forward bring the pages back", async () => {
    const { api } = fakeApi();
    const user = await start("/", api);
    await follow(user, "Tickets", nav());
    await follow(user, "New laptop");
    await goBack();
    assert.equal(path(), "/tickets");
    assert.equal(heading(), "Tickets");
    await goBack();
    assert.equal(heading(), "Overview");
  });

  test("a click with Ctrl, Shift or Meta is left to the browser (a new tab or window)", async () => {
    const { api } = fakeApi();
    const user = await start("/", api);
    for (const key of ["Control", "Shift", "Meta"]) {
      await user.keyboard(`{${key}>}`);
      await user.click(navLink("Tickets"));
      await user.keyboard(`{/${key}}`);
      assert.equal(path(), "/", key);
      assert.equal(heading(), "Overview", key);
    }
  });
});
