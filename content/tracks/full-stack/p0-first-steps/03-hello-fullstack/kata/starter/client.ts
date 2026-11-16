// The client half: call the guestbook API with fetch and turn entries into HTML.

export interface Entry {
  id: number;
  name: string;
  message: string;
}

export interface NewEntry {
  name: string;
  message: string;
}

export type FetchFn = typeof fetch;

/** Escapes &, <, >, " and ' so text can go inside HTML safely. */
export function escapeHtml(text: string): string {
  throw new Error("todo: write escapeHtml()");
}

/** One <li><strong>NAME</strong>: MESSAGE</li> per entry, escaped, joined with no separator. */
export function renderEntries(entries: readonly Entry[]): string {
  throw new Error("todo: write renderEntries()");
}

/** GET {baseUrl}/api/entries and return the entries; throw when the status is not 2xx. */
export async function fetchEntries(baseUrl: string, fetchFn: FetchFn = fetch): Promise<Entry[]> {
  throw new Error("todo: write fetchEntries()");
}

/** POST the entry as JSON; return the created entry, or throw with the server's error text. */
export async function postEntry(
  baseUrl: string,
  input: NewEntry,
  fetchFn: FetchFn = fetch,
): Promise<Entry> {
  throw new Error("todo: write postEntry()");
}
