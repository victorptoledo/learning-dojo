import assert from "node:assert/strict";
import { test } from "node:test";
import { parseConfig } from "../config.ts";

test("fills in the defaults", () => {
  assert.deepEqual(
    parseConfig({ server: { port: 8080 }, database: { url: "postgres://db.local/app" } }),
    {
      ok: true,
      value: {
        server: { host: "0.0.0.0", port: 8080 },
        database: { url: "postgres://db.local/app", poolSize: 10 },
        logLevel: "info",
        features: [],
      },
    },
  );
});
