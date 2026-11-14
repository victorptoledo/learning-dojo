use crate::checks::CheckQueue;
use crate::code_workspace::CodeWorkspace;
use crate::editor::Editor;
use crate::environment::Environment;
use crate::quiz_session::QuizSession;
use crate::setup::SetupSession;
use crate::tree::TrackTree;
use crate::workspace::Workspace;
use anyhow::{Result, anyhow, bail};
use chrono::NaiveDate;
use dojo_core::{
    Catalog, CheckOutcome, CheckRecord, CheckReport, Evidence, KataRunner, Mode, Node, NodeKind,
    Profile, ProgressEvent, ProgressStore, QuizAttempt, Refusal, Settings, Track,
};
use std::collections::{BTreeSet, HashMap, HashSet};
use std::sync::Arc;

const DETAIL_SCROLL_STEP: u16 = 5;

/// Key of per-node state kept in memory: `(track_id, node_id)`.
pub type NodeKey = (String, String);

/// Adapters the app talks to.
pub struct Services {
    pub store: Box<dyn ProgressStore>,
    pub runner: Arc<dyn KataRunner + Send + Sync>,
    pub workspace: Workspace,
    pub editor: Editor,
    /// Regenerated when a kata folder is created, so rust-analyzer picks it up.
    pub code_workspace: Option<CodeWorkspace>,
    /// Checks and installs what nodes need; `None` skips the setup step (tests).
    pub environment: Option<Environment>,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum Screen {
    TrackView,
    TrackSelector,
    Quiz,
    Setup,
    Placement,
    Career,
}

/// Everything the TUI shows and the actions it can take. Rendering lives in `ui`.
pub struct App {
    pub catalog: Catalog,
    pub profile: Profile,
    pub mode: Mode,
    pub screen: Screen,
    pub tree: TrackTree,
    pub selector_cursor: usize,
    /// Highlighted career on the career screen.
    pub career_cursor: usize,
    pub status_message: Option<String>,
    /// Lines scrolled in the detail panel (PageUp/PageDown).
    pub detail_scroll: u16,
    /// A kata folder just created by the dojo: its own file writes must not trigger a check.
    pub(crate) quiet_kata: Option<(std::path::PathBuf, std::time::Instant)>,
    pub workspace: Workspace,
    pub editor: Editor,
    pub(crate) code_workspace: Option<CodeWorkspace>,
    pub(crate) environment: Option<Environment>,
    /// The setup step on screen, while `screen` is `Screen::Setup`.
    pub setup: Option<SetupSession>,
    /// Nodes whose setup already passed in this session.
    pub(crate) setup_cleared: HashSet<NodeKey>,
    /// Latest stored check and attempt count per kata.
    pub check_history: HashMap<NodeKey, (CheckRecord, u32)>,
    /// Latest quiz attempt per lesson.
    pub quiz_history: HashMap<NodeKey, QuizAttempt>,
    /// The quiz being taken, while `screen` is `Screen::Quiz`.
    pub quiz: Option<QuizSession>,
    /// The placement test, while `screen` is `Screen::Placement`.
    pub placement: Option<crate::placement_flow::PlacementFlow>,
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
            code_workspace,
            environment,
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
        let quiz_history = load_quiz_history(&catalog, store.as_ref())?;

        let mut app = Self {
            tree: TrackTree::new(&catalog.tracks[active_track]),
            profile: Profile::replay(settings, &events),
            catalog,
            mode,
            screen: Screen::TrackView,
            selector_cursor: active_track,
            career_cursor: 0,
            status_message: None,
            detail_scroll: 0,
            quiet_kata: None,
            workspace,
            editor,
            code_workspace,
            environment,
            setup: None,
            setup_cleared: HashSet::new(),
            check_history,
            quiz_history,
            quiz: None,
            placement: None,
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

    pub fn scroll_detail_down(&mut self) {
        self.detail_scroll = self.detail_scroll.saturating_add(DETAIL_SCROLL_STEP);
    }

    pub fn scroll_detail_up(&mut self) {
        self.detail_scroll = self.detail_scroll.saturating_sub(DETAIL_SCROLL_STEP);
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
        self.detail_scroll = 0;
        self.status_message = None;
    }

    pub fn move_up(&mut self) {
        self.tree.move_up();
        self.detail_scroll = 0;
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
        let message = match event {
            ProgressEvent::NodeCompleted { xp, .. } => format!("⭐ Node completed! +{xp} XP"),
            ProgressEvent::NodeReset { xp, .. } => format!("Node reset (-{xp} XP)."),
            ProgressEvent::NodePlacedOut { .. }
            | ProgressEvent::BeltExamAssigned { .. }
            | ProgressEvent::PlacementRevoked { .. }
            | ProgressEvent::HintRevealed { .. }
            | ProgressEvent::ReviewAnswered { .. } => return,
        };
        self.status_message = Some(message);
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

    pub(crate) fn save_settings(&mut self) {
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
        Refusal::NotPassed => "Not passed yet.".to_string(),
        Refusal::NeedsQuiz => "A lesson is completed by passing its quiz: press [Enter].".to_string(),
        Refusal::UnderConstruction => {
            "🚧 Under construction: this node still needs its document and test. It unlocks once they are written.".to_string()
        }
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

impl App {
    /// The evidence a node gets when one of its parts passes. A checkpoint needs its
    /// quiz and its kata, so the part that did not just run comes from history.
    pub(crate) fn evidence_for<'a>(
        &self,
        key: &NodeKey,
        node: &Node,
        passed_now: Evidence<'a>,
    ) -> Evidence<'a> {
        if node.kind != NodeKind::Checkpoint {
            return passed_now;
        }
        let quiz_before = self
            .quiz_history
            .get(key)
            .is_some_and(|attempt| attempt.score.passed);
        let checks_before = self
            .check_history
            .get(key)
            .is_some_and(|(record, _)| record.outcome == CheckOutcome::Passed);
        match passed_now {
            Evidence::Quiz(score) => Evidence::Checkpoint {
                quiz_passed: score.passed,
                checks_passed: checks_before,
            },
            Evidence::Checks(report) => Evidence::Checkpoint {
                quiz_passed: quiz_before,
                checks_passed: report.is_pass(),
            },
            other => other,
        }
    }
}

/// What a half-done checkpoint still needs, for the status line.
pub(crate) fn checkpoint_missing_part(evidence: &Evidence) -> Option<&'static str> {
    match evidence {
        Evidence::Checkpoint {
            quiz_passed: true,
            checks_passed: false,
        } => Some("The checkpoint also needs its integration kata: press [Enter]."),
        Evidence::Checkpoint {
            quiz_passed: false,
            checks_passed: true,
        } => Some("The checkpoint also needs its quiz: press [Q]."),
        _ => None,
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

fn load_quiz_history(
    catalog: &Catalog,
    store: &dyn ProgressStore,
) -> Result<HashMap<NodeKey, QuizAttempt>> {
    let mut history = HashMap::new();
    for track in &catalog.tracks {
        for node in track.nodes().filter(|node| node.quiz.is_some()) {
            if let Some(attempt) = store
                .latest_quiz_attempt(&track.id, &node.id)
                .map_err(|error| anyhow!(error))?
            {
                history.insert((track.id.clone(), node.id.clone()), attempt);
            }
        }
    }
    Ok(history)
}
