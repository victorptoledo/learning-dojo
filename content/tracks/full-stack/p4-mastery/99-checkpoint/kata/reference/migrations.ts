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
  {
    // Expand: new columns, nullable and without a default, so the release still running keeps
    // inserting rows (with full_name only) and "not backfilled yet" stays visible as NULL.
    name: "002_split_name_expand",
    sql: `ALTER TABLE users ADD COLUMN first_name TEXT;
          ALTER TABLE users ADD COLUMN last_name TEXT;`,
  },
];

/** Applies the migrations not recorded yet, in order; returns the names applied by this call. */
export function migrate(db: DatabaseSync, migrations: Migration[] = MIGRATIONS): string[] {
  db.exec("CREATE TABLE IF NOT EXISTS schema_migrations (name TEXT PRIMARY KEY, applied_at TEXT NOT NULL)");
  const done = new Set(
    (db.prepare("SELECT name FROM schema_migrations").all() as { name: string }[]).map((row) => row.name),
  );
  const record = db.prepare("INSERT INTO schema_migrations (name, applied_at) VALUES (?, datetime('now'))");
  const applied: string[] = [];
  for (const migration of migrations) {
    if (done.has(migration.name)) continue;
    db.exec("BEGIN");
    try {
      db.exec(migration.sql);
      record.run(migration.name);
      db.exec("COMMIT");
    } catch (error) {
      db.exec("ROLLBACK");
      throw error;
    }
    applied.push(migration.name);
  }
  return applied;
}
