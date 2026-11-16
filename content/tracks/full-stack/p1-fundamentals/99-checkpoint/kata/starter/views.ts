// The server-rendered page: every user-supplied text goes through escapeHtml.
import type { FieldErrors, Note, NoteInput } from "./notes.ts";

const ESCAPES: Record<string, string> = { "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" };

/** Escapes &, <, >, " and ' for HTML text and attribute values. */
export function escapeHtml(text: string): string {
  return text.replace(/[&<>"']/g, (char) => ESCAPES[char] ?? char);
}

export interface PageModel {
  notes: readonly Note[];
  /** The search text, shown again in the search box. */
  search?: string;
  /** Validation errors to show above the form. */
  errors?: FieldErrors;
  /** What the user typed, shown again after an error. */
  values?: Partial<NoteInput>;
}

/** The whole HTML page (see README.md for the parts the tests look for). */
export function notesPage(model: PageModel): string {
  throw new Error("todo: write notesPage()");
}
