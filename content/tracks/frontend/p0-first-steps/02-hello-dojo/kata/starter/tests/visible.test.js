import assert from "node:assert/strict";
import { test } from "node:test";
import { openSite } from "dojo-web";

const site = await openSite();

test("the page has a title", async () => {
  const page = await site.open("/index.html");
  assert.notEqual((await page.title()).trim(), "", "add a <title> inside <head>");
});
