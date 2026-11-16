import assert from "node:assert/strict";
import { test } from "node:test";
import { openSite, assertAccessible, assertNoPageErrors, settle } from "dojo-web";

const received = [];
const site = await openSite({
  routes: {
    "POST /api/feedback": (request) => {
      const body = request.json();
      received.push({ body, type: request.headers["content-type"] });
      if (body?.name === "Offline") {
        return { status: 503, json: { error: "try later" } };
      }
      return { status: 201, json: { id: received.length }, delayMs: 300 };
    },
  },
});

const GOOD = { name: "Ada", email: "ada@example.com", rating: "4", message: "The kata about forms was great." };

async function fill(page, values) {
  if (values.name !== undefined) await page.getByLabel("Name").fill(values.name);
  if (values.email !== undefined) await page.getByLabel("Email").fill(values.email);
  if (values.rating) await page.getByRole("radio", { name: values.rating, exact: true }).check();
  if (values.message !== undefined) await page.getByLabel("Message").fill(values.message);
}

const send = (page) => page.getByRole("button", { name: "Send feedback" }).click();
const errorText = (page, name) => page.locator(`#${name}-error`).innerText();

test("errorFor() gives a message for each broken constraint", async () => {
  const page = await site.open("/index.html");
  const out = await page.evaluate(async () => {
    const { errorFor } = await import("/app.js");
    const $ = (selector) => document.querySelector(selector);
    const results = [errorFor($("#name")), errorFor($("#email")), errorFor($('[name="rating"]')), errorFor($("#message"))];
    $("#name").value = "   ";
    results.push(errorFor($("#name")));
    $("#email").value = "not-an-email";
    results.push(errorFor($("#email")));
    $("#message").value = "too short";
    results.push(errorFor($("#message")));
    $("#name").value = "Ada";
    $("#email").value = "ada@example.com";
    $('[name="rating"][value="3"]').checked = true;
    $("#message").value = "Long enough message.";
    results.push(errorFor($("#name")), errorFor($("#email")), errorFor($('[name="rating"]')), errorFor($("#message")));
    return results;
  });
  assert.deepEqual(out, [
    "Enter your name.",
    "Enter your email address.",
    "Choose a rating.",
    "Write a message.",
    "Enter your name.",
    "Enter an email address like name@example.com.",
    "Write at least 10 characters.",
    "",
    "",
    "",
    "",
  ]);
});

test("submitting an empty form shows every error and focuses the first field", async () => {
  const page = await site.open("/index.html");
  received.length = 0;
  await send(page);
  await settle(page);
  assert.equal(await errorText(page, "name"), "Enter your name.");
  assert.equal(await errorText(page, "email"), "Enter your email address.");
  assert.equal(await errorText(page, "rating"), "Choose a rating.");
  assert.equal(await errorText(page, "message"), "Write a message.");
  assert.equal(await page.getByLabel("Name").getAttribute("aria-invalid"), "true");
  assert.equal(await page.getByRole("group", { name: "Rating" }).getAttribute("aria-invalid"), "true", "the fieldset is marked invalid");
  assert.equal(await page.evaluate(() => document.activeElement?.id), "name");
  assert.equal(await page.locator("#form-status").innerText(), "Please fix the errors in the form.");
  assert.equal(received.length, 0, "nothing is sent");
});

test("the first invalid field gets the focus, not always the first field", async () => {
  const page = await site.open("/index.html");
  await fill(page, { name: "Ada", email: "ada.example.com", rating: "5", message: "Hello there, dojo!" });
  await send(page);
  await settle(page);
  assert.equal(await page.evaluate(() => document.activeElement?.id), "email");
  assert.equal(await errorText(page, "email"), "Enter an email address like name@example.com.");
  assert.equal(await page.getByLabel("Name").getAttribute("aria-invalid"), null, "valid fields are not marked");
  assert.equal(await errorText(page, "name"), "");
});

test("a field marked invalid is re-checked as the user fixes it", async () => {
  const page = await site.open("/index.html");
  await send(page);
  await settle(page);
  await page.getByLabel("Name").fill("Ada");
  await settle(page);
  assert.equal(await errorText(page, "name"), "");
  assert.equal(await page.getByLabel("Name").getAttribute("aria-invalid"), null);
  await page.getByRole("radio", { name: "2", exact: true }).check();
  await settle(page);
  assert.equal(await errorText(page, "rating"), "");
  assert.equal(await errorText(page, "email"), "Enter your email address.", "other errors stay");
});

test("errors are not shown before the first submit", async () => {
  const page = await site.open("/index.html");
  await page.getByLabel("Email").fill("half");
  await settle(page);
  assert.equal(await errorText(page, "email"), "");
  assert.equal(await page.getByLabel("Email").getAttribute("aria-invalid"), null);
});

test("formValues() reads the form with FormData", async () => {
  const page = await site.open("/index.html");
  await fill(page, GOOD);
  const values = await page.evaluate(async () => {
    const { formValues } = await import("/app.js");
    const none = formValues(document.createElement("form"));
    return [formValues(document.querySelector("#feedback")), none];
  });
  assert.deepEqual(values[0], GOOD);
  assert.deepEqual(values[1], { name: "", email: "", rating: "", message: "" });
});

test("the message counter follows the text", async () => {
  const page = await site.open("/index.html");
  await page.getByLabel("Message").fill("Hello");
  await settle(page);
  assert.equal(await page.locator("#message-count").innerText(), "5 / 500");
});

test("typing saves a draft that comes back after a reload", async () => {
  const page = await site.open("/index.html");
  await fill(page, GOOD);
  await settle(page);
  const saved = JSON.parse(await page.evaluate(() => localStorage.getItem("feedback-draft")));
  assert.deepEqual(saved, GOOD);
  await page.reload();
  await settle(page);
  assert.equal(await page.getByLabel("Name").inputValue(), "Ada");
  assert.equal(await page.getByLabel("Message").inputValue(), GOOD.message);
  assert.equal(await page.getByRole("radio", { name: "4", exact: true }).isChecked(), true);
  assert.equal(await page.locator("#message-count").innerText(), `${GOOD.message.length} / 500`);
});

test("a broken draft is ignored", async () => {
  const page = await site.open("/index.html", { storage: { "feedback-draft": "{not json" } });
  await settle(page);
  assert.equal(await page.getByLabel("Name").inputValue(), "");
  assertNoPageErrors(page);
});

test("a valid form is sent as JSON, with the button disabled meanwhile", async () => {
  const page = await site.open("/index.html");
  received.length = 0;
  await fill(page, GOOD);
  await send(page);
  await page.getByText("Sending…").waitFor();
  assert.equal(await page.getByRole("button", { name: "Send feedback" }).isDisabled(), true, "no double submit");
  await page.getByText("Thanks, Ada! Your feedback was sent.").waitFor();
  assert.equal(received.length, 1);
  assert.deepEqual(received[0].body, GOOD);
  assert.match(received[0].type ?? "", /application\/json/);
  assert.equal(await page.getByRole("button", { name: "Send feedback" }).isDisabled(), false);
});

test("after a successful send the form is empty and the draft is gone", async () => {
  const page = await site.open("/index.html");
  await fill(page, GOOD);
  await send(page);
  await page.getByText("Thanks, Ada! Your feedback was sent.").waitFor();
  assert.equal(await page.getByLabel("Name").inputValue(), "");
  assert.equal(await page.getByLabel("Message").inputValue(), "");
  assert.equal(await page.evaluate(() => localStorage.getItem("feedback-draft")), null);
  assert.equal(await page.locator("#message-count").innerText(), "0 / 500");
});

test("a failed send keeps the text and the draft", async () => {
  const page = await site.open("/index.html");
  await fill(page, { ...GOOD, name: "Offline" });
  await send(page);
  await page.getByText("Sending failed. Your message is saved; try again.").waitFor();
  assert.equal(await page.getByLabel("Message").inputValue(), GOOD.message);
  assert.notEqual(await page.evaluate(() => localStorage.getItem("feedback-draft")), null);
  assert.equal(await page.getByRole("button", { name: "Send feedback" }).isDisabled(), false);
  assertNoPageErrors(page);
});

test("sendFeedback() throws when the server says no", async () => {
  const page = await site.open("/index.html");
  const out = await page.evaluate(async () => {
    const { sendFeedback } = await import("/app.js");
    return sendFeedback({ name: "Offline" }).then(() => "no error", (error) => error.message);
  });
  assert.match(out, /503/);
});

test("axe-core finds no problems with the errors shown", async () => {
  const page = await site.open("/index.html");
  await send(page);
  await settle(page);
  await assertAccessible(page);
});
