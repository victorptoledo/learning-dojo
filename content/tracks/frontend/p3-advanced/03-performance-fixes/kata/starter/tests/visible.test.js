import assert from "node:assert/strict";
import { test } from "node:test";
import { openSite } from "dojo-web";

const site = await openSite();

test("the hero image is not lazy-loaded", async () => {
  const page = await site.open("/index.html");
  assert.notEqual(await page.locator("img.hero").getAttribute("loading"), "lazy", "the hero is the LCP element");
});
