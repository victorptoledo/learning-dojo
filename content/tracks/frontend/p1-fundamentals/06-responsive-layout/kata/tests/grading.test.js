import assert from "node:assert/strict";
import { test } from "node:test";
import { openSite, assertAccessible, box, computed } from "dojo-web";

const site = await openSite();
const near = (a, b, tolerance = 1.5) => Math.abs(a - b) <= tolerance;

/** The boxes of every card, and the distinct column positions they use. */
async function cards(page) {
  const boxes = await page.$$eval(".card", (all) =>
    all.map((card) => {
      const rect = card.getBoundingClientRect();
      return { x: rect.x, y: rect.y, width: rect.width, height: rect.height };
    }),
  );
  const columns = [...new Set(boxes.map((b) => Math.round(b.x)))].sort((a, b) => a - b);
  return { boxes, columns };
}

const noSideScroll = async (page, width) => {
  const scroll = await page.evaluate(() => document.documentElement.scrollWidth);
  assert.ok(scroll <= width, `the page is ${scroll}px wide at ${width}px: something overflows`);
};

test("every element uses border-box sizing and body has no margin", async () => {
  const page = await site.open("/index.html");
  assert.equal((await computed(page, ".card", ["box-sizing"]))["box-sizing"], "border-box");
  assert.equal((await computed(page, "body", ["margin-top"]))["margin-top"], "0px");
});

test("on a phone (375px) the cards form one full-width column", async () => {
  const page = await site.open("/index.html", { width: 375, height: 800 });
  const { boxes, columns } = await cards(page);
  assert.equal(columns.length, 1, `cards start at x = ${columns.join(", ")}`);
  const list = await box(page, ".cards");
  assert.ok(near(boxes[0].width, list.width), `a card is ${boxes[0].width}px in a ${list.width}px list`);
  assert.ok(near(boxes[1].y - (boxes[0].y + boxes[0].height), 16), "1rem (16px) between cards");
  await noSideScroll(page, 375);
});

test("nothing overflows on a 320px screen", async () => {
  const page = await site.open("/index.html", { width: 320, height: 640 });
  await noSideScroll(page, 320);
  const image = await box(page, ".card img");
  const card = await box(page, ".card");
  assert.ok(image.x + image.width <= card.x + card.width + 0.5, "the image is wider than its card");
});

test("on a tablet (768px) there are two columns with a 1rem gap", async () => {
  const page = await site.open("/index.html", { width: 768, height: 1024 });
  const { boxes, columns } = await cards(page);
  assert.equal(columns.length, 2, `cards start at x = ${columns.join(", ")}`);
  const [first, second] = boxes;
  assert.ok(near(first.width, second.width), "both columns are equally wide");
  assert.ok(near(second.x - (first.x + first.width), 16), "1rem (16px) between the columns");
});

test("on a tablet the aside comes below main", async () => {
  const page = await site.open("/index.html", { width: 768, height: 1024 });
  const main = await box(page, "main");
  const aside = await box(page, "aside");
  assert.ok(aside.y >= main.y + main.height, "the aside is below the cards");
  assert.ok(near(aside.width, main.width), "the aside is as wide as main");
});

test("on a desktop (1280px) there are three columns and the aside is 16rem on the right", async () => {
  const page = await site.open("/index.html", { width: 1280, height: 900 });
  const { boxes, columns } = await cards(page);
  assert.equal(columns.length, 3, `cards start at x = ${columns.join(", ")}`);
  assert.ok(near(boxes[0].y, boxes[2].y), "the first three cards share a row");
  const main = await box(page, "main");
  const aside = await box(page, "aside");
  assert.ok(near(aside.width, 256), `the aside is ${aside.width}px wide, not 256px`);
  assert.ok(aside.x >= main.x + main.width, "the aside is right of main");
  assert.ok(near(aside.x - (main.x + main.width), 32), "2rem (32px) between main and the aside");
  assert.ok(near(aside.y, main.y), "main and the aside start at the same height");
});

test("cards in a row are equally tall and their links line up at the bottom", async () => {
  const page = await site.open("/index.html", { width: 1280, height: 900 });
  const { boxes } = await cards(page);
  assert.ok(near(boxes[0].height, boxes[1].height) && near(boxes[1].height, boxes[2].height), "equal heights in the first row");
  const links = await page.$$eval(".card-link", (all) => all.slice(0, 3).map((a) => a.getBoundingClientRect().bottom));
  assert.ok(near(links[0], links[1]) && near(links[1], links[2]), `link bottoms: ${links.map(Math.round).join(", ")}`);
});

test("on a wide screen the content stays at most 72rem and centered", async () => {
  const page = await site.open("/index.html", { width: 1600, height: 900 });
  const layout = await box(page, ".layout");
  assert.ok(layout.width <= 1152.5, `the layout is ${layout.width}px wide`);
  const right = 1600 - (layout.x + layout.width);
  assert.ok(near(layout.x, right), `left gap ${layout.x}px, right gap ${right}px`);
  const header = await box(page, ".site-header .page");
  assert.ok(near(header.x, layout.x), "the header content lines up with the layout");
});

test("the header puts the brand and the nav on one line on a desktop", async () => {
  const page = await site.open("/index.html", { width: 1280, height: 900 });
  const brand = await box(page, ".brand");
  const nav = await box(page, ".site-nav");
  assert.ok(nav.x > brand.x + brand.width, "the nav is to the right of the brand");
  const links = await page.$$eval(".site-nav a", (all) => all.map((a) => Math.round(a.getBoundingClientRect().top)));
  assert.equal(new Set(links).size, 1, "the nav links are in one row");
});

test("on a phone the nav links wrap instead of overflowing", async () => {
  const page = await site.open("/index.html", { width: 320, height: 640 });
  const display = (await computed(page, ".site-nav ul", ["display", "flex-wrap"]));
  assert.equal(display.display, "flex");
  assert.equal(display["flex-wrap"], "wrap");
  const nav = await box(page, ".site-nav");
  assert.ok(nav.x + nav.width <= 320.5, "the nav stays on screen");
});

test("the layout keeps the page accessible", async () => {
  const page = await site.open("/index.html", { width: 375, height: 800 });
  await assertAccessible(page);
});
