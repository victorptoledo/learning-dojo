import assert from "node:assert/strict";
import { test } from "node:test";
import { openSite } from "dojo-web";

const site = await openSite();

test("an empty name gets the 'Enter your name.' message", async () => {
  const page = await site.open("/index.html");
  const message = await page.evaluate(async () => {
    const { errorFor } = await import("/app.js");
    return errorFor(document.querySelector("#name"));
  });
  assert.equal(message, "Enter your name.");
});
