//! What the learner can do with a kata: start it, check it, open it, and what
//! happens when a check finishes (history, completion, auto-commit).

use crate::app::{App, NodeKey, checkpoint_missing_part, refusal_message};
use crate::checks::{CheckTarget, FinishedCheck};
use crate::workspace::{CommitOutcome, Prepared, STATEMENT_FILE};
use chrono::Utc;
use dojo_core::{CheckRecord, CheckReport, Evidence, Kata, Node, Refusal, Track};
use std::path::{Component, Path, PathBuf};
use std::time::{Duration, Instant};

/// Ignore file events in a kata folder for this long after creating it.
const QUIET_AFTER_CREATE: Duration = Duration::from_secs(3);

/// The selected node when it is a kata, with everything needed to act on it.
pub struct SelectedKata<'a> {
    pub track: &'a Track,
    pub node: &'a Node,
    pub kata: &'a Kata,
    pub dir: PathBuf,
}

impl App {
    pub fn selected_kata(&self) -> Option<SelectedKata<'_>> {
        let track = self.active_track();
        let (phase, node) = self.tree.selected(track)?;
        let kata = node.kata.as_ref()?;
        let dir = self.workspace.kata_dir(track, phase, node);
        Some(SelectedKata {
            track,
            node,
            kata,
            dir,
        })
    }

    /// Enter: opens the selected kata (creating its folder first) or lesson in the editor.
    pub fn activate_selected(&mut self) {
        if self.begin_setup_if_needed() {
            return;
        }
        match self.selected_kata() {
            Some(selected) => {
                let (kata, dir) = (selected.kata.clone(), selected.dir.clone());
                self.start_kata(&kata, &dir);
            }
            None => self.open_lesson_document(),
        }
    }

    fn start_kata(&mut self, kata: &Kata, dir: &Path) {
        let prepared = match self.workspace.prepare(kata, dir) {
            Ok(prepared) => prepared,
            Err(error) => {
                self.status_message =
                    Some(format!("⚠ Could not create the kata folder: {error:#}"));
                return;
            }
        };
        if prepared == Prepared::Created {
            self.quiet_kata = Some((dir.to_path_buf(), Instant::now() + QUIET_AFTER_CREATE));
            if let Some(code_workspace) = &self.code_workspace
                && let Err(error) = code_workspace.write()
            {
                self.status_message = Some(format!(
                    "⚠ Could not update the VS Code workspace: {error:#}"
                ));
            }
        }
        let opened = self.open_kata_folder(dir);
        let created = if prepared == Prepared::Created {
            "Kata folder created. "
        } else {
            ""
        };
        self.status_message = Some(format!(
            "{created}{opened} Save a file to run the checks, or press [C]."
        ));
    }

    /// Opens the kata folder with its statement and code, and says what happened.
    fn open_kata_folder(&self, dir: &Path) -> String {
        let files = [dir.join(STATEMENT_FILE), dir.join("src").join("lib.rs")];
        let existing: Vec<_> = files.into_iter().filter(|file| file.is_file()).collect();
        match self.editor.open_folder(dir, &existing) {
            Ok(()) => format!("Opened {} in `{}`.", dir.display(), self.editor.command()),
            Err(error) => format!(
                "⚠ {error}. Set `editor` in ~/.dojo/config.toml. The kata is in {}.",
                dir.display()
            ),
        }
    }

    /// C: checks the selected kata now.
    pub fn check_selected(&mut self) {
        let Some(selected) = self.selected_kata() else {
            self.status_message = Some("Only katas have checks.".to_string());
            return;
        };
        if !selected.dir.exists() {
            self.status_message = Some("Press Enter to start this kata first.".to_string());
            return;
        }
        let target = CheckTarget {
            track_id: selected.track.id.clone(),
            node_id: selected.node.id.clone(),
            kata_dir: selected.dir.clone(),
        };
        let kata = selected.kata.clone();
        self.start_check(target, kata);
    }

    fn start_check(&mut self, target: CheckTarget, kata: Kata) {
        let label = target.node_id.clone();
        if self.checks.start(target, kata) {
            self.status_message = Some(format!("⏳ Checking {label} in Docker…"));
        }
    }

    /// O: opens the selected kata folder in the configured editor.
    pub fn open_selected(&mut self) {
        let Some(selected) = self.selected_kata() else {
            self.status_message = Some("Only katas have a folder to open.".to_string());
            return;
        };
        if !selected.dir.exists() {
            self.status_message = Some("Press Enter to start this kata first.".to_string());
            return;
        }
        let dir = selected.dir.clone();
        self.status_message = Some(self.open_kata_folder(&dir));
    }

    /// A file changed in the workspace: check the kata it belongs to.
    pub fn on_workspace_change(&mut self, changed: &Path) {
        if self.is_quiet(changed) {
            return;
        }
        let Some((track_id, phase_id, slug)) = kata_components(self.workspace.root(), changed)
        else {
            return;
        };
        let node_id = format!("{track_id}.{phase_id}.{slug}");
        let Some(track) = self.catalog.track(&track_id) else {
            return;
        };
        let Some(kata) = track.node(&node_id).and_then(|node| node.kata.clone()) else {
            return;
        };
        let kata_dir = self
            .workspace
            .root()
            .join(&track_id)
            .join(&phase_id)
            .join(&slug);
        self.start_check(
            CheckTarget {
                track_id,
                node_id,
                kata_dir,
            },
            kata,
        );
    }

    /// True while the dojo's own copy of a fresh starter is still settling.
    fn is_quiet(&self, changed: &Path) -> bool {
        self.quiet_kata
            .as_ref()
            .is_some_and(|(dir, until)| changed.starts_with(dir) && Instant::now() < *until)
    }

    /// Called every tick: applies the result of a finished background check.
    pub fn poll_checks(&mut self) {
        if let Some(finished) = self.checks.poll() {
            self.finish_check(finished);
        }
    }

    pub fn is_checking(&self, track_id: &str, node_id: &str) -> bool {
        self.checks
            .running()
            .is_some_and(|target| target.track_id == track_id && target.node_id == node_id)
    }

    pub(crate) fn finish_check(&mut self, finished: FinishedCheck) {
        let FinishedCheck { target, result } = finished;
        let report = match result {
            Ok(report) => report,
            Err(reason) => {
                self.status_message = Some(format!("⚠ {reason}"));
                return;
            }
        };
        let key: NodeKey = (target.track_id.clone(), target.node_id.clone());
        let record =
            CheckRecord::from_report(&target.track_id, &target.node_id, &report, Utc::now());
        if let Err(error) = self.store.append_check(&record) {
            self.status_message = Some(format!("⚠ Could not save the check: {error}"));
            return;
        }
        let attempts = self
            .check_history
            .get(&key)
            .map_or(0, |(_, attempts)| *attempts)
            + 1;
        self.check_history.insert(key.clone(), (record, attempts));
        self.status_message = Some(if report.is_pass() {
            self.reward_pass(&target, &report)
        } else {
            format!(
                "❌ {} — {}. Details in the kata panel.",
                target.node_id,
                report.summary()
            )
        });
        self.last_reports.insert(key, report);
    }

    /// Completes the kata (first pass only) and commits the solution.
    fn reward_pass(&mut self, target: &CheckTarget, report: &CheckReport) -> String {
        let decision = self
            .catalog
            .track(&target.track_id)
            .and_then(|track| Some((track, track.node(&target.node_id)?)))
            .map(|(track, node)| {
                let key = (track.id.clone(), node.id.clone());
                let evidence = self.evidence_for(&key, node, Evidence::Checks(report));
                (
                    evidence,
                    self.profile
                        .complete_by_evidence(track, node, evidence, Self::today()),
                )
            });
        let missing_part = decision
            .as_ref()
            .and_then(|(evidence, _)| checkpoint_missing_part(evidence));
        let decision = decision.map(|(_, decision)| decision);
        let progress = match decision {
            Some(Ok(event)) => self.record_completion(event),
            Some(Err(Refusal::AlreadyCompleted)) | None => String::new(),
            Some(Err(Refusal::NotPassed)) if missing_part.is_some() => {
                format!(" {}", missing_part.unwrap_or_default())
            }
            Some(Err(refusal)) => format!(" {}", refusal_message(refusal)),
        };
        let commit = match self.workspace.commit_pass(&target.kata_dir, report) {
            CommitOutcome::Committed => " Solution committed.",
            CommitOutcome::NothingToCommit => "",
            CommitOutcome::GitUnavailable(_) => {
                " (Not committed: the workspace is not a git repository.)"
            }
        };
        format!(
            "✅ {} — {}.{progress}{commit}",
            target.node_id,
            report.summary()
        )
    }
}

/// `<root>/<track>/<phase>/<slug>/...` → `(track, phase, slug)`.
fn kata_components(root: &Path, changed: &Path) -> Option<(String, String, String)> {
    let relative = changed.strip_prefix(root).ok()?;
    let mut parts = relative
        .components()
        .filter_map(|component| match component {
            Component::Normal(name) => Some(name.to_string_lossy().into_owned()),
            _ => None,
        });
    Some((parts.next()?, parts.next()?, parts.next()?))
}

#[cfg(test)]
mod tests {
    use super::kata_components;
    use std::path::Path;

    #[test]
    fn maps_a_changed_file_to_its_kata() {
        let root = Path::new("/ws");
        assert_eq!(
            kata_components(
                root,
                Path::new("/ws/rust/p1/control-flow-workout/src/lib.rs")
            ),
            Some(("rust".into(), "p1".into(), "control-flow-workout".into()))
        );
        assert_eq!(kata_components(root, Path::new("/ws/README.md")), None);
        assert_eq!(
            kata_components(root, Path::new("/elsewhere/rust/p1/x/a.rs")),
            None
        );
    }
}
