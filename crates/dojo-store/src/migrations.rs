use rusqlite::{Connection, Result};

/// Schema versions, applied in order and tracked with `PRAGMA user_version`.
/// Never edit a released step: add a new one.
const MIGRATIONS: &[&str] = &[
    // 1: settings and the append-only progress event log.
    "CREATE TABLE settings (
         key   TEXT PRIMARY KEY,
         value TEXT NOT NULL
     );
     CREATE TABLE event (
         id          INTEGER PRIMARY KEY AUTOINCREMENT,
         kind        TEXT    NOT NULL CHECK (kind IN ('node_completed', 'node_reset')),
         track_id    TEXT    NOT NULL,
         node_id     TEXT    NOT NULL,
         xp          INTEGER NOT NULL CHECK (xp >= 0),
         occurred_on TEXT    NOT NULL,
         recorded_at TEXT    NOT NULL
     );
     CREATE INDEX event_by_node ON event (track_id, node_id);",
];

pub fn apply(connection: &mut Connection) -> Result<()> {
    let current: u32 = connection.query_row("PRAGMA user_version", [], |row| row.get(0))?;
    for (version, sql) in (1u32..).zip(MIGRATIONS).skip(current as usize) {
        let transaction = connection.transaction()?;
        transaction.execute_batch(sql)?;
        transaction.pragma_update(None, "user_version", version)?;
        transaction.commit()?;
    }
    Ok(())
}
