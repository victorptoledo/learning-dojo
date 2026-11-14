use crate::migrations;
use chrono::{NaiveDate, Utc};
use dojo_core::{ProgressEvent, ProgressStore, Settings, StoreError, StreakBaseline};
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
        let mut statement = self
            .connection
            .prepare("SELECT kind, track_id, node_id, xp, occurred_on FROM event ORDER BY id")?;
        let rows = statement.query_map([], |row| {
            Ok(EventRow {
                kind: row.get(0)?,
                track_id: row.get(1)?,
                node_id: row.get(2)?,
                xp: row.get(3)?,
                occurred_on: row.get(4)?,
            })
        })?;
        rows.map(|row| row?.into_event()).collect()
    }

    fn append_event(&mut self, event: &ProgressEvent) -> Result<(), StoreError> {
        insert_event(&self.connection, event)
    }
}

// ---- rows -------------------------------------------------------------------

struct EventRow {
    kind: String,
    track_id: String,
    node_id: String,
    xp: u32,
    occurred_on: String,
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
            other => Err(format!("unknown event kind `{other}`").into()),
        }
    }
}

fn insert_event(connection: &Connection, event: &ProgressEvent) -> Result<(), StoreError> {
    let (kind, track_id, node_id, xp, on) = match event {
        ProgressEvent::NodeCompleted {
            track_id,
            node_id,
            xp,
            on,
        } => ("node_completed", track_id, node_id, xp, on),
        ProgressEvent::NodeReset {
            track_id,
            node_id,
            xp,
            on,
        } => ("node_reset", track_id, node_id, xp, on),
    };
    connection.execute(
        "INSERT INTO event (kind, track_id, node_id, xp, occurred_on, recorded_at)
         VALUES (?1, ?2, ?3, ?4, ?5, ?6)",
        params![
            kind,
            track_id,
            node_id,
            xp,
            on.format(DATE_FORMAT).to_string(),
            Utc::now().to_rfc3339()
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
