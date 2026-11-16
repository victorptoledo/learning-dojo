import assert from "node:assert/strict";
import type { AddressInfo } from "node:net";
import { test, type TestContext } from "node:test";
import { renderEntries } from "../client.ts";
import { createGuestbookServer } from "../server.ts";

/** Starts a fresh guestbook on a free port and returns its base URL; it stops after the test. */
async function start(t: TestContext): Promise<string> {
  const server = createGuestbookServer();
  await new Promise<void>((resolve) => server.listen(0, "127.0.0.1", resolve));
  t.after(() => server.close());
  return `http://127.0.0.1:${(server.address() as AddressInfo).port}`;
}

test("renders an entry as a list item", () => {
  assert.equal(
    renderEntries([{ id: 1, name: "Ada", message: "Hello" }]),
    "<li><strong>Ada</strong>: Hello</li>",
  );
});

test("a new guestbook has no entries", async (t) => {
  const base = await start(t);
  const response = await fetch(`${base}/api/entries`);
  assert.equal(response.status, 200);
  assert.deepEqual(await response.json(), { entries: [] });
});
