import assert from "node:assert/strict";
import { test } from "node:test";
import { openSite, settle } from "dojo-web";

const site = await openSite();

test("adding a task puts it in the to-do list", async () => {
  const page = await site.open("/index.html");
  await page.getByLabel("New task").fill("Buy rice");
  await page.getByRole("button", { name: "Add" }).click();
  await settle(page);
  assert.deepEqual(await page.locator("#todo .task-title").allInnerTexts(), ["Buy rice"]);
});
