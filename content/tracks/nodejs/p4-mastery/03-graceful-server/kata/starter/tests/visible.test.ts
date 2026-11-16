import assert from "node:assert/strict";
import { test } from "node:test";
import { createLogger, requestContext } from "../service.ts";

test("log lines carry the request id inside a request", () => {
  const lines: string[] = [];
  const logger = createLogger((line) => lines.push(line), () => new Date("2026-03-01T12:00:00.000Z"));
  requestContext.run({ requestId: "r-1" }, () => logger.info("hello"));
  assert.deepEqual(JSON.parse(lines[0] ?? "{}"), {
    time: "2026-03-01T12:00:00.000Z",
    level: "info",
    msg: "hello",
    requestId: "r-1",
  });
});
