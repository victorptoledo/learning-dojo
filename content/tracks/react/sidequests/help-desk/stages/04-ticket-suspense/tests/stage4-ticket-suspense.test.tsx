import assert from "node:assert/strict";
import { describe, test } from "node:test";
import { screen, within } from "@testing-library/react";
import { NotFoundError } from "../helpdesk-types.ts";
import { expectAbsent, fakeApi, field, follow, goBack, heading, settle, start } from "./desk-helpers.tsx";

const MESSAGES = {
  t2: [
    { id: "m1", author: "Caio", body: "My password stopped working.", sentAt: "2026-01-01T09:00:00Z" },
    { id: "m2", author: "Support", body: "Did you change it recently?", sentAt: "2026-01-01T09:05:00Z" },
  ],
};

const main = () => screen.getByRole("main");

/** Checks that each item of the Messages list shows its author and its body, in order. */
function expectMessages(expected: { author: string; body: string }[]): void {
  const items = within(screen.getByRole("list", { name: "Messages" })).getAllByRole("listitem");
  assert.equal(items.length, expected.length, "number of messages");
  expected.forEach(({ author, body }, index) => {
    const item = items[index] as HTMLElement;
    assert.ok(within(item).getByText(author), `message ${index + 1} shows its author`);
    assert.ok(within(item).getByText(body), `message ${index + 1} shows its body`);
  });
}

describe("the ticket page", () => {
  test("loads the ticket by its id and says so meanwhile", async () => {
    const { api, ticketCalls, detail } = fakeApi(undefined, { manual: ["ticket"], messages: MESSAGES });
    await start("/tickets/t2", api);
    assert.ok(screen.getByText("Loading ticket"));
    assert.deepEqual(
      ticketCalls.map((call) => call.id),
      ["t2"],
    );
    await ticketCalls[0]?.answer(detail("t2")!);
    await settle();
    assert.equal(heading(), "Cannot log in");
    assert.equal(field("Status"), "Pending");
    expectAbsent(screen.queryByText("Loading ticket"));
  });

  test("shows the messages in order, with their author", async () => {
    const { api } = fakeApi(undefined, { messages: MESSAGES });
    await start("/tickets/t2", api);
    assert.ok(screen.getByRole("heading", { level: 2, name: "Messages" }));
    expectMessages(MESSAGES.t2);
  });

  test("a ticket without messages says so", async () => {
    const { api } = fakeApi();
    await start("/tickets/t3", api);
    assert.ok(screen.getByText("No messages yet"));
    expectAbsent(screen.queryByRole("list", { name: "Messages" }));
  });

  test("does not wait for the whole list", async () => {
    const { api, ticketCalls, detail } = fakeApi(undefined, { manual: ["list", "ticket"] });
    await start("/tickets/t4", api);
    await ticketCalls[0]?.answer(detail("t4")!);
    await settle();
    assert.equal(heading(), "New laptop");
  });

  test("a ticket the API does not know is not found", async () => {
    const { api, ticketCalls } = fakeApi(undefined, { manual: ["ticket"] });
    await start("/tickets/t9", api);
    await ticketCalls[0]?.fail(new NotFoundError("no t9"));
    await settle();
    assert.equal(heading(), "Ticket not found");
    expectAbsent(screen.queryByRole("alert"));
  });

  test("any other failure can be tried again", async () => {
    const original = console.error;
    console.error = () => {};
    try {
      const { api, ticketCalls, detail } = fakeApi(undefined, { manual: ["ticket"] });
      const user = await start("/tickets/t1", api);
      await ticketCalls[0]?.fail();
      await settle();
      const alert = screen.getByRole("alert");
      assert.ok(within(alert).getByText("Could not load this ticket."));
      await user.click(within(alert).getByRole("button", { name: "Try again" }));
      await settle();
      assert.equal(ticketCalls.length, 2, "Try again asks again");
      assert.ok(screen.getByText("Loading ticket"));
      await ticketCalls[1]?.answer(detail("t1")!);
      await settle();
      assert.equal(heading(), "Printer on fire");
    } finally {
      console.error = original;
    }
  });
});

describe("moving between pages", () => {
  test("the list stays on screen, busy, until the ticket is ready", async () => {
    const { api, ticketCalls, detail } = fakeApi(undefined, { manual: ["ticket"] });
    const user = await start("/tickets", api);
    assert.notEqual(main().getAttribute("aria-busy"), "true");
    await follow(user, "VPN is slow");
    assert.equal(heading(), "Tickets", "no loading flash: the old page stays");
    assert.equal(main().getAttribute("aria-busy"), "true");
    expectAbsent(screen.queryByText("Loading ticket"));
    await ticketCalls[0]?.answer(detail("t3")!);
    await settle();
    assert.equal(heading(), "VPN is slow");
    assert.notEqual(main().getAttribute("aria-busy"), "true");
  });

  test("a ticket seen before shows at once, without asking again", async () => {
    const { api, fetchedIds } = fakeApi();
    const user = await start("/tickets", api);
    await follow(user, "New laptop");
    await goBack();
    await follow(user, "New laptop");
    assert.equal(heading(), "New laptop");
    assert.deepEqual(fetchedIds(), ["t4"]);
  });

  test("pages that need nothing new change at once", async () => {
    const { api } = fakeApi(undefined, { manual: ["ticket"] });
    const user = await start("/", api);
    await follow(user, "Tickets", screen.getByRole("navigation", { name: "Main" }));
    assert.equal(heading(), "Tickets");
    assert.notEqual(main().getAttribute("aria-busy"), "true");
  });
});
