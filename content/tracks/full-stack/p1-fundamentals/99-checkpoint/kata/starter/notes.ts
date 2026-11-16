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
  throw new Error("todo: write validateNote()");
}
