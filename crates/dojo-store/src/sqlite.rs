use crate::migrations;
use chrono::{DateTime, NaiveDate, Utc};
use dojo_core::{
    CheckOutcome, CheckRecord, ProgressEvent, ProgressStore, QuizAttempt, QuizScore, Settings,
    StoreError, StreakBaseline,
};
use rusqlite::{Connection, params};
use std::collections::HashMap;
use std::fs;
use std::path::Path;

const DATE_FORMAT: &str = "%Y-%m-%d";

const KEY_NAME: &str = "name";
const KEY_ACTIVE_TRACK: &str = "active_track_id";
const KEY_BRIDGE_FROM: &str = "bridge_from";
const KEY_STREAK_CURRENT: &str = "streak_baseline_current";
const KEY_STREAK_BEST: &str = "streak_baseline_best";
const KEY_STREAK_LAST: &str = "streak_baseline_last_trained";

/// Progress stored in a single SQLite file (`~/.dojo/dojo.db` by default).
pub struct SqliteStore {
    connection: Connection,
}

impl SqliteStore {
    pub fn open(path: &Path) -> Result<Self, StoreError> {
        if let Some(parent) = path.parent() {
            fs::create_dir_all(parent)?;
        }
        Self::prepare(Connection::open(path)?)
    }

    pub fn open_in_memory() -> Result<Self, StoreError> {
        Self::prepare(Connection::open_in_memory()?)
    }

    fn prepare(mut connection: Connection) -> Result<Self, StoreError> {
        connection.pragma_update(None, "foreign_keys", true)?;
        migrations::apply(&mut connection)?;
        Ok(Self { connection })
    }

    pub fn schema_version(&self) -> Result<u32, StoreError> {
        Ok(self
            .connection
            .query_row("PRAGMA user_version", [], |row| row.get(0))?)
    }

    /// Writes settings and events atomically (used by the v1 import).
    pub fn import(
        &mut self,
        settings: &Settings,
        events: &[ProgressEvent],
    ) -> Result<(), StoreError> {
        let transaction = self.connection.transaction()?;
        write_settings(&transaction, settings)?;
        for event in events {
            insert_event(&transaction, event)?;
        }
        transaction.commit()?;
        Ok(())
    }
}

impl ProgressStore for SqliteStore {
    fn load_settings(&self) -> Result<Option<Settings>, StoreError> {
        let mut statement = self.connection.prepare("SELECT key, value FROM settings")?;
        let values: HashMap<String, String> = statement
            .query_map([], |row| Ok((row.get(0)?, row.get(1)?)))?
            .collect::<Result<_, _>>()?;
        let Some(active_track_id) = values.get(KEY_ACTIVE_TRACK) else {
            return Ok(None);
        };
        let mut settings = Settings::new(active_track_id);
        if let Some(name) = values.get(KEY_NAME) {
            settings.name = name.clone();
        }
        if let Some(bridge_from) = values.get(KEY_BRIDGE_FROM) {
            settings.bridge_from = bridge_from.clone();
        }
        settings.streak_baseline = StreakBaseline {
            current: parse_or_default(values.get(KEY_STREAK_CURRENT))?,
            best: parse_or_default(values.get(KEY_STREAK_BEST))?,
            last_trained: values
                .get(KEY_STREAK_LAST)
                .map(|day| parse_date(day))
                .transpose()?,
        };
        Ok(Some(settings))
    }

    fn save_settings(&mut self, settings: &Settings) -> Result<(), StoreError> {
        let transaction = self.connection.transaction()?;
        write_settings(&transaction, settings)?;
        transaction.commit()?;
        Ok(())
    }

    fn load_events(&self) -> Result<Vec<ProgressEvent>, StoreError> {
        let mut statement = self.connection.prepare(
            "SELECT kind, track_id, node_id, xp, occurred_on, hint_level, question_id, correct
             FROM event ORDER BY id",
        )?;
        let rows = statement.query_map([], |row| {
            Ok(EventRow {
                kind: row.get(0)?,
                track_id: row.get(1)?,
                node_id: row.get(2)?,
                xp: row.get(3)?,
                occurred_on: row.get(4)?,
                hint_level: row.get(5)?,
                question_id: row.get(6)?,
                correct: row.get(7)?,
            })
        })?;
        rows.map(|row| row?.into_event()).collect()
    }

    fn append_event(&mut self, event: &ProgressEvent) -> Result<(), StoreError> {
        insert_event(&self.connection, event)
    }

    fn append_check(&mut self, record: &CheckRecord) -> Result<(), StoreError> {
        self.connection.execute(
            "INSERT INTO check_run (track_id, node_id, outcome, passed_tests, total_tests, duration_ms, ran_at)
             VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7)",
            params![
                record.track_id,
                record.node_id,
                record.outcome.as_str(),
                record.passed_tests,
                record.total_tests,
                record.duration_ms as i64,
                record.ran_at.to_rfc3339(),
            ],
        )?;
        Ok(())
    }

    fn latest_check(
        &self,
        track_id: &str,
        node_id: &str,
    ) -> Result<Option<CheckRecord>, StoreError> {
        let mut statement = self.connection.prepare(
            "SELECT outcome, passed_tests, total_tests, duration_ms, ran_at FROM check_run
             WHERE track_id = ?1 AND node_id = ?2 ORDER BY id DESC LIMIT 1",
        )?;
        let mut rows = statement.query(params![track_id, node_id])?;
        let Some(row) = rows.next()? else {
            return Ok(None);
        };
        let outcome: String = row.get(0)?;
        let duration_ms: i64 = row.get(3)?;
        let ran_at: String = row.get(4)?;
        Ok(Some(CheckRecord {
            track_id: track_id.to_string(),
            node_id: node_id.to_string(),
            outcome: CheckOutcome::parse(&outcome)
                .ok_or_else(|| format!("unknown check outcome `{outcome}`"))?,
            passed_tests: row.get(1)?,
            total_tests: row.get(2)?,
            duration_ms: u64::try_from(duration_ms)?,
            ran_at: DateTime::parse_from_rfc3339(&ran_at)?.with_timezone(&Utc),
        }))
    }

    fn check_count(&self, track_id: &str, node_id: &str) -> Result<u32, StoreError> {
        Ok(self.connection.query_row(
            "SELECT COUNT(*) FROM check_run WHERE track_id = ?1 AND node_id = ?2",
            params![track_id, node_id],
            |row| row.get(0),
        )?)
    }

    fn append_quiz_attempt(&mut self, attempt: &QuizAttempt) -> Result<(), StoreError> {
        self.connection.execute(
            "INSERT INTO quiz_attempt (track_id, node_id, correct, total, passed, taken_at)
             VALUES (?1, ?2, ?3, ?4, ?5, ?6)",
            params![
                attempt.track_id,
                attempt.node_id,
                attempt.score.correct,
                attempt.score.total,
                attempt.score.passed,
                attempt.taken_at.to_rfc3339(),
            ],
        )?;
        Ok(())
    }

    fn latest_quiz_attempt(
        &self,
        track_id: &str,
        node_id: &str,
    ) -> Result<Option<QuizAttempt>, StoreError> {
        let mut statement = self.connection.prepare(
            "SELECT correct, total, passed, taken_at FROM quiz_attempt
             WHERE track_id = ?1 AND node_id = ?2 ORDER BY id DESC LIMIT 1",
        )?;
        let mut rows = statement.query(params![track_id, node_id])?;
        let Some(row) = rows.next()? else {
            return Ok(None);
        };
        let taken_at: String = row.get(3)?;
        Ok(Some(QuizAttempt {
            track_id: track_id.to_string(),
            node_id: node_id.to_string(),
            score: QuizScore {
                correct: row.get(0)?,
                total: row.get(1)?,
                passed: row.get(2)?,
            },
            taken_at: DateTime::parse_from_rfc3339(&taken_at)?.with_timezone(&Utc),
        }))
    }
}

// ---- rows -------------------------------------------------------------------

struct EventRow {
    kind: String,
    track_id: String,
    node_id: String,
    xp: u32,
    occurred_on: String,
    hint_level: Option<u8>,
    question_id: Option<String>,
    correct: Option<bool>,
}

impl EventRow {
    fn into_event(self) -> Result<ProgressEvent, StoreError> {
        let on = parse_date(&self.occurred_on)?;
        let Self {
            track_id,
            node_id,
            xp,
            ..
        } = self;
        match self.kind.as_str() {
            "node_completed" => Ok(ProgressEvent::NodeCompleted {
                track_id,
                node_id,
                xp,
                on,
            }),
            "node_reset" => Ok(ProgressEvent::NodeReset {
                track_id,
                node_id,
                xp,
                on,
            }),
            "node_placed_out" => Ok(ProgressEvent::NodePlacedOut {
                track_id,
                node_id,
                on,
            }),
            "belt_exam_assigned" => Ok(ProgressEvent::BeltExamAssigned {
                track_id,
                node_id,
                on,
            }),
            "placement_revoked" => Ok(ProgressEvent::PlacementRevoked {
                track_id,
                node_id,
                on,
            }),
            "hint_revealed" => Ok(ProgressEvent::HintRevealed {
                track_id,
                node_id,
                level: self.hint_level.ok_or("hint_revealed without hint_level")?,
                on,
            }),
            "review_answered" => Ok(ProgressEvent::ReviewAnswered {
                track_id,
                node_id,
                question_id: self
                    .question_id
                    .ok_or("review_answered without question_id")?,
                correct: self.correct.ok_or("review_answered without correct")?,
                on,
            }),
            other => Err(format!("unknown event kind `{other}`").into()),
        }
    }
}

/// The columns of one event row besides the id and `recorded_at`.
struct NewRow<'a> {
    kind: &'static str,
    track_id: &'a str,
    node_id: &'a str,
    xp: u32,
    on: NaiveDate,
    hint_level: Option<u8>,
    question_id: Option<&'a str>,
    correct: Option<bool>,
}

impl<'a> NewRow<'a> {
    fn plain(
        kind: &'static str,
        track_id: &'a str,
        node_id: &'a str,
        xp: u32,
        on: NaiveDate,
    ) -> Self {
        Self {
            kind,
            track_id,
            node_id,
            xp,
            on,
            hint_level: None,
            question_id: None,
            correct: None,
        }
    }
}

fn insert_event(connection: &Connection, event: &ProgressEvent) -> Result<(), StoreError> {
    let row = match event {
        ProgressEvent::NodeCompleted {
            track_id,
            node_id,
            xp,
            on,
        } => NewRow::plain("node_completed", track_id, node_id, *xp, *on),
        ProgressEvent::NodeReset {
            track_id,
            node_id,
            xp,
            on,
        } => NewRow::plain("node_reset", track_id, node_id, *xp, *on),
        ProgressEvent::NodePlacedOut {
            track_id,
            node_id,
            on,
        } => NewRow::plain("node_placed_out", track_id, node_id, 0, *on),
        ProgressEvent::BeltExamAssigned {
            track_id,
            node_id,
            on,
        } => NewRow::plain("belt_exam_assigned", track_id, node_id, 0, *on),
        ProgressEvent::PlacementRevoked {
            track_id,
            node_id,
            on,
        } => NewRow::plain("placement_revoked", track_id, node_id, 0, *on),
        ProgressEvent::HintRevealed {
            track_id,
            node_id,
            level,
            on,
        } => NewRow {
            hint_level: Some(*level),
            ..NewRow::plain("hint_revealed", track_id, node_id, 0, *on)
        },
        ProgressEvent::ReviewAnswered {
            track_id,
            node_id,
            question_id,
            correct,
            on,
        } => NewRow {
            question_id: Some(question_id),
            correct: Some(*correct),
            ..NewRow::plain("review_answered", track_id, node_id, 0, *on)
        },
    };
    connection.execute(
        "INSERT INTO event (kind, track_id, node_id, xp, occurred_on, recorded_at,
                            hint_level, question_id, correct)
         VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7, ?8, ?9)",
        params![
            row.kind,
            row.track_id,
            row.node_id,
            row.xp,
            row.on.format(DATE_FORMAT).to_string(),
            Utc::now().to_rfc3339(),
            row.hint_level,
            row.question_id,
            row.correct,
        ],
    )?;
    Ok(())
}

fn write_settings(connection: &Connection, settings: &Settings) -> Result<(), StoreError> {
    let baseline = &settings.streak_baseline;
    let mut pairs = vec![
        (KEY_NAME, settings.name.clone()),
        (KEY_ACTIVE_TRACK, settings.active_track_id.clone()),
        (KEY_BRIDGE_FROM, settings.bridge_from.clone()),
        (KEY_STREAK_CURRENT, baseline.current.to_string()),
        (KEY_STREAK_BEST, baseline.best.to_string()),
    ];
    if let Some(last) = baseline.last_trained {
        pairs.push((KEY_STREAK_LAST, last.format(DATE_FORMAT).to_string()));
    }
    for (key, value) in pairs {
        connection.execute(
            "INSERT INTO settings (key, value) VALUES (?1, ?2)
             ON CONFLICT (key) DO UPDATE SET value = excluded.value",
            params![key, value],
        )?;
    }
    if baseline.last_trained.is_none() {
        connection.execute(
            "DELETE FROM settings WHERE key = ?1",
            params![KEY_STREAK_LAST],
        )?;
    }
    Ok(())
}

fn parse_date(text: &str) -> Result<NaiveDate, StoreError> {
    Ok(NaiveDate::parse_from_str(text, DATE_FORMAT)?)
}

fn parse_or_default(value: Option<&String>) -> Result<u32, StoreError> {
    Ok(value
        .map(|text| text.parse())
        .transpose()?
        .unwrap_or_default())
}
