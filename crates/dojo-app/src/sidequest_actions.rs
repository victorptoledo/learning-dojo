//! Sidequests (D028, D030): optional projects of the active track, opened with S. One
//! folder per sidequest holds every stage; stages open one at a time, and a check runs
//! the contract tests of every stage up to the open one. A passing stage gives sidequest
//! XP once and opens the next stage; all stages passed earn the badge. Nothing here
//! completes a node or counts towards belts.

use crate::app::{App, Screen};
use crate::checks::{CheckTarget, TargetKind};
use crate::workspace::{CommitOutcome, Prepared};
use dojo_core::{CheckReport, SIDEQUEST_STAGE_XP, Sidequest};

impl App {
    /// S: the sidequest screen of the active track.
    pub fn open_sidequests(&mut self) {
        let track_id = self.active_track().id.clone();
        let quests = self.catalog.sidequests_of(&track_id);
        if quests.is_empty() {
            self.status_message = Some(format!(
                "No sidequests for {} yet.",
                self.active_track().name
            ));
            return;
        }
        // Start on the first unlocked sidequest not finished yet.
        self.sidequest_cursor = quests
            .iter()
            .position(|quest| {
                quest.is_unlocked(&self.catalog, &self.profile) && !quest.is_done(&self.profile)
            })
            .unwrap_or(0);
        self.screen = Screen::Sidequests;
        self.status_message = None;
    }

    pub fn close_sidequests(&mut self) {
        self.screen = Screen::TrackView;
    }

    pub fn sidequest_down(&mut self) {
        let count = self.track_sidequests().len().max(1);
        self.sidequest_cursor = (self.sidequest_cursor + 1) % count;
    }

    pub fn sidequest_up(&mut self) {
        let count = self.track_sidequests().len().max(1);
        self.sidequest_cursor = (self.sidequest_cursor + count - 1) % count;
    }

    /// The active track's sidequests, by level.
    pub fn track_sidequests(&self) -> Vec<&Sidequest> {
        self.catalog.sidequests_of(&self.active_track().id)
    }

    pub fn selected_sidequest(&self) -> Option<&Sidequest> {
        self.track_sidequests().get(self.sidequest_cursor).copied()
    }

    /// Enter: creates or updates the sidequest folder and opens it, once it is unlocked.
    pub fn start_sidequest(&mut self) {
        let Some(quest) = self.selected_sidequest().cloned() else {
            return;
        };
        if let Some(message) = self.locked_message(&quest) {
            self.status_message = Some(message);
            return;
        }
        let passed = quest.passed_stages(&self.profile);
        let prepared = match self.workspace.prepare_sidequest(&quest, passed) {
            Ok(prepared) => prepared,
            Err(error) => {
                self.status_message = Some(format!(
                    "⚠ Could not prepare the sidequest folder: {error:#}"
                ));
                return;
            }
        };
        let dir = self.workspace.sidequest_dir(&quest);
        // The dojo just wrote README.md (and maybe a starter): that is not a save.
        self.quiet_new_folder(&dir);
        if prepared == Prepared::Created
            && let Some(code_workspace) = &self.code_workspace
            && let Err(error) = code_workspace.write()
        {
            self.status_message = Some(format!("⚠ {error:#}"));
            return;
        }
        let opened = self.open_kata_folder(&dir);
        let progress = match quest.current_stage(&self.profile) {
            Some(index) => format!(
                "stage {} of {}: {} (+{SIDEQUEST_STAGE_XP} sidequest XP)",
                index + 1,
                quest.stages.len(),
                quest.stages[index].title
            ),
            None => "every stage passed, the badge is yours".to_string(),
        };
        self.status_message = Some(format!(
            "⚔ {} — {progress}. {opened} Save a file to check it.",
            quest.title
        ));
    }

    /// C on the sidequest screen: checks the open stage now.
    pub fn check_selected_sidequest(&mut self) {
        let Some(quest) = self.selected_sidequest().cloned() else {
            return;
        };
        if !self.workspace.sidequest_dir(&quest).exists() {
            self.status_message = Some("Press Enter to start this sidequest first.".to_string());
            return;
        }
        self.check_sidequest(&quest);
    }

    /// A file saved in `<track>/sidequests/<slug>`: checks that sidequest's open stage.
    pub(crate) fn check_sidequest_folder(&mut self, track_id: &str, slug: &str) {
        let Some(quest) = self
            .catalog
            .sidequests_of(track_id)
            .into_iter()
            .find(|quest| quest.slug == slug)
            .cloned()
        else {
            return;
        };
        self.check_sidequest(&quest);
    }

    /// Runs the open stage's check (the last stage again once all are passed).
    fn check_sidequest(&mut self, quest: &Sidequest) {
        if let Some(message) = self.locked_message(quest) {
            self.status_message = Some(message);
            return;
        }
        let Some(last) = quest.stages.len().checked_sub(1) else {
            return;
        };
        let index = quest.current_stage(&self.profile).unwrap_or(last);
        let target = CheckTarget {
            track_id: quest.track_id.clone(),
            node_id: quest.stages[index].id.clone(),
            kata_dir: self.workspace.sidequest_dir(quest),
            kind: TargetKind::Stage,
        };
        let label = format!("{} stage {}", quest.title, index + 1);
        if self.checks.start_parts(target, quest.check_plan(index)) {
            self.status_message = Some(format!("⏳ Checking {label} in Docker…"));
        } else {
            self.status_message =
                Some("A check is already running; save again when it finishes.".to_string());
        }
    }

    /// `None` when unlocked, otherwise what is still missing.
    fn locked_message(&self, quest: &Sidequest) -> Option<String> {
        let missing = quest.missing(&self.catalog, &self.profile);
        (!missing.is_empty()).then(|| {
            let labels: Vec<String> = missing
                .iter()
                .map(|requirement| requirement.label(&self.catalog))
                .collect();
            format!(
                "🔒 {} is locked. Missing: {}.",
                quest.title,
                labels.join(", ")
            )
        })
    }

    /// A passing stage: sidequest XP the first time, the next stage opened, and the
    /// folder committed.
    pub(crate) fn reward_stage(&mut self, target: &CheckTarget, report: &CheckReport) -> String {
        let Some(quest) = self.catalog.sidequest_of_stage(&target.node_id).cloned() else {
            return format!("✅ {} — {}.", target.node_id, report.summary());
        };
        let Some((index, _)) = quest.stage(&target.node_id) else {
            return String::new();
        };
        let reward = match self
            .profile
            .pass_stage(&target.track_id, &target.node_id, Self::today())
        {
            Some(event) => {
                self.record(event);
                format!(" ⚔ +{SIDEQUEST_STAGE_XP} sidequest XP.")
            }
            None => " Already passed before: no more XP.".to_string(),
        };
        let commit = match self.workspace.commit_pass(&target.kata_dir, report) {
            CommitOutcome::Committed => " Committed.",
            CommitOutcome::NothingToCommit => "",
            CommitOutcome::GitUnavailable(_) => {
                " (Not committed: the workspace is not a git repository.)"
            }
        };
        let next = match quest.current_stage(&self.profile) {
            Some(open) if open > index => {
                let opened = self
                    .workspace
                    .prepare_sidequest(&quest, quest.passed_stages(&self.profile));
                self.quiet_new_folder(&target.kata_dir);
                match opened {
                    Ok(_) => format!(
                        " 🔓 Stage {} of {} opened: {} (see README.md).",
                        open + 1,
                        quest.stages.len(),
                        quest.stages[open].title
                    ),
                    Err(error) => format!(" ⚠ Could not open the next stage: {error:#}"),
                }
            }
            Some(_) => String::new(),
            None => {
                let _ = self.workspace.prepare_sidequest(&quest, quest.stages.len());
                self.quiet_new_folder(&target.kata_dir);
                format!(" 🏅 Sidequest complete: {}! Badge earned.", quest.title)
            }
        };
        format!(
            "✅ {} stage {} of {} — {}.{reward}{next}{commit}",
            quest.title,
            index + 1,
            quest.stages.len(),
            report.summary()
        )
    }
}
