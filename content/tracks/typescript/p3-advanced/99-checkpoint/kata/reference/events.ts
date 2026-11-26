// The webhook events a payment provider sends, validated at the boundary with zod. The schema
// is the single source of truth: every type below is derived from it.
import { z } from "zod";

export const PaymentIdSchema = z
  .string()
  .regex(/^pay_[A-Za-z0-9]+$/)
  .brand<"PaymentId">();

/** A payment id that went through the schema: plain strings cannot pose as one. */
export type PaymentId = z.output<typeof PaymentIdSchema>;

const base = {
  id: z.string().regex(/^evt_[A-Za-z0-9]+$/),
  createdAt: z.iso.datetime().transform((text) => new Date(text)),
};

export const WebhookEventSchema = z.discriminatedUnion("type", [
  z.object({
    ...base,
    type: z.literal("payment.succeeded"),
    data: z.object({
      paymentId: PaymentIdSchema,
      amountCents: z.int().positive(),
      currency: z.string().regex(/^[A-Z]{3}$/),
    }),
  }),
  z.object({
    ...base,
    type: z.literal("payment.refunded"),
    data: z.object({
      paymentId: PaymentIdSchema,
      refundedCents: z.int().positive(),
      reason: z.string().optional(),
    }),
  }),
  z.object({
    ...base,
    type: z.literal("customer.created"),
    data: z.object({
      customerId: z.string().min(1),
      email: z.email(),
    }),
  }),
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
  const parsed = WebhookEventSchema.safeParse(input);
  if (parsed.success) {
    return { ok: true, event: parsed.data };
  }
  return {
    ok: false,
    issues: parsed.error.issues.map(
      (issue) => `${issue.path.map(String).join(".") || "(root)"}: ${issue.message}`,
    ),
  };
}
