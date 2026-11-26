// The webhook events a payment provider sends, validated at the boundary with zod. The schema
// is the single source of truth: every type below must be derived from it.
import { z } from "zod";

// TODO: a string that matches /^pay_[A-Za-z0-9]+$/, branded "PaymentId".
export const PaymentIdSchema = z.string();

/** A payment id that went through the schema: plain strings cannot pose as one. */
export type PaymentId = z.output<typeof PaymentIdSchema>;

// A discriminated union on "type" with the three events of the README.
export const WebhookEventSchema = z.discriminatedUnion("type", [
  z.object({
    id: z.string(), // TODO: /^evt_[A-Za-z0-9]+$/
    type: z.literal("payment.succeeded"),
    createdAt: z.string(), // TODO: an ISO date-time string, turned into a Date
    data: z.object({
      paymentId: PaymentIdSchema,
      amountCents: z.number(), // TODO: the rules of the README
      currency: z.string(),
    }),
  }),
  // TODO: "payment.refunded" and "customer.created"
]);

/** An event after parsing: createdAt is a Date, ids are branded. */
export type WebhookEvent = z.output<typeof WebhookEventSchema>;

/** The JSON the provider sends: createdAt is still a string. */
export type WebhookEventInput = z.input<typeof WebhookEventSchema>;

export type EventType = WebhookEvent["type"];

export type EventOf<T extends EventType> = Extract<WebhookEvent, { type: T }>;

export type ParseResult = { ok: true; event: WebhookEvent } | { ok: false; issues: string[] };

/** Validates untrusted data; each issue is "path: message", for example "data.currency: ...". */
export function parseEvent(input: unknown): ParseResult {
  void input;
  return { ok: false, issues: ["(root): not implemented"] };
}
