//! One-time import of the v1 `profile.json` into the SQLite store.

use crate::sqlite::SqliteStore;
use anyhow::{Context, Result};
use chrono::NaiveDate;
use dojo_core::{Catalog, ProgressEvent, ProgressStore, Settings, StreakBaseline};
use serde::Deserialize;
use std::collections::{BTreeMap, HashMap, HashSet};
use std::fs;
use std::path::{Path, PathBuf};

/// The JSON written by v1 (and by M0) at `data/profile.json`.
#[derive(Deserialize)]
struct V1Profile {
    name: String,
    current_streak: u32,
    best_streak: u32,
    last_trained: Option<NaiveDate>,
    active_track_id: String,
    #[serde(default)]
    bridge_from: Option<String>,
    #[serde(default)]
    track_progress: HashMap<String, HashSet<String>>,
}

#[derive(Debug, PartialEq, Eq)]
pub enum ImportOutcome {
    /// No v1 file, or the store already holds a profile.
    NothingToImport,
    Imported {
        completed_nodes: usize,
        /// Completed IDs that no longer exist in the catalog.
        unknown_nodes: usize,
        backup: PathBuf,
    },
}

/// Imports `v1_path` into `store` when the store is still empty, then renames the
/// file to `profile.v1.json` so it is never imported twice.
pub fn import_v1_profile(
    v1_path: &Path,
    catalog: &Catalog,
    store: &mut SqliteStore,
    today: NaiveDate,
) -> Result<ImportOutcome> {
    let store_is_empty = store
        .load_settings()
        .map_err(|error| anyhow::anyhow!(error))?
        .is_none();
    if !v1_path.is_file() || !store_is_empty {
        return Ok(ImportOutcome::NothingToImport);
    }

    let text = fs::read_to_string(v1_path)
        .with_context(|| format!("cannot read {}", v1_path.display()))?;
    let v1: V1Profile = serde_json::from_str(&text)
        .with_context(|| format!("invalid v1 profile {}", v1_path.display()))?;
    let (events, unknown_nodes) = completion_events(&v1, catalog, today);
    store
        .import(&settings_from(&v1), &events)
        .map_err(|error| anyhow::anyhow!(error))?;

    let backup = v1_path.with_file_name("profile.v1.json");
    fs::rename(v1_path, &backup)
        .with_context(|| format!("imported, but cannot rename {}", v1_path.display()))?;
    Ok(ImportOutcome::Imported {
        completed_nodes: events.len(),
        unknown_nodes,
        backup,
    })
}

fn settings_from(v1: &V1Profile) -> Settings {
    let mut settings = Settings::new(&v1.active_track_id);
    settings.name = v1.name.clone();
    if let Some(bridge_from) = &v1.bridge_from {
        settings.bridge_from = bridge_from.clone();
    }
    settings.streak_baseline = StreakBaseline {
        current: v1.current_streak,
        best: v1.best_streak,
        last_trained: v1.last_trained,
    };
    settings
}

/// One `NodeCompleted` per completed node that still exists, dated on the last training day.
fn completion_events(
    v1: &V1Profile,
    catalog: &Catalog,
    today: NaiveDate,
) -> (Vec<ProgressEvent>, usize) {
    let legacy_ids: BTreeMap<String, String> = catalog.legacy_id_map();
    let on = v1.last_trained.unwrap_or(today);
    let mut events = Vec::new();
    let mut unknown = 0;
    for (track_id, completed) in &v1.track_progress {
        let mut node_ids: Vec<&String> = completed.iter().collect();
        node_ids.sort();
        for stored_id in node_ids {
            let node_id = legacy_ids.get(stored_id).unwrap_or(stored_id);
            match catalog
                .track(track_id)
                .and_then(|track| track.node(node_id))
            {
                Some(node) => events.push(ProgressEvent::NodeCompleted {
                    track_id: track_id.clone(),
                    node_id: node.id.clone(),
                    xp: node.xp,
                    on,
                }),
                None => unknown += 1,
            }
        }
    }
    (events, unknown)
}
