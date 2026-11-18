// Your own tests: `npm test` runs every tests/*.test.ts. Add more as you go.
import assert from "node:assert/strict";
import type { AddressInfo } from "node:net";
import { test } from "node:test";
import { createApp } from "../polls.ts";

test("a new poll answers 201 with a Location", async () => {
  const server = createApp();
  await new Promise<void>((resolve) => server.listen(0, "127.0.0.1", resolve));
  const { port } = server.address() as AddressInfo;
  try {
    const response = await fetch(`http://127.0.0.1:${port}/polls`, {
      method: "POST",
      headers: { "content-type": "application/json" },
      body: JSON.stringify({ question: "Tabs or spaces?", options: ["Tabs", "Spaces"] }),
    });
    assert.equal(response.status, 201);
    assert.match(response.headers.get("location") ?? "", /^\/polls\/.+/);
  } finally {
    server.closeAllConnections();
    server.close();
  }
});
