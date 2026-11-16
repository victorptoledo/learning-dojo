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
    // 2: history of kata check runs (attempts).
    "CREATE TABLE check_run (
         id           INTEGER PRIMARY KEY AUTOINCREMENT,
         track_id     TEXT    NOT NULL,
         node_id      TEXT    NOT NULL,
         outcome      TEXT    NOT NULL
                      CHECK (outcome IN ('passed', 'tests_failed', 'compile_error', 'timed_out')),
         passed_tests INTEGER NOT NULL CHECK (passed_tests >= 0),
         total_tests  INTEGER NOT NULL CHECK (total_tests >= passed_tests),
         duration_ms  INTEGER NOT NULL CHECK (duration_ms >= 0),
         ran_at       TEXT    NOT NULL
     );
     CREATE INDEX check_run_by_node ON check_run (track_id, node_id, id);",
    // 3: lesson quiz attempts.
    "CREATE TABLE quiz_attempt (
         id       INTEGER PRIMARY KEY AUTOINCREMENT,
         track_id TEXT    NOT NULL,
         node_id  TEXT    NOT NULL,
         correct  INTEGER NOT NULL CHECK (correct >= 0),
         total    INTEGER NOT NULL CHECK (total >= correct),
         passed   INTEGER NOT NULL CHECK (passed IN (0, 1)),
         taken_at TEXT    NOT NULL
     );
     CREATE INDEX quiz_attempt_by_node ON quiz_attempt (track_id, node_id, id);",
    // 4: placement events. SQLite cannot change a CHECK, so the event table is rebuilt.
    "CREATE TABLE event_v4 (
         id          INTEGER PRIMARY KEY AUTOINCREMENT,
         kind        TEXT    NOT NULL CHECK (kind IN ('node_completed', 'node_reset',
                         'node_placed_out', 'belt_exam_assigned', 'placement_revoked')),
         track_id    TEXT    NOT NULL,
         node_id     TEXT    NOT NULL,
         xp          INTEGER NOT NULL CHECK (xp >= 0),
         occurred_on TEXT    NOT NULL,
         recorded_at TEXT    NOT NULL
     );
     INSERT INTO event_v4 (id, kind, track_id, node_id, xp, occurred_on, recorded_at)
         SELECT id, kind, track_id, node_id, xp, occurred_on, recorded_at FROM event;
     DROP TABLE event;
     ALTER TABLE event_v4 RENAME TO event;
     CREATE INDEX event_by_node ON event (track_id, node_id);",
    // 5: hint and spaced review events, with the columns they need.
    "CREATE TABLE event_v5 (
         id          INTEGER PRIMARY KEY AUTOINCREMENT,
         kind        TEXT    NOT NULL CHECK (kind IN ('node_completed', 'node_reset',
                         'node_placed_out', 'belt_exam_assigned', 'placement_revoked',
                         'hint_revealed', 'review_answered')),
         track_id    TEXT    NOT NULL,
         node_id     TEXT    NOT NULL,
         xp          INTEGER NOT NULL CHECK (xp >= 0),
         occurred_on TEXT    NOT NULL,
         recorded_at TEXT    NOT NULL,
         hint_level  INTEGER CHECK (hint_level >= 1),
         question_id TEXT,
         correct     INTEGER CHECK (correct IN (0, 1))
     );
     INSERT INTO event_v5 (id, kind, track_id, node_id, xp, occurred_on, recorded_at)
         SELECT id, kind, track_id, node_id, xp, occurred_on, recorded_at FROM event;
     DROP TABLE event;
     ALTER TABLE event_v5 RENAME TO event;
     CREATE INDEX event_by_node ON event (track_id, node_id);",
    // 6: practice (remedial) kata completions. node_id holds the practice kata id.
    "CREATE TABLE event_v6 (
         id          INTEGER PRIMARY KEY AUTOINCREMENT,
         kind        TEXT    NOT NULL CHECK (kind IN ('node_completed', 'node_reset',
                         'node_placed_out', 'belt_exam_assigned', 'placement_revoked',
                         'hint_revealed', 'review_answered', 'practice_completed')),
         track_id    TEXT    NOT NULL,
         node_id     TEXT    NOT NULL,
         xp          INTEGER NOT NULL CHECK (xp >= 0),
         occurred_on TEXT    NOT NULL,
         recorded_at TEXT    NOT NULL,
         hint_level  INTEGER CHECK (hint_level >= 1),
         question_id TEXT,
         correct     INTEGER CHECK (correct IN (0, 1))
     );
     INSERT INTO event_v6 (id, kind, track_id, node_id, xp, occurred_on, recorded_at,
                           hint_level, question_id, correct)
         SELECT id, kind, track_id, node_id, xp, occurred_on, recorded_at,
                hint_level, question_id, correct FROM event;
     DROP TABLE event;
     ALTER TABLE event_v6 RENAME TO event;
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
