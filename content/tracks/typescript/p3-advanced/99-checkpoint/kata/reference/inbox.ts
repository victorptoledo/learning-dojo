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

type AnyHandler = (event: EventOf<EventType>) => void | Promise<void>;

export class WebhookInbox {
  readonly #secret: string;
  readonly #handlers = new Map<EventType, AnyHandler[]>();
  /** Ids handled successfully, and ids being handled right now. */
  readonly #done = new Set<string>();
  readonly #inFlight = new Set<string>();

  constructor(secret: string) {
    this.#secret = secret;
  }

  /** Registers a handler for one event type; returns a function that removes it. */
  on<T extends EventType>(type: T, handler: Handler<T>): () => void {
    // The one cast: the map holds handlers of every type, and receive() only calls a
    // handler with an event of the type it was registered for.
    const stored = handler as unknown as AnyHandler;
    const list = this.#handlers.get(type) ?? [];
    list.push(stored);
    this.#handlers.set(type, list);
    return () => {
      const current = this.#handlers.get(type) ?? [];
      this.#handlers.set(
        type,
        current.filter((item) => item !== stored),
      );
    };
  }

  async receive(body: string, signature: string): Promise<Receipt> {
    if (!verify(this.#secret, body, signature)) {
      return { status: "rejected", reason: "signature", issues: [] };
    }

    let json: unknown;
    try {
      json = JSON.parse(body);
    } catch {
      return { status: "rejected", reason: "json", issues: [] };
    }

    const parsed = parseEvent(json);
    if (!parsed.ok) {
      return { status: "rejected", reason: "schema", issues: parsed.issues };
    }
    const event = parsed.event;

    // Claim the id before the first await, so a second delivery that arrives while the
    // handlers run is a duplicate too.
    if (this.#done.has(event.id) || this.#inFlight.has(event.id)) {
      return { status: "duplicate", eventId: event.id };
    }
    this.#inFlight.add(event.id);

    const handlers = [...(this.#handlers.get(event.type) ?? [])];
    try {
      for (const handler of handlers) {
        await handler(event);
      }
    } catch (error) {
      // Not marked as done: the provider will send it again, and the retry is processed.
      return {
        status: "failed",
        eventId: event.id,
        error: error instanceof Error ? error : new Error(String(error)),
      };
    } finally {
      this.#inFlight.delete(event.id);
    }

    this.#done.add(event.id);
    return { status: "accepted", eventId: event.id, type: event.type, handled: handlers.length };
  }
}
