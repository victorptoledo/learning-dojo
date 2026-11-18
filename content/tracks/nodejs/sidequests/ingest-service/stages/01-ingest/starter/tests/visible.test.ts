// Your own tests: `npm test` runs every tests/*.test.ts. Add more as you go.
import assert from "node:assert/strict";
import type { AddressInfo } from "node:net";
import { test } from "node:test";
import { createService } from "../service.ts";

test("two valid events are accepted and reach the sink after flush", async () => {
  const batches: (readonly string[])[] = [];
  const service = createService({ sink: { write: async (lines) => void batches.push(lines) } });
  await new Promise<void>((resolve) => service.server.listen(0, "127.0.0.1", resolve));
  const { port } = service.server.address() as AddressInfo;
  try {
    const response = await fetch(`http://127.0.0.1:${port}/events`, {
      method: "POST",
      headers: { "content-type": "application/x-ndjson" },
      body: '{"type":"click","time":"2026-03-01T12:00:00Z"}\n{"type":"view","time":"2026-03-01T12:00:01Z"}\n',
    });
    assert.equal(response.status, 202);
    assert.deepEqual(await response.json(), { accepted: 2, rejected: 0, errors: [] });
    await service.flush();
    assert.equal(batches.flat().length, 2);
  } finally {
    service.server.closeAllConnections();
    service.server.close();
  }
});
