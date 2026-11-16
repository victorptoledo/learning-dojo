//! Practice katas (`remedial` in `node.yaml`): short katas on the same skills as a
//! kata or checkpoint, opened on request with F. Passing one gives practice XP once; it
//! never completes, unlocks or changes the node.

use crate::app::App;
use crate::checks::CheckTarget;
use crate::workspace::{CommitOutcome, Prepared};
use dojo_core::{CheckReport, Kata, REMEDIAL_XP, Remedial};

impl App {
    /// F on a kata or checkpoint: opens its next practice kata (the first one not passed
    /// yet, or the first one again when all are passed), creating its folder first.
    pub fn start_practice(&mut self) {
        let track = self.active_track();
        let Some((phase, node)) = self.tree.selected(track) else {
            return;
        };
        if node.remedial.is_empty() {
            self.status_message = Some(if node.kata.is_some() {
                "No practice kata for this one yet. Try the hints [H] or the AI review [A]."
                    .to_string()
            } else {
                "Practice katas belong to katas and checkpoints.".to_string()
            });
            return;
        }
        let remedial = next_practice(&node.remedial, |id| self.profile.practice_done(id));
        let dir = self
            .workspace
            .practice_dir(track, phase, node, &remedial.slug);
        let (title, kata) = (remedial.title.clone(), remedial.kata.clone());
        let done = self.profile.practice_done(&remedial.id);
        let prepared = match self.workspace.prepare(&kata, &dir) {
            Ok(prepared) => prepared,
            Err(error) => {
                self.status_message =
                    Some(format!("⚠ Could not create the practice folder: {error:#}"));
                return;
            }
        };
        if prepared == Prepared::Created {
            self.quiet_new_folder(&dir);
        }
        let opened = self.open_kata_folder(&dir);
        let reward = if done {
            "already passed, no more XP".to_string()
        } else {
            format!("passing gives +{REMEDIAL_XP} practice XP")
        };
        self.status_message = Some(format!(
            "🔁 Practice: {title} ({reward}; the node is unchanged). {opened} Save a file to check it."
        ));
    }

    /// The check target and kata for a practice folder `<track>/practice/<dir name>`.
    pub(crate) fn practice_target(
        &self,
        track_id: &str,
        dir_name: &str,
    ) -> Option<(CheckTarget, Kata)> {
        let track = self.catalog.track(track_id)?;
        track.phases.iter().find_map(|phase| {
            phase.nodes.iter().find_map(|node| {
                node.remedial.iter().find_map(|remedial| {
                    let dir = self
                        .workspace
                        .practice_dir(track, phase, node, &remedial.slug);
                    let matches = dir.file_name().is_some_and(|name| name == dir_name);
                    matches.then(|| {
                        (
                            CheckTarget {
                                track_id: track.id.clone(),
                                node_id: remedial.id.clone(),
                                kata_dir: dir,
                                practice: true,
                            },
                            remedial.kata.clone(),
                        )
                    })
                })
            })
        })
    }

    /// A passing practice check: practice XP the first time, and the solution committed.
    pub(crate) fn reward_practice(&mut self, target: &CheckTarget, report: &CheckReport) -> String {
        let reward =
            match self
                .profile
                .complete_practice(&target.track_id, &target.node_id, Self::today())
            {
                Some(event) => {
                    self.record(event);
                    format!(" 🔁 +{REMEDIAL_XP} practice XP.")
                }
                None => " Already passed before: no more XP.".to_string(),
            };
        let commit = match self.workspace.commit_pass(&target.kata_dir, report) {
            CommitOutcome::Committed => " Solution committed.",
            CommitOutcome::NothingToCommit => "",
            CommitOutcome::GitUnavailable(_) => {
                " (Not committed: the workspace is not a git repository.)"
            }
        };
        format!(
            "✅ Practice {} — {}.{reward}{commit}",
            target.node_id,
            report.summary()
        )
    }
}

/// The first practice kata not passed yet, or the first one when all are passed.
fn next_practice(remedial: &[Remedial], done: impl Fn(&str) -> bool) -> &Remedial {
    remedial
        .iter()
        .find(|practice| !done(&practice.id))
        .unwrap_or(&remedial[0])
}
