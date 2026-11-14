use crate::profile_file::ProfileFile;
use crate::tree::TrackTree;
use anyhow::{Result, bail};
use dojo_core::{Catalog, Profile, ToggleOutcome, Track};
use std::collections::BTreeSet;

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum Mode {
    TrackView,
    TrackSelector,
}

/// Everything the TUI shows and the actions it can take. Rendering lives in `ui`.
pub struct App {
    pub catalog: Catalog,
    pub profile: Profile,
    pub mode: Mode,
    pub tree: TrackTree,
    pub selector_cursor: usize,
    pub status_message: Option<String>,
    active_track: usize,
    profile_file: ProfileFile,
}

impl App {
    pub fn new(mut catalog: Catalog, profile_file: ProfileFile) -> Result<Self> {
        if catalog.tracks.is_empty() {
            bail!("no tracks found in the content folder");
        }
        catalog
            .tracks
            .sort_by(|a, b| (&a.category, &a.name).cmp(&(&b.category, &b.name)));

        let mut profile = profile_file.load_or_new(&catalog.tracks[0].id)?;
        let migrated = profile.migrate_legacy_ids(&catalog.legacy_id_map());
        let active_track = catalog.track_index(&profile.active_track_id).unwrap_or(0);
        profile.active_track_id = catalog.tracks[active_track].id.clone();

        let mut app = Self {
            tree: TrackTree::new(&catalog.tracks[active_track]),
            catalog,
            profile,
            mode: Mode::TrackView,
            selector_cursor: active_track,
            status_message: None,
            active_track,
            profile_file,
        };
        if migrated > 0 {
            app.save();
            app.status_message = Some(format!(
                "Migrated {migrated} completed nodes from the v1 profile."
            ));
        }
        Ok(app)
    }

    pub fn active_track(&self) -> &Track {
        &self.catalog.tracks[self.active_track]
    }

    pub fn active_track_index(&self) -> usize {
        self.active_track
    }

    // ---- track view ---------------------------------------------------------

    pub fn move_down(&mut self) {
        self.tree.move_down();
        self.status_message = None;
    }

    pub fn move_up(&mut self) {
        self.tree.move_up();
        self.status_message = None;
    }

    /// Marks the selected node as done (or undone). Replaced by evidence-based completion later.
    pub fn toggle_selected(&mut self) {
        let track = &self.catalog.tracks[self.active_track];
        let Some((_, node)) = self.tree.selected(track) else {
            return;
        };
        let today = chrono::Local::now().date_naive();
        let outcome = self.profile.toggle_complete(track, node, today);
        self.status_message = Some(match outcome {
            ToggleOutcome::Completed { xp } => format!("⭐ Node completed! +{xp} XP"),
            ToggleOutcome::Uncompleted { xp } => format!("Node marked as not done (-{xp} XP)."),
            ToggleOutcome::Locked => "🔒 Complete the prerequisites first.".to_string(),
        });
        if outcome != ToggleOutcome::Locked {
            self.save();
        }
    }

    /// Cycles the Rosetta bridge source language among those available in the catalog.
    pub fn cycle_bridge_language(&mut self) {
        let languages: Vec<String> = self.bridge_languages().into_iter().collect();
        if languages.is_empty() {
            self.status_message = Some("No bridges available yet.".to_string());
            return;
        }
        let current = languages
            .iter()
            .position(|lang| *lang == self.profile.bridge_from);
        let next = current.map_or(0, |index| (index + 1) % languages.len());
        self.profile.bridge_from = languages[next].clone();
        self.status_message = Some(format!(
            "Bridges now explained from {}.",
            self.profile.bridge_from
        ));
        self.save();
    }

    fn bridge_languages(&self) -> BTreeSet<String> {
        self.catalog
            .tracks
            .iter()
            .flat_map(|track| track.nodes())
            .flat_map(|node| node.bridges.keys().cloned())
            .collect()
    }

    // ---- track selector -----------------------------------------------------

    pub fn open_selector(&mut self) {
        self.selector_cursor = self.active_track;
        self.mode = Mode::TrackSelector;
        self.status_message = Some("Pick a track and press Enter.".to_string());
    }

    pub fn close_selector(&mut self) {
        self.mode = Mode::TrackView;
        self.status_message = None;
    }

    pub fn selector_down(&mut self) {
        self.selector_cursor = (self.selector_cursor + 1) % self.catalog.tracks.len();
    }

    pub fn selector_up(&mut self) {
        let count = self.catalog.tracks.len();
        self.selector_cursor = (self.selector_cursor + count - 1) % count;
    }

    pub fn confirm_selection(&mut self) {
        self.active_track = self.selector_cursor;
        self.tree = TrackTree::new(&self.catalog.tracks[self.active_track]);
        self.profile.active_track_id = self.active_track().id.clone();
        self.mode = Mode::TrackView;
        self.status_message = Some(format!("🥋 Now training: {}", self.active_track().name));
        self.save();
    }

    fn save(&mut self) {
        if let Err(error) = self.profile_file.save(&self.profile) {
            self.status_message = Some(format!("⚠ Could not save progress: {error:#}"));
        }
    }
}
