import assert from "node:assert/strict";
import { test } from "node:test";
import { openSite } from "dojo-web";

const site = await openSite({ routes: { "POST /api/errors": () => ({ status: 204 }) } });

test("formatPrice() formats euro cents for Ireland", async () => {
  const page = await site.open("/index.html");
  const out = await page.evaluate(async () => {
    const { formatPrice } = await import("/app.js");
    return formatPrice(5400, "EUR", "en-IE");
  });
  assert.equal(out, "€54.00");
});
