import assert from "node:assert/strict";
import { test } from "node:test";
import { WebhookInbox } from "../inbox.ts";
import { sign } from "../vendor/signature.js";

test("a signed payment event reaches its handler", async () => {
  const inbox = new WebhookInbox("whsec_test");
  const seen: number[] = [];
  inbox.on("payment.succeeded", (event) => {
    seen.push(event.data.amountCents);
  });
  const body = JSON.stringify({
    id: "evt_1",
    type: "payment.succeeded",
    createdAt: "2026-11-26T10:00:00Z",
    data: { paymentId: "pay_1", amountCents: 1250, currency: "EUR" },
  });
  const receipt = await inbox.receive(body, sign("whsec_test", body));
  assert.deepEqual(receipt, { status: "accepted", eventId: "evt_1", type: "payment.succeeded", handled: 1 });
  assert.deepEqual(seen, [1250]);
});
