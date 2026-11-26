// The users repository of the expand release: it writes both shapes and reads either.
import type { DatabaseSync } from "node:sqlite";
import { joinName, splitFullName } from "./names.ts";

export interface User {
  id: number;
  fullName: string;
  firstName: string;
  lastName: string;
  email: string;
}

/** What old clients send (`fullName`) or what new clients send (`firstName` and `lastName`). */
export type Rename = { fullName: string } | { firstName: string; lastName: string };

export interface UsersRepo {
  /** With `readSplit`, first and last names come from their columns once the row has them. */
  get(id: number, readSplit: boolean): User | undefined;
  /** Writes full_name, first_name and last_name together; `undefined` for an unknown user. */
  rename(id: number, change: Rename): User | undefined;
  /** Fills first_name and last_name of at most `batchSize` rows that lack them; returns how many. */
  backfillNames(batchSize: number): number;
}

interface Row {
  id: number;
  full_name: string;
  first_name: string | null;
  last_name: string | null;
  email: string;
}

export function usersRepo(db: DatabaseSync): UsersRepo {
  const one = db.prepare("SELECT id, full_name, first_name, last_name, email FROM users WHERE id = ?");
  const write = db.prepare("UPDATE users SET full_name = ?, first_name = ?, last_name = ? WHERE id = ?");
  const pending = db.prepare("SELECT id, full_name FROM users WHERE first_name IS NULL ORDER BY id LIMIT ?");
  // `AND first_name IS NULL` again: a new client may have renamed the user since the SELECT.
  const fill = db.prepare("UPDATE users SET first_name = ?, last_name = ? WHERE id = ? AND first_name IS NULL");

  function get(id: number, readSplit: boolean): User | undefined {
    const row = one.get(id) as Row | undefined;
    if (!row) return undefined;
    const split =
      readSplit && row.first_name !== null
        ? { firstName: row.first_name, lastName: row.last_name ?? "" }
        : splitFullName(row.full_name);
    return { id: row.id, fullName: row.full_name, ...split, email: row.email };
  }

  return {
    get,
    rename(id, change) {
      const split =
        "fullName" in change
          ? splitFullName(change.fullName)
          : { firstName: change.firstName.trim(), lastName: change.lastName.trim() };
      const result = write.run(joinName(split.firstName, split.lastName), split.firstName, split.lastName, id);
      return result.changes === 0 ? undefined : get(id, true);
    },
    backfillNames(batchSize) {
      const rows = pending.all(batchSize) as { id: number; full_name: string }[];
      db.exec("BEGIN");
      try {
        for (const row of rows) {
          const split = splitFullName(row.full_name);
          fill.run(split.firstName, split.lastName, row.id);
        }
        db.exec("COMMIT");
      } catch (error) {
        db.exec("ROLLBACK");
        throw error;
      }
      return rows.length;
    },
  };
}
