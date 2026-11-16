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

function noteHtml(note: Note): string {
  return (
    `<article class="note" data-id="${note.id}"><h2>${escapeHtml(note.title)}</h2>` +
    `<p>${escapeHtml(note.body)}</p>` +
    `<form method="post" action="/notes/${note.id}/delete"><button>Delete</button></form></article>`
  );
}

/** The whole HTML page (see README.md for the parts the tests look for). */
export function notesPage(model: PageModel): string {
  const errors = Object.values(model.errors ?? {})
    .map((message) => `<p class="error">${escapeHtml(message)}</p>`)
    .join("");
  const notes = model.notes.length === 0 ? `<p class="empty">No notes yet</p>` : model.notes.map(noteHtml).join("\n");
  return `<!doctype html>
<html lang="en">
<head><meta charset="utf-8"><title>Notes</title></head>
<body>
<h1>Notes</h1>
<form method="get" action="/"><input name="q" value="${escapeHtml(model.search ?? "")}"><button>Search</button></form>
<form method="post" action="/notes">
${errors}
<input name="title" value="${escapeHtml(model.values?.title ?? "")}">
<textarea name="body">${escapeHtml(model.values?.body ?? "")}</textarea>
<button>Add note</button>
</form>
${notes}
</body>
</html>`;
}
