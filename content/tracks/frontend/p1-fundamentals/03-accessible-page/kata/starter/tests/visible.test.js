import assert from "node:assert/strict";
import { test } from "node:test";
import { openSite } from "dojo-web";

const site = await openSite();

test("the logo has a text alternative", async () => {
  const page = await site.open("/index.html");
  const alt = await page.locator("img.logo").getAttribute("alt");
  assert.ok(alt && alt.trim() !== "", 'the logo needs alt text, for example alt="Frontend Meetup"');
});
