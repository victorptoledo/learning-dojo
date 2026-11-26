// Schema migrations, applied in order, each in its own transaction, recorded in schema_migrations.
import type { DatabaseSync } from "node:sqlite";

export interface Migration {
  name: string;
  sql: string;
}

export const MIGRATIONS: Migration[] = [
  {
    name: "001_create_users",
    sql: `CREATE TABLE users (
            id        INTEGER PRIMARY KEY,
            full_name TEXT NOT NULL,
            email     TEXT NOT NULL UNIQUE
          )`,
  },
  // todo: 002_split_name_expand
];

/** Applies the migrations not recorded yet, in order; returns the names applied by this call. */
export function migrate(db: DatabaseSync, migrations: Migration[] = MIGRATIONS): string[] {
  // The release in production today runs the migrations without recording them. Replace this.
  for (const migration of migrations) db.exec(migration.sql.replace("CREATE TABLE users", "CREATE TABLE IF NOT EXISTS users"));
  return [];
}
