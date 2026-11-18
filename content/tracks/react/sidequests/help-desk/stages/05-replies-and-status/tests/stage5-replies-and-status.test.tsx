import assert from "node:assert/strict";
import { describe, test } from "node:test";
import { screen, within } from "@testing-library/react";
import { choose, expectAbsent, expectFocus, fakeApi, field, follow, heading, nav, press, settle, start, type User } from "./desk-helpers.tsx";

const HISTORY = {
  t2: [{ id: "m1", author: "Caio", body: "My password stopped working.", sentAt: "2026-01-01T09:00:00Z" }],
};

const replyBox = () => screen.getByRole("textbox", { name: "Your reply" }) as HTMLTextAreaElement;
const sendButton = () =>
  within(screen.getByRole("form", { name: "Reply" })).getByRole("button", { name: "Send reply" }) as HTMLButtonElement;
const statusSelect = () => screen.getByRole("combobox", { name: "Change status" }) as HTMLSelectElement;

/** Each message as "author: body", plus " (sending)" while it is on its way. */
function messages(): string[] {
  const list = screen.queryByRole("list", { name: "Messages" });
  if (!list) {
    return [];
  }
  return within(list)
    .getAllByRole("listitem")
    .map((item) => {
      const author = within(item).getByText(/^(Caio|You|Agent Rita)$/).textContent;
      const body = within(item).queryByText(/stopped working|Try|restart/)?.textContent ?? "?";
      return `${author}: ${body}${within(item).queryByText("Sending…") ? " (sending)" : ""}`;
    });
}

async function reply(user: User, text: string): Promise<void> {
  await user.clear(replyBox());
  if (text !== "") {
    await user.type(replyBox(), text);
  }
  await press(user, sendButton());
}

const RITA = { id: "m2", author: "Agent Rita", body: "Try a restart, please.", sentAt: "2026-01-01T10:00:00Z" };

describe("replies", () => {
  test("a reply form under the messages", async () => {
    const { api } = fakeApi(undefined, { messages: HISTORY });
    await start("/tickets/t2", api);
    assert.equal(replyBox().value, "");
    assert.equal(sendButton().disabled, false);
  });

  test("an empty reply is not sent and says why", async () => {
    const { api, replyCalls } = fakeApi(undefined, { messages: HISTORY });
    const user = await start("/tickets/t2", api);
    await reply(user, "   ");
    assert.equal(replyCalls.length, 0);
    assert.equal(replyBox().getAttribute("aria-invalid"), "true");
    const described = replyBox().getAttribute("aria-describedby") ?? "";
    assert.equal(document.getElementById(described)?.textContent?.trim(), "Write a reply first");
    expectFocus(replyBox());
  });

  test("a reply shows at once, marked as sending, and the box is emptied", async () => {
    const { api, replyCalls } = fakeApi(undefined, { messages: HISTORY });
    const user = await start("/tickets/t2", api);
    await reply(user, "  Try a restart, please. ");
    assert.equal(replyCalls.length, 1);
    assert.equal(replyCalls[0]?.id, "t2");
    assert.equal(replyCalls[0]?.body, "Try a restart, please.");
    assert.deepEqual(messages(), ["Caio: My password stopped working.", "You: Try a restart, please. (sending)"]);
    assert.equal(replyBox().value, "");
    assert.equal(sendButton().disabled, true, "one reply at a time");
  });

  test("once stored, the message is the one the server answered", async () => {
    const { api, replyCalls } = fakeApi(undefined, { messages: HISTORY });
    const user = await start("/tickets/t2", api);
    await reply(user, "Try a restart, please.");
    await replyCalls[0]?.answer(RITA);
    await settle();
    assert.deepEqual(messages(), ["Caio: My password stopped working.", "Agent Rita: Try a restart, please."]);
    assert.equal(sendButton().disabled, false);
    expectAbsent(screen.queryByRole("alert"));
  });

  test("a reply that fails goes away, says so and gives the text back", async () => {
    const { api, replyCalls } = fakeApi(undefined, { messages: HISTORY });
    const user = await start("/tickets/t2", api);
    await reply(user, "Try a restart, please.");
    await replyCalls[0]?.fail();
    await settle();
    assert.deepEqual(messages(), ["Caio: My password stopped working."]);
    assert.ok(within(screen.getByRole("alert")).getByText("Your reply was not sent."));
    assert.equal(replyBox().value, "Try a restart, please.");
    assert.equal(sendButton().disabled, false);
  });

  test("a stored reply is still there when the ticket is opened again", async () => {
    const { api, replyCalls, fetchedIds } = fakeApi(undefined, { messages: HISTORY });
    const user = await start("/tickets/t2", api);
    await reply(user, "Try a restart, please.");
    await replyCalls[0]?.answer(RITA);
    await settle();
    await follow(user, "All tickets");
    await follow(user, "Cannot log in");
    assert.equal(heading(), "Cannot log in");
    assert.deepEqual(messages(), ["Caio: My password stopped working.", "Agent Rita: Try a restart, please."]);
    assert.deepEqual(fetchedIds(), ["t2"]);
  });
});

describe("status", () => {
  test("the select shows the ticket's status", async () => {
    const { api } = fakeApi();
    await start("/tickets/t2", api);
    assert.equal(statusSelect().selectedOptions[0]?.textContent, "Pending");
  });

  test("a new status shows everywhere at once, before the server answers", async () => {
    const { api, statusCalls } = fakeApi();
    const user = await start("/tickets/t3", api);
    await choose(user, statusSelect(), "Closed");
    assert.equal(field("Status"), "Closed");
    assert.deepEqual(
      statusCalls.map((call) => [call.id, call.status]),
      [["t3", "closed"]],
    );
    await follow(user, "Overview", nav());
    assert.ok(screen.getByText("Open: 1"));
    assert.ok(screen.getByText("Closed: 2"));
    await follow(user, "Tickets", nav());
    const list = screen.getByRole("list", { name: "Tickets" });
    const row = within(list).getByRole("link", { name: "VPN is slow" }).closest("li");
    assert.match(row?.textContent ?? "", /Closed · High · Ana/);
  });

  test("a status the server refuses goes back everywhere and says so", async () => {
    const { api, statusCalls } = fakeApi();
    const user = await start("/tickets/t3", api);
    await choose(user, statusSelect(), "Pending");
    await statusCalls[0]?.fail();
    await settle();
    assert.equal(field("Status"), "Open");
    assert.equal(statusSelect().selectedOptions[0]?.textContent, "Open");
    assert.ok(within(screen.getByRole("alert")).getByText("Could not change the status."));
    await follow(user, "Overview", nav());
    assert.ok(screen.getByText("Open: 2"));
    assert.ok(screen.getByText("Pending: 1"));
  });

  test("a stored status is kept when the ticket is opened again", async () => {
    const { api, statusCalls, fetchedIds } = fakeApi();
    const user = await start("/tickets/t4", api);
    await choose(user, statusSelect(), "Closed");
    await statusCalls[0]?.answer({ ...(await api.listTickets()).find((one) => one.id === "t4")!, status: "closed" });
    await settle();
    await follow(user, "All tickets");
    await follow(user, "New laptop");
    assert.equal(field("Status"), "Closed");
    assert.deepEqual(fetchedIds(), ["t4"]);
  });
});
