// The Server-Sent Events wire format (text/event-stream) and the Last-Event-ID header.

export interface SseEvent {
  id?: number | string;
  event?: string;
  data: string;
}

/** One event frame: optional `id:` and `event:` lines, one `data:` line per line of data, a blank line. */
export function formatEvent(event: SseEvent): string {
  throw new Error(`todo: format the event ${JSON.stringify(event)}`);
}

/** A comment frame: clients ignore it, proxies and load balancers see traffic (a heartbeat). */
export function formatComment(text: string): string {
  throw new Error(`todo: format the comment ${text}`);
}

/** The version a reconnecting client last saw, or `undefined` when the header is missing or not a whole number. */
export function parseLastEventId(header: string | undefined): number | undefined {
  throw new Error(`todo: parse the Last-Event-ID header ${header}`);
}
