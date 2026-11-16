import assert from "node:assert/strict";
import { test } from "node:test";
import { openSite } from "dojo-web";

const site = await openSite();

test("resultLabel() counts results", async () => {
  const page = await site.open("/index.html");
  const labels = await page.evaluate(async () => {
    const { resultLabel } = await import("/app.js");
    return [resultLabel(0), resultLabel(1), resultLabel(3)];
  });
  assert.deepEqual(labels, ["No results", "1 result", "3 results"]);
});
