import assert from "node:assert/strict";
import { test } from "node:test";
import { openSite } from "dojo-web";

const site = await openSite();

test("on a desktop the cards form three columns", async () => {
  const page = await site.open("/index.html", { width: 1280, height: 900 });
  const xs = await page.$$eval(".card", (all) => all.map((card) => Math.round(card.getBoundingClientRect().x)));
  assert.equal(new Set(xs).size, 3, `the cards start at x = ${[...new Set(xs)].join(", ")}`);
});
