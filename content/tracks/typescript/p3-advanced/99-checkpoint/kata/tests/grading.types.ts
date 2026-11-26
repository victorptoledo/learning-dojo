import type { EventOf, EventType, PaymentId, WebhookEvent, WebhookEventInput } from "../events.ts";
import type { Receipt } from "../inbox.ts";
import { WebhookInbox } from "../inbox.ts";
import { sign, verify } from "../vendor/signature.js";
import type { Equal, Expect } from "./expect.ts";

// test: the declaration file describes the vendor module exactly
export type SignType = Expect<Equal<typeof sign, (secret: string, body: string) => string>>;
export type VerifyType = Expect<
  Equal<typeof verify, (secret: string, body: string, signature: string) => boolean>
>;

// test: the event types come from the schema
export type Types = Expect<Equal<EventType, "payment.succeeded" | "payment.refunded" | "customer.created">>;
export type Refunded = Expect<Equal<EventOf<"payment.refunded">["data"]["refundedCents"], number>>;
export type Reason = Expect<Equal<EventOf<"payment.refunded">["data"]["reason"], string | undefined>>;
export type Email = Expect<Equal<EventOf<"customer.created">["data"]["email"], string>>;

// test: createdAt is a string going in and a Date coming out
export type CreatedOut = Expect<Equal<WebhookEvent["createdAt"], Date>>;
export type CreatedIn = Expect<Equal<WebhookEventInput["createdAt"], string>>;

// test: a plain string is not a PaymentId
// @ts-expect-error: only the schema makes a PaymentId
export const forged: PaymentId = "pay_1";
export type PaymentIdInEvent = Expect<Equal<EventOf<"payment.succeeded">["data"]["paymentId"], PaymentId>>;

// test: handlers receive the event of their type
declare const inbox: WebhookInbox;
inbox.on("payment.succeeded", (event) => {
  const amount: number = event.data.amountCents;
  void amount;
});
inbox.on("customer.created", (event) => {
  // @ts-expect-error: a customer event has no amount
  void event.data.amountCents;
});
// @ts-expect-error: not an event type
inbox.on("payment.failed", () => {});

// test: a receipt narrows on its status
declare const receipt: Receipt;
if (receipt.status === "failed") {
  const error: Error = receipt.error;
  void error;
}
// @ts-expect-error: only a rejected receipt has a reason
void receipt.reason;
