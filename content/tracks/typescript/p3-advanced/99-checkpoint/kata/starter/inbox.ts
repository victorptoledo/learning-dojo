// A webhook inbox: checks the signature, validates the event, drops duplicates and calls the
// handlers registered for its type.
import { parseEvent, type EventOf, type EventType } from "./events.ts";
import { verify } from "./vendor/signature.js";

export type Handler<T extends EventType> = (event: EventOf<T>) => void | Promise<void>;

export type Receipt =
  | { status: "accepted"; eventId: string; type: EventType; handled: number }
  | { status: "duplicate"; eventId: string }
  | { status: "rejected"; reason: "signature" | "json" | "schema"; issues: string[] }
  | { status: "failed"; eventId: string; error: Error };

export class WebhookInbox {
  readonly #secret: string;

  constructor(secret: string) {
    this.#secret = secret;
  }

  /** Registers a handler for one event type; returns a function that removes it. */
  on<T extends EventType>(type: T, handler: Handler<T>): () => void {
    void type;
    void handler;
    return () => {};
  }

  async receive(body: string, signature: string): Promise<Receipt> {
    // TODO: signature, then JSON, then schema, then duplicates, then the handlers.
    if (!verify(this.#secret, body, signature)) {
      return { status: "rejected", reason: "signature", issues: [] };
    }
    const parsed = parseEvent(JSON.parse(body));
    if (!parsed.ok) {
      return { status: "rejected", reason: "schema", issues: parsed.issues };
    }
    return { status: "accepted", eventId: parsed.event.id, type: parsed.event.type, handled: 0 };
  }
}
