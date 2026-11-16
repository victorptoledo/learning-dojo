// What a note is, and the one validation that the form and the JSON API share.

export interface NoteInput {
  title: string;
  body: string;
}

export interface Note extends NoteInput {
  id: number;
}

export type FieldErrors = Partial<Record<keyof NoteInput, string>>;

export type Validation = { ok: true; note: NoteInput } | { ok: false; errors: FieldErrors };

/** Checks and normalises a note that came from a form or a JSON body (see README.md). */
export function validateNote(input: Record<string, unknown>): Validation {
  const errors: FieldErrors = {};
  const title = typeof input["title"] === "string" ? input["title"].trim() : "";
  if (title === "") {
    errors.title = "Title is required";
  } else if (title.length > 80) {
    errors.title = "Title must be at most 80 characters";
  }
  const rawBody = input["body"] ?? "";
  const body = typeof rawBody === "string" ? rawBody.trim() : undefined;
  if (body === undefined || body.length > 2000) {
    errors.body = "Body must be at most 2000 characters";
  }
  if (errors.title !== undefined || errors.body !== undefined) {
    return { ok: false, errors };
  }
  return { ok: true, note: { title, body: body ?? "" } };
}
