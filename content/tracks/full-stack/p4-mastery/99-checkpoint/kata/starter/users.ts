// The users repository of the expand release: it writes both shapes and reads either.
import type { DatabaseSync } from "node:sqlite";

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

export function usersRepo(db: DatabaseSync): UsersRepo {
  db.prepare("SELECT 1").get();
  throw new Error("todo: write the UsersRepo methods with prepared statements");
}
