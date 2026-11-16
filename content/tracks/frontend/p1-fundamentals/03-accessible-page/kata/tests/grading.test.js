import assert from "node:assert/strict";
import { test } from "node:test";
import { openSite, assertAccessible, assertNoPageErrors, assertValidHtml, box, computed, settle } from "dojo-web";

const site = await openSite();

test("axe-core finds no accessibility problems", async () => {
  const page = await site.open("/index.html");
  await assertAccessible(page);
});

test("axe-core finds none with the details open either", async () => {
  const page = await site.open("/index.html");
  await page.locator(".details-toggle").click();
  await settle(page);
  await assertAccessible(page);
});

test("the HTML is valid", async () => {
  await assertValidHtml("index.html");
});

test("the page has one banner, nav, main and contentinfo landmark", async () => {
  const page = await site.open("/index.html");
  for (const role of ["banner", "navigation", "main", "contentinfo"]) {
    assert.equal(await page.getByRole(role).count(), 1, `one ${role} landmark`);
  }
  assert.equal(await page.locator("main").getAttribute("id"), "main", '<main id="main">');
});

test("headings start at h1 and never skip a level", async () => {
  const page = await site.open("/index.html");
  const levels = await page.$$eval("h1, h2, h3, h4, h5, h6", (all) => all.map((h) => Number(h.tagName[1])));
  assert.equal(levels[0], 1, "the first heading is the h1");
  assert.equal(levels.filter((level) => level === 1).length, 1, "one h1");
  levels.forEach((level, i) => {
    if (i > 0) {
      assert.ok(level <= levels[i - 1] + 1, `h${levels[i - 1]} is followed by h${level}`);
    }
  });
  assert.ok(levels.length >= 4, "the three sections keep their headings");
});

test("informative images describe themselves, decorative ones are hidden", async () => {
  const page = await site.open("/index.html");
  const speaker = (await page.locator("img.speaker").getAttribute("alt")) ?? "";
  assert.ok(speaker.trim().length >= 5, "the speaker's photo needs a real description");
  assert.doesNotMatch(speaker, /\.svg|image|picture of/i, "describe the person, not the file");
  assert.equal(await page.locator("img.divider").getAttribute("alt"), "", 'the divider is decorative: alt=""');
});

test("the first Tab reaches a skip link that jumps to the main content", async () => {
  const page = await site.open("/index.html");
  await page.keyboard.press("Tab");
  const focused = await page.evaluate(() => ({
    tag: document.activeElement?.tagName,
    href: document.activeElement?.getAttribute("href"),
    text: document.activeElement?.textContent?.trim(),
  }));
  assert.deepEqual([focused.tag, focused.href], ["A", "#main"], "the first focusable element is <a href=\"#main\">");
  assert.match(focused.text ?? "", /skip/i);
  const link = await box(page, ":focus");
  assert.ok(link.y >= 0 && link.width > 0 && link.height > 0, "the skip link is visible while it has focus");
  await page.keyboard.press("Enter");
  await settle(page);
  assert.equal(await page.evaluate(() => location.hash), "#main");
});

test("the skip link is out of sight until it is focused", async () => {
  const page = await site.open("/index.html");
  const hidden = await page.locator('a[href="#main"]').evaluate((link) => {
    const rect = link.getBoundingClientRect();
    return rect.bottom <= 0 || rect.right <= 0 || rect.width <= 1 || rect.height <= 1;
  });
  assert.ok(hidden, "hide the skip link off screen (it must stay focusable, so no display: none)");
});

test("the details toggle is a button that reports its state", async () => {
  const page = await site.open("/index.html");
  const toggle = page.locator(".details-toggle");
  assert.equal(await toggle.evaluate((el) => el.tagName), "BUTTON", "use a <button>");
  assert.equal(await toggle.getAttribute("type"), "button");
  assert.equal(await toggle.getAttribute("aria-controls"), "talk-details");
  assert.equal(await toggle.getAttribute("aria-expanded"), "false");
  await toggle.focus();
  await page.keyboard.press("Enter");
  await settle(page);
  assert.equal(await toggle.getAttribute("aria-expanded"), "true");
  assert.equal(await page.locator("#talk-details").isVisible(), true);
  await page.keyboard.press("Space");
  await settle(page);
  assert.equal(await toggle.getAttribute("aria-expanded"), "false");
  assert.equal(await page.locator("#talk-details").isVisible(), false);
});

test("the newsletter form has a labelled email field and a submit button", async () => {
  const page = await site.open("/index.html");
  const email = page.getByLabel("Email", { exact: true });
  assert.equal(await email.count(), 1, "a <label> that says Email, tied to the input");
  assert.equal(await email.getAttribute("type"), "email");
  assert.equal(await email.getAttribute("autocomplete"), "email");
  const submit = page.getByRole("button", { name: "Subscribe" });
  assert.equal(await submit.getAttribute("type"), "submit");
  await email.fill("ada@example.com");
  await page.keyboard.press("Enter");
  await settle(page);
  assert.equal(await page.getByRole("status").innerText(), "Thanks! Check your inbox to confirm.");
  assertNoPageErrors(page);
});

test("keyboard focus is visible", async () => {
  const page = await site.open("/index.html");
  await page.locator(".nav a").first().focus();
  await page.keyboard.press("Tab");
  const style = await computed(page, ":focus", ["outline-style", "outline-width", "box-shadow"]);
  const ring = (style["outline-style"] !== "none" && style["outline-width"] !== "0px") || style["box-shadow"] !== "none";
  assert.ok(ring, `a focused link has no outline or box-shadow (${JSON.stringify(style)})`);
});

test("link texts make sense on their own", async () => {
  const page = await site.open("/index.html");
  const texts = await page.$$eval("a", (links) => links.map((a) => a.textContent.trim().toLowerCase()));
  for (const text of texts) {
    assert.ok(!["click here", "here", "read more", "more", "link"].includes(text), `the link "${text}"`);
  }
});
