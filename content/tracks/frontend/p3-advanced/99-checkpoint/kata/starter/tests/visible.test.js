import assert from "node:assert/strict";
import { test } from "node:test";
import { openSite } from "dojo-web";

const site = await openSite({ routes: { "GET /api/reviews": () => ({ json: [] }) } });

test("safeUrl() refuses javascript: URLs", async () => {
  const page = await site.open("/index.html");
  const out = await page.evaluate(async () => {
    const { safeUrl } = await import("/app.js");
    return [safeUrl("javascript:alert(1)"), safeUrl("https://example.com/")];
  });
  assert.deepEqual(out, [null, "https://example.com/"]);
});
