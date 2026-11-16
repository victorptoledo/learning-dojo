import assert from "node:assert/strict";
import { test } from "node:test";
import { openSite, assertAccessible, assertNoPageErrors, settle } from "dojo-web";

const site = await openSite();

async function add(page, title) {
  await page.getByLabel("New task").fill(title);
  await page.getByLabel("New task").press("Enter");
  await settle(page);
}

const titles = (page, list) => page.locator(`#${list} .task-title`).allInnerTexts();

test("countLabel() counts tasks in English", async () => {
  const page = await site.open("/index.html");
  const labels = await page.evaluate(async () => {
    const { countLabel } = await import("/app.js");
    return [0, 1, 2, 11].map(countLabel);
  });
  assert.deepEqual(labels, ["0 tasks left", "1 task left", "2 tasks left", "11 tasks left"]);
});

test("taskElement() builds the item with text, not HTML", async () => {
  const page = await site.open("/index.html");
  const item = await page.evaluate(async () => {
    const { taskElement } = await import("/app.js");
    const li = taskElement({ id: 7, title: '<img src=x onerror="window.hacked=1">', done: false });
    return {
      tag: li.tagName,
      className: li.className,
      id: li.dataset.id,
      title: li.querySelector(".task-title")?.textContent,
      images: li.querySelectorAll("img").length,
      buttons: [...li.querySelectorAll("button")].map((b) => [b.type, b.dataset.action, b.textContent]),
    };
  });
  assert.equal(item.tag, "LI");
  assert.equal(item.className, "task");
  assert.equal(item.id, "7");
  assert.equal(item.title, '<img src=x onerror="window.hacked=1">', "the title is shown as typed");
  assert.equal(item.images, 0, "the title must not become HTML");
  assert.deepEqual(item.buttons, [
    ["button", "toggle", "Done"],
    ["button", "delete", "Delete"],
  ]);
});

test("pressing Enter adds a trimmed task, clears the field and keeps focus there", async () => {
  const page = await site.open("/index.html");
  await add(page, "  Water the plants  ");
  await add(page, "Call Grace");
  assert.deepEqual(await titles(page, "todo"), ["Water the plants", "Call Grace"]);
  assert.equal(await page.getByLabel("New task").inputValue(), "");
  assert.equal(await page.evaluate(() => document.activeElement?.id), "task-title");
  assert.equal(await page.locator("#count").innerText(), "2 tasks left");
});

test("an empty or blank title adds nothing and explains why", async () => {
  const page = await site.open("/index.html");
  await add(page, "");
  await add(page, "    ");
  assert.equal(await page.locator(".task").count(), 0);
  const message = await page.getByLabel("New task").evaluate((input) => input.validationMessage);
  assert.notEqual(message, "", "the field reports a validation message");
  await add(page, "Real task");
  assert.deepEqual(await titles(page, "todo"), ["Real task"], "typing clears the custom error");
  assertNoPageErrors(page);
});

test("the buttons have names that say which task they act on", async () => {
  const page = await site.open("/index.html");
  await add(page, "Buy rice");
  await add(page, "Buy tea");
  assert.equal(await page.getByRole("button", { name: "Done: Buy tea" }).count(), 1);
  assert.equal(await page.getByRole("button", { name: "Delete Buy rice" }).count(), 1);
});

test("Done moves a task to the done list and Undo moves it back", async () => {
  const page = await site.open("/index.html");
  await add(page, "Buy rice");
  await add(page, "Buy tea");
  await page.getByRole("button", { name: "Done: Buy rice" }).click();
  await settle(page);
  assert.deepEqual(await titles(page, "todo"), ["Buy tea"]);
  assert.deepEqual(await titles(page, "done"), ["Buy rice"]);
  assert.equal(await page.locator("#count").innerText(), "1 task left");
  const undo = page.getByRole("button", { name: "Undo: Buy rice" });
  assert.equal(await undo.innerText(), "Undo");
  await undo.click();
  await settle(page);
  assert.deepEqual((await titles(page, "todo")).sort(), ["Buy rice", "Buy tea"]);
  assert.equal(await page.locator("#done .task").count(), 0);
  assert.equal(await page.locator("#count").innerText(), "2 tasks left");
});

test("Delete removes the task and sends focus back to the field", async () => {
  const page = await site.open("/index.html");
  await add(page, "Buy rice");
  await add(page, "Buy tea");
  await page.getByRole("button", { name: "Delete Buy rice" }).click();
  await settle(page);
  assert.deepEqual(await titles(page, "todo"), ["Buy tea"]);
  assert.equal(await page.locator("#count").innerText(), "1 task left");
  assert.equal(await page.evaluate(() => document.activeElement?.id), "task-title", "focus does not get lost on <body>");
});

test("one listener on the board handles tasks it has never seen (delegation)", async () => {
  const page = await site.open("/index.html");
  await page.evaluate(async () => {
    const { taskElement } = await import("/app.js");
    document.querySelector("#todo").append(taskElement({ id: 99, title: "Injected", done: false }));
  });
  await page.getByRole("button", { name: "Done: Injected" }).click();
  await settle(page);
  assert.deepEqual(await titles(page, "done"), ["Injected"], "the board's listener found the new task");
});

test("listeners are not attached to each task", async () => {
  const page = await site.open("/index.html");
  await add(page, "Buy rice");
  const client = await page.context().newCDPSession(page);
  const { result } = await client.send("Runtime.evaluate", { expression: 'document.querySelector("#todo .task button")' });
  const { listeners } = await client.send("DOMDebugger.getEventListeners", { objectId: result.objectId });
  assert.equal(listeners.length, 0, "the task's button has its own listener: listen on the board instead");
});

test("clicks outside the task buttons are ignored", async () => {
  const page = await site.open("/index.html");
  await add(page, "Buy rice");
  await page.locator("#todo .task-title").click();
  await page.locator("h1").click();
  await settle(page);
  assert.deepEqual(await titles(page, "todo"), ["Buy rice"]);
});

test("everything works from the keyboard", async () => {
  const page = await site.open("/index.html");
  await add(page, "Buy rice");
  await page.keyboard.press("Tab"); // Add button
  await page.keyboard.press("Tab"); // the task's Done button
  await page.keyboard.press("Enter");
  await settle(page);
  assert.deepEqual(await titles(page, "done"), ["Buy rice"]);
  assert.equal(
    await page.evaluate(() => document.activeElement?.getAttribute("aria-label")),
    "Undo: Buy rice",
    "after moving a task, focus follows it to its new toggle button",
  );
});

test("a title is shown exactly as typed and never runs as HTML", async () => {
  const page = await site.open("/index.html");
  await add(page, '<img src=x onerror="window.hacked=1">');
  await settle(page, 200);
  assert.equal(await page.evaluate(() => window.hacked), undefined);
  assert.deepEqual(await titles(page, "todo"), ['<img src=x onerror="window.hacked=1">']);
});

test("axe-core finds no problems with tasks on the board", async () => {
  const page = await site.open("/index.html");
  await add(page, "Buy rice");
  await add(page, "Buy tea");
  await page.getByRole("button", { name: "Done: Buy tea" }).click();
  await settle(page);
  await assertAccessible(page);
});
