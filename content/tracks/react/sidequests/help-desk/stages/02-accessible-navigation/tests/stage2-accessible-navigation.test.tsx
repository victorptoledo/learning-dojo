import assert from "node:assert/strict";
import { describe, test } from "node:test";
import { screen, within } from "@testing-library/react";
import { expectFocus, fakeApi, follow, goBack, heading, nav, start } from "./desk-helpers.tsx";

const h1 = () => screen.getByRole("heading", { level: 1 });
const announced = () => (screen.getByRole("status").textContent ?? "").trim();

describe("the browser tab", () => {
  test("is named after the page", async () => {
    const { api } = fakeApi();
    const user = await start("/", api);
    assert.equal(document.title, "Overview · Help desk");
    await follow(user, "Tickets", nav());
    assert.equal(document.title, "Tickets · Help desk");
    await follow(user, "Cannot log in");
    assert.equal(document.title, "Cannot log in · Help desk");
  });

  test("also on a ticket that is not found", async () => {
    const { api } = fakeApi();
    await start("/tickets/t42", api);
    assert.equal(document.title, "Ticket not found · Help desk");
  });

  test("and on a page that is not found", async () => {
    const { api } = fakeApi();
    await start("/oops", api);
    assert.equal(document.title, "Page not found · Help desk");
  });
});

describe("focus and announcements", () => {
  test("opening the app leaves the focus alone and announces nothing", async () => {
    const { api } = fakeApi();
    await start("/tickets", api);
    expectFocus(document.body);
    assert.equal(announced(), "");
  });

  test("after following a link the focus is on the new page's heading", async () => {
    const { api } = fakeApi();
    const user = await start("/", api);
    await follow(user, "Tickets", nav());
    expectFocus(h1(), "on Tickets");
    await follow(user, "VPN is slow");
    assert.equal(heading(), "VPN is slow");
    expectFocus(h1(), "on the ticket");
  });

  test("the new page is announced", async () => {
    const { api } = fakeApi();
    const user = await start("/", api);
    await follow(user, "Tickets", nav());
    assert.equal(announced(), "Navigated to Tickets");
    await follow(user, "New laptop");
    assert.equal(announced(), "Navigated to New laptop");
  });

  test("Back moves the focus and announces too", async () => {
    const { api } = fakeApi();
    const user = await start("/", api);
    await follow(user, "Tickets", nav());
    await goBack();
    assert.equal(heading(), "Overview");
    expectFocus(h1());
    assert.equal(announced(), "Navigated to Overview");
  });

  test("the headings can take the focus without joining the tab order", async () => {
    const { api } = fakeApi();
    await start("/", api);
    assert.equal(h1().getAttribute("tabindex"), "-1");
  });
});

describe("skip link", () => {
  test("is the first link of the page and jumps to the main content", async () => {
    const { api } = fakeApi();
    const user = await start("/tickets", api);
    const first = screen.getAllByRole("link")[0];
    assert.ok(first);
    assert.equal(first.textContent?.trim(), "Skip to content");
    assert.equal(first.getAttribute("href"), "#main");
    await user.click(first);
    const main = screen.getByRole("main");
    assert.equal(main.id, "main");
    expectFocus(main);
    assert.ok(within(main).getByRole("heading", { level: 1, name: "Tickets" }));
  });

  test("is reached first with Tab", async () => {
    const { api } = fakeApi();
    const user = await start("/", api);
    await user.tab();
    expectFocus(screen.getByRole("link", { name: "Skip to content" }));
  });
});
