import assert from "node:assert/strict";
import { test } from "node:test";
import { openSite, assertAccessible, assertNoPageErrors, assertValidHtml, box, settle } from "dojo-web";

const site = await openSite();

test("the html element says the page is in English", async () => {
  const page = await site.open("/index.html");
  assert.equal(await page.locator("html").getAttribute("lang"), "en");
});

test("the HTML is valid", async () => {
  await assertValidHtml("index.html");
});

test("the card is an article inside main, with one h1", async () => {
  const page = await site.open("/index.html");
  assert.equal(await page.locator("main article.card").count(), 1, "main > article.card");
  assert.equal(await page.locator("h1").count(), 1, "exactly one <h1>");
  assert.equal(await page.locator("article.card h1").count(), 1, "the <h1> is in the card");
  assert.notEqual((await page.locator("h1").innerText()).trim(), "", "the <h1> holds your name");
  assert.equal(await page.locator(".card p.role").count(), 1, 'a <p class="role">');
});

test("the card lists at least two links", async () => {
  const page = await site.open("/index.html");
  const links = page.locator(".card ul > li > a[href]");
  assert.ok((await links.count()) >= 2, "a <ul> with at least two <li><a href>");
});

test("the page has no accessibility problems", async () => {
  const page = await site.open("/index.html");
  await assertAccessible(page);
});

test("the card is at most 24rem (384px) wide and centered", async () => {
  const page = await site.open("/index.html", { width: 1280, height: 800 });
  const card = await box(page, ".card");
  assert.ok(card.width <= 384.5, `the card is ${card.width}px wide`);
  const left = card.x;
  const right = 1280 - (card.x + card.width);
  assert.ok(Math.abs(left - right) <= 2, `left gap ${left}px, right gap ${right}px`);
});

test("the card fits a phone screen", async () => {
  const page = await site.open("/index.html", { width: 320, height: 640 });
  const card = await box(page, ".card");
  assert.ok(card.x >= 0 && card.x + card.width <= 320, "the card stays inside 320px");
  const scroll = await page.evaluate(() => document.documentElement.scrollWidth);
  assert.ok(scroll <= 320, `the page scrolls sideways (${scroll}px wide)`);
});

test("greeting() builds the message", async () => {
  const page = await site.open("/index.html");
  const results = await page.evaluate(async () => {
    const { greeting } = await import("/app.js");
    return [greeting("Ada Lovelace"), greeting("  Grace  "), greeting("   "), greeting(undefined)];
  });
  assert.deepEqual(results, [
    "Hello from Ada Lovelace!",
    "Hello from Grace!",
    "Hello from the dojo!",
    "Hello from the dojo!",
  ]);
});

test("the button is a real button that shows the greeting", async () => {
  const page = await site.open("/index.html");
  const button = page.getByRole("button", { name: "Say hello" });
  assert.equal(await button.count(), 1, 'a <button> named "Say hello"');
  assert.equal(await page.locator("#say-hello").evaluate((el) => el.tagName), "BUTTON");
  assert.equal(await page.locator("#say-hello").getAttribute("type"), "button");
  await button.click();
  await settle(page);
  const name = (await page.locator("h1").innerText()).trim();
  assert.equal(await page.locator("output#greeting").innerText(), `Hello from ${name}!`);
});

test("the button also works from the keyboard", async () => {
  const page = await site.open("/index.html");
  await page.locator("#say-hello").focus();
  await page.keyboard.press("Enter");
  await settle(page);
  assert.match(await page.locator("#greeting").innerText(), /^Hello from .+!$/);
  assertNoPageErrors(page);
});
