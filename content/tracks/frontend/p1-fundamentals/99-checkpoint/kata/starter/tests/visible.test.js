import assert from "node:assert/strict";
import { test } from "node:test";
import { openSite } from "dojo-web";

const site = await openSite();

test("the product heading is the page's h1", async () => {
  const page = await site.open("/index.html");
  assert.equal(await page.locator("main article.product h1").count(), 1, 'an <h1> inside <article class="product">');
  assert.equal(await page.locator("h1").innerText(), "Trail Jacket");
});
