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

const ESCAPES: Record<string, string> = {
  "&": "&amp;",
  "<": "&lt;",
  ">": "&gt;",
  '"': "&quot;",
  "'": "&#39;",
};

/** Escapes &, <, >, " and ' so text can go inside HTML safely. */
export function escapeHtml(text: string): string {
  return text.replace(/[&<>"']/g, (char) => ESCAPES[char] ?? char);
}

/** One <li><strong>NAME</strong>: MESSAGE</li> per entry, escaped, joined with no separator. */
export function renderEntries(entries: readonly Entry[]): string {
  return entries
    .map((entry) => `<li><strong>${escapeHtml(entry.name)}</strong>: ${escapeHtml(entry.message)}</li>`)
    .join("");
}

/** GET {baseUrl}/api/entries and return the entries; throw when the status is not 2xx. */
export async function fetchEntries(baseUrl: string, fetchFn: FetchFn = fetch): Promise<Entry[]> {
  const response = await fetchFn(`${baseUrl}/api/entries`);
  if (!response.ok) {
    throw new Error(`GET /api/entries failed with ${response.status}`);
  }
  const body = (await response.json()) as { entries: Entry[] };
  return body.entries;
}

/** POST the entry as JSON; return the created entry, or throw with the server's error text. */
export async function postEntry(
  baseUrl: string,
  input: NewEntry,
  fetchFn: FetchFn = fetch,
): Promise<Entry> {
  const response = await fetchFn(`${baseUrl}/api/entries`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(input),
  });
  const body = (await response.json()) as Entry | { error: string };
  if (!response.ok) {
    throw new Error("error" in body ? body.error : `POST /api/entries failed with ${response.status}`);
  }
  return body as Entry;
}
