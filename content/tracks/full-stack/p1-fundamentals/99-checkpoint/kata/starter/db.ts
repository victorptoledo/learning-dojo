// The notes table and its repository, on Node.js's built-in SQLite.
import { DatabaseSync } from "node:sqlite";
import type { Note, NoteInput } from "./notes.ts";

export const SCHEMA = `
  CREATE TABLE IF NOT EXISTS notes (
    id    INTEGER PRIMARY KEY,
    title TEXT NOT NULL CHECK (length(title) BETWEEN 1 AND 80),
    body  TEXT NOT NULL DEFAULT ''
  )`;

export interface NotesRepo {
  create(note: NoteInput): Note;
  /** Newest first; with a search text, only titles containing it (ASCII case ignored, % and _ literal). */
  list(search?: string): Note[];
  get(id: number): Note | undefined;
  remove(id: number): boolean;
}

/** Opens the database, creates the table when needed, and returns the repository. */
export function openNotes(path = ":memory:"): NotesRepo {
  const db = new DatabaseSync(path);
  db.exec(SCHEMA);
  throw new Error("todo: write the NotesRepo methods with prepared statements");
}
