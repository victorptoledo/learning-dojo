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

function toNote(row: Record<string, unknown>): Note {
  return { id: Number(row["id"]), title: String(row["title"]), body: String(row["body"]) };
}

/** Escapes the LIKE wildcards so the search text is matched literally. */
function likePattern(search: string): string {
  return `%${search.replace(/[\\%_]/g, (char) => `\\${char}`)}%`;
}

/** Opens the database, creates the table when needed, and returns the repository. */
export function openNotes(path = ":memory:"): NotesRepo {
  const db = new DatabaseSync(path);
  db.exec(SCHEMA);
  const insert = db.prepare("INSERT INTO notes (title, body) VALUES (?, ?)");
  const all = db.prepare("SELECT id, title, body FROM notes ORDER BY id DESC");
  const matching = db.prepare(
    "SELECT id, title, body FROM notes WHERE title LIKE ? ESCAPE '\\' ORDER BY id DESC",
  );
  const one = db.prepare("SELECT id, title, body FROM notes WHERE id = ?");
  const remove = db.prepare("DELETE FROM notes WHERE id = ?");
  return {
    create(note) {
      const result = insert.run(note.title, note.body);
      return { id: Number(result.lastInsertRowid), ...note };
    },
    list(search) {
      const rows = search ? matching.all(likePattern(search)) : all.all();
      return rows.map(toNote);
    },
    get(id) {
      const row = one.get(id);
      return row === undefined ? undefined : toNote(row);
    },
    remove(id) {
      return Number(remove.run(id).changes) > 0;
    },
  };
}
