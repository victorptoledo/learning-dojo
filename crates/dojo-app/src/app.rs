use crate::checks::CheckQueue;
use crate::tree::TrackTree;
use crate::workspace::Workspace;
use anyhow::{Result, anyhow, bail};
use chrono::NaiveDate;
use dojo_core::{
    Catalog, CheckRecord, CheckReport, KataRunner, Mode, NodeKind, Profile, ProgressEvent,
    ProgressStore, Refusal, Settings, Track,
};
use std::collections::{BTreeSet, HashMap};
use std::sync::Arc;

/// Key of per-node state kept in memory: `(track_id, node_id)`.
pub type NodeKey = (String, String);

/// Adapters the app talks to.
pub struct Services {
    pub store: Box<dyn ProgressStore>,
    pub runner: Arc<dyn KataRunner + Send + Sync>,
    pub workspace: Workspace,
    /// Command that opens a kata folder (`explorer`, `idea64`, `code`, ...).
    pub editor: String,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum Screen {
    TrackView,
    TrackSelector,
}

/// Everything the TUI shows and the actions it can take. Rendering lives in `ui`.
pub struct App {
    pub catalog: Catalog,
    pub profile: Profile,
    pub mode: Mode,
    pub screen: Screen,
    pub tree: TrackTree,
    pub selector_cursor: usize,
    pub status_message: Option<String>,
    pub workspace: Workspace,
    pub editor: String,
    /// Latest stored check and attempt count per kata.
    pub check_history: HashMap<NodeKey, (CheckRecord, u32)>,
    /// Full report of checks run in this session (details are not stored).
    pub last_reports: HashMap<NodeKey, CheckReport>,
    pub(crate) checks: CheckQueue,
    pub(crate) store: Box<dyn ProgressStore>,
    active_track: usize,
}

impl App {
    pub fn new(mut catalog: Catalog, services: Services, mode: Mode) -> Result<Self> {
        let Services {
            store,
            runner,
            workspace,
            editor,
        } = services;
        if catalog.tracks.is_empty() {
            bail!("no tracks found in the content folder");
        }
        catalog
            .tracks
            .sort_by(|a, b| (&a.category, &a.name).cmp(&(&b.category, &b.name)));

        let stored_settings = store.load_settings().map_err(|error| anyhow!(error))?;
        let is_first_run = stored_settings.is_none();
        let mut settings = stored_settings.unwrap_or_else(|| Settings::new(&catalog.tracks[0].id));
        let active_track = catalog.track_index(&settings.active_track_id).unwrap_or(0);
        settings.active_track_id = catalog.tracks[active_track].id.clone();
        let events = store.load_events().map_err(|error| anyhow!(error))?;
        let check_history = load_check_history(&catalog, store.as_ref())?;

        let mut app = Self {
            tree: TrackTree::new(&catalog.tracks[active_track]),
            profile: Profile::replay(settings, &events),
            catalog,
            mode,
            screen: Screen::TrackView,
            selector_cursor: active_track,
            status_message: None,
            workspace,
            editor,
            check_history,
            last_reports: HashMap::new(),
            checks: CheckQueue::new(runner),
            store,
            active_track,
        };
        if is_first_run {
            app.save_settings();
        }
        Ok(app)
    }

    pub fn active_track(&self) -> &Track {
        &self.catalog.tracks[self.active_track]
    }

    pub fn active_track_index(&self) -> usize {
        self.active_track
    }

    pub fn today() -> NaiveDate {
        chrono::Local::now().date_naive()
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

    /// Marks the selected node as done, or undoes it in dev mode.
    pub fn toggle_selected(&mut self) {
        let track = &self.catalog.tracks[self.active_track];
        let Some((_, node)) = self.tree.selected(track) else {
            return;
        };
        let today = Self::today();
        let decision = if self.profile.is_completed(&track.id, &node.id) {
            self.profile.request_reset(track, node, today, self.mode)
        } else {
            self.profile
                .request_completion(track, node, today, self.mode)
        };
        match decision {
            Ok(event) => self.record(event),
            Err(refusal) => self.status_message = Some(refusal_message(refusal)),
        }
    }

    pub(crate) fn record(&mut self, event: ProgressEvent) {
        if let Err(error) = self.store.append_event(&event) {
            self.status_message = Some(format!("⚠ Could not save progress: {error}"));
            return;
        }
        self.profile.apply(&event);
        self.status_message = Some(match event {
            ProgressEvent::NodeCompleted { xp, .. } => format!("⭐ Node completed! +{xp} XP"),
            ProgressEvent::NodeReset { xp, .. } => format!("Node reset (-{xp} XP)."),
        });
    }

    /// Cycles the Rosetta bridge source language among those available in the catalog.
    pub fn cycle_bridge_language(&mut self) {
        let languages: Vec<String> = self.bridge_languages().into_iter().collect();
        if languages.is_empty() {
            self.status_message = Some("No bridges available yet.".to_string());
            return;
        }
        let settings = &mut self.profile.settings;
        let current = languages
            .iter()
            .position(|lang| *lang == settings.bridge_from);
        let next = current.map_or(0, |index| (index + 1) % languages.len());
        settings.bridge_from = languages[next].clone();
        self.status_message = Some(format!("Bridges now explained from {}.", languages[next]));
        self.save_settings();
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
        self.screen = Screen::TrackSelector;
        self.status_message = Some("Pick a track and press Enter.".to_string());
    }

    pub fn close_selector(&mut self) {
        self.screen = Screen::TrackView;
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
        self.profile.settings.active_track_id = self.active_track().id.clone();
        self.screen = Screen::TrackView;
        self.status_message = Some(format!("🥋 Now training: {}", self.active_track().name));
        self.save_settings();
    }

    fn save_settings(&mut self) {
        if let Err(error) = self.store.save_settings(&self.profile.settings) {
            self.status_message = Some(format!("⚠ Could not save settings: {error}"));
        }
    }
}

pub(crate) fn refusal_message(refusal: Refusal) -> String {
    match refusal {
        Refusal::Locked => "🔒 Complete the prerequisites first.".to_string(),
        Refusal::NeedsEvidence(kind) => format!(
            "This {} is completed by passing its checks, not by ticking it. Use --dev to tick it.",
            kind_label(kind)
        ),
        Refusal::DevOnly => "Already done. Undoing a completion needs --dev.".to_string(),
        Refusal::ChecksFailed => "The checks did not pass yet.".to_string(),
        Refusal::AlreadyCompleted => "Already done.".to_string(),
        Refusal::NotCompleted => "Nothing to undo.".to_string(),
    }
}

fn kind_label(kind: NodeKind) -> &'static str {
    match kind {
        NodeKind::Lesson => "lesson",
        NodeKind::Kata => "kata",
        NodeKind::Quiz => "quiz",
        NodeKind::Checkpoint => "checkpoint",
        NodeKind::Project => "project",
    }
}

fn load_check_history(
    catalog: &Catalog,
    store: &dyn ProgressStore,
) -> Result<HashMap<NodeKey, (CheckRecord, u32)>> {
    let mut history = HashMap::new();
    for track in &catalog.tracks {
        for node in track.nodes().filter(|node| node.kata.is_some()) {
            let latest = store
                .latest_check(&track.id, &node.id)
                .map_err(|error| anyhow!(error))?;
            if let Some(record) = latest {
                let attempts = store
                    .check_count(&track.id, &node.id)
                    .map_err(|error| anyhow!(error))?;
                history.insert((track.id.clone(), node.id.clone()), (record, attempts));
            }
        }
    }
    Ok(history)
}
