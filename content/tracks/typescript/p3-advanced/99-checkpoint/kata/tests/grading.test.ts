import assert from "node:assert/strict";
import { describe, test } from "node:test";
import { parseEvent } from "../events.ts";
import { WebhookInbox } from "../inbox.ts";
import { sign } from "../vendor/signature.js";

const SECRET = "whsec_grading";

const payment = (overrides: Record<string, unknown> = {}) => ({
  id: "evt_pay1",
  type: "payment.succeeded",
  createdAt: "2026-11-26T10:00:00Z",
  data: { paymentId: "pay_42", amountCents: 1250, currency: "EUR" },
  ...overrides,
});

const deliver = (inbox: WebhookInbox, event: unknown) => {
  const body = JSON.stringify(event);
  return inbox.receive(body, sign(SECRET, body));
};

const issuesOf = (input: unknown): string[] => {
  const parsed = parseEvent(input);
  assert.equal(parsed.ok, false, "the input should be rejected");
  return parsed.ok ? [] : parsed.issues;
};

describe("parseEvent", () => {
  test("accepts the three event types and turns createdAt into a Date", () => {
    const parsed = parseEvent(payment());
    assert.ok(parsed.ok);
    assert.ok(parsed.event.createdAt instanceof Date);
    assert.equal(parsed.event.createdAt.toISOString(), "2026-11-26T10:00:00.000Z");

    const refund = parseEvent({
      id: "evt_ref1",
      type: "payment.refunded",
      createdAt: "2026-11-26T11:00:00Z",
      data: { paymentId: "pay_42", refundedCents: 500 },
    });
    assert.ok(refund.ok, "a refund without a reason is valid");

    const customer = parseEvent({
      id: "evt_cus1",
      type: "customer.created",
      createdAt: "2026-11-26T12:00:00Z",
      data: { customerId: "cus_7", email: "ada@example.com" },
    });
    assert.ok(customer.ok);
  });

  test("reports every issue with its path", () => {
    const issues = issuesOf(payment({ data: { paymentId: "pay_42", amountCents: -5, currency: "eur" } }));
    assert.ok(issues.some((issue) => issue.startsWith("data.amountCents: ")), issues.join("\n"));
    assert.ok(issues.some((issue) => issue.startsWith("data.currency: ")), issues.join("\n"));
  });

  test("rejects an unknown type on the type path", () => {
    const issues = issuesOf(payment({ type: "payment.failed" }));
    assert.ok(issues.some((issue) => issue.startsWith("type: ")), issues.join("\n"));
  });

  test("checks ids, integers, emails and dates", () => {
    issuesOf(payment({ id: "pay1" }));
    issuesOf(payment({ createdAt: "yesterday" }));
    issuesOf(payment({ data: { paymentId: "42", amountCents: 1250, currency: "EUR" } }));
    issuesOf(payment({ data: { paymentId: "pay_42", amountCents: 12.5, currency: "EUR" } }));
    issuesOf({
      id: "evt_cus1",
      type: "customer.created",
      createdAt: "2026-11-26T12:00:00Z",
      data: { customerId: "cus_7", email: "not an email" },
    });
    issuesOf(null);
  });
});

describe("WebhookInbox", () => {
  test("checks the signature before reading the body", async () => {
    const inbox = new WebhookInbox(SECRET);
    const receipt = await inbox.receive("{ not json", "sha256=00");
    assert.deepEqual(receipt, { status: "rejected", reason: "signature", issues: [] });
    const body = JSON.stringify(payment());
    const wrongKey = await inbox.receive(body, sign("another secret", body));
    assert.equal(wrongKey.status, "rejected");
  });

  test("rejects a signed body that is not JSON without throwing", async () => {
    const inbox = new WebhookInbox(SECRET);
    const body = "{ not json";
    assert.deepEqual(await inbox.receive(body, sign(SECRET, body)), {
      status: "rejected",
      reason: "json",
      issues: [],
    });
  });

  test("rejects an invalid event with its issues", async () => {
    const inbox = new WebhookInbox(SECRET);
    const receipt = await deliver(inbox, payment({ data: { paymentId: "pay_42", amountCents: 0, currency: "EUR" } }));
    assert.equal(receipt.status, "rejected");
    assert.ok(receipt.status === "rejected" && receipt.reason === "schema");
    assert.ok(receipt.issues.some((issue) => issue.startsWith("data.amountCents: ")));
  });

  test("calls only the handlers of the event's type, in order, with the parsed event", async () => {
    const inbox = new WebhookInbox(SECRET);
    const calls: string[] = [];
    inbox.on("payment.succeeded", (event) => {
      calls.push(`first ${event.data.amountCents} ${event.createdAt instanceof Date}`);
    });
    inbox.on("customer.created", () => {
      calls.push("customer");
    });
    inbox.on("payment.succeeded", () => {
      calls.push("second");
    });
    const receipt = await deliver(inbox, payment());
    assert.deepEqual(receipt, { status: "accepted", eventId: "evt_pay1", type: "payment.succeeded", handled: 2 });
    assert.deepEqual(calls, ["first 1250 true", "second"]);
  });

  test("an event nobody listens to is still accepted", async () => {
    const inbox = new WebhookInbox(SECRET);
    assert.deepEqual(await deliver(inbox, payment()), {
      status: "accepted",
      eventId: "evt_pay1",
      type: "payment.succeeded",
      handled: 0,
    });
  });

  test("waits for async handlers one after the other", async () => {
    const inbox = new WebhookInbox(SECRET);
    const calls: string[] = [];
    inbox.on("payment.succeeded", async () => {
      calls.push("a start");
      await new Promise((resolve) => setTimeout(resolve, 20));
      calls.push("a end");
    });
    inbox.on("payment.succeeded", () => {
      calls.push("b");
    });
    await deliver(inbox, payment());
    assert.deepEqual(calls, ["a start", "a end", "b"]);
  });

  test("drops a redelivered event", async () => {
    const inbox = new WebhookInbox(SECRET);
    let calls = 0;
    inbox.on("payment.succeeded", () => {
      calls += 1;
    });
    await deliver(inbox, payment());
    assert.deepEqual(await deliver(inbox, payment()), { status: "duplicate", eventId: "evt_pay1" });
    assert.equal(calls, 1);
  });

  test("drops a redelivery that arrives while the first one is still being handled", async () => {
    const inbox = new WebhookInbox(SECRET);
    let calls = 0;
    inbox.on("payment.succeeded", async () => {
      calls += 1;
      await new Promise((resolve) => setTimeout(resolve, 20));
    });
    const receipts = await Promise.all([deliver(inbox, payment()), deliver(inbox, payment())]);
    assert.deepEqual(receipts.map((receipt) => receipt.status).sort(), ["accepted", "duplicate"]);
    assert.equal(calls, 1);
  });

  test("a failing handler fails the delivery, and the retry is processed again", async () => {
    const inbox = new WebhookInbox(SECRET);
    let attempts = 0;
    const after: number[] = [];
    inbox.on("payment.succeeded", () => {
      attempts += 1;
      if (attempts === 1) {
        throw new Error("database is down");
      }
    });
    inbox.on("payment.succeeded", () => {
      after.push(attempts);
    });
    const first = await deliver(inbox, payment());
    assert.equal(first.status, "failed");
    assert.ok(first.status === "failed" && first.error.message === "database is down");
    assert.deepEqual(after, [], "the handlers after the failing one do not run");
    const retry = await deliver(inbox, payment());
    assert.equal(retry.status, "accepted");
    assert.deepEqual(after, [2]);
  });

  test("a thrown non-Error becomes an Error", async () => {
    const inbox = new WebhookInbox(SECRET);
    inbox.on("payment.succeeded", () => {
      throw "boom";
    });
    const receipt = await deliver(inbox, payment());
    assert.ok(receipt.status === "failed");
    assert.ok(receipt.error instanceof Error);
    assert.match(receipt.error.message, /boom/);
  });

  test("the function returned by on removes the handler", async () => {
    const inbox = new WebhookInbox(SECRET);
    const calls: string[] = [];
    const off = inbox.on("payment.succeeded", () => {
      calls.push("removed");
    });
    inbox.on("payment.succeeded", () => {
      calls.push("kept");
    });
    off();
    const receipt = await deliver(inbox, payment());
    assert.ok(receipt.status === "accepted" && receipt.handled === 1);
    assert.deepEqual(calls, ["kept"]);
  });
});
