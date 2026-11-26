// The Server-Sent Events wire format (text/event-stream) and the Last-Event-ID header.

export interface SseEvent {
  id?: number | string;
  event?: string;
  data: string;
}

/** One event frame: optional `id:` and `event:` lines, one `data:` line per line of data, a blank line. */
export function formatEvent(event: SseEvent): string {
  let frame = "";
  if (event.id !== undefined) frame += `id: ${event.id}\n`;
  if (event.event !== undefined) frame += `event: ${event.event}\n`;
  for (const line of event.data.split(/\r\n|\r|\n/)) frame += `data: ${line}\n`;
  return `${frame}\n`;
}

/** A comment frame: clients ignore it, proxies and load balancers see traffic (a heartbeat). */
export function formatComment(text: string): string {
  return `: ${text}\n\n`;
}

/** The version a reconnecting client last saw, or `undefined` when the header is missing or not a whole number. */
export function parseLastEventId(header: string | undefined): number | undefined {
  if (header === undefined || !/^\d+$/.test(header)) return undefined;
  const value = Number(header);
  return Number.isSafeInteger(value) ? value : undefined;
}
