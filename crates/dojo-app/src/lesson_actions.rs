//! What the learner can do with a lesson: read it and take its quiz.

use crate::app::{App, NodeKey, Screen, checkpoint_missing_part};
use crate::quiz_session::QuizSession;
use chrono::Utc;
use dojo_core::{Evidence, Node, QuizAttempt, QuizScore, Refusal, Track};

impl App {
    /// Enter on a lesson: opens its document in the editor.
    pub(crate) fn open_lesson_document(&mut self) {
        let track = self.active_track();
        let Some((_, node)) = self.tree.selected(track) else {
            return;
        };
        if node.under_construction() {
            self.status_message = Some(crate::app::refusal_message(Refusal::UnderConstruction));
            return;
        }
        let Some(file) = node.lesson_file.clone() else {
            return;
        };
        self.status_message = Some(match self.editor.open_file(&file) {
            Ok(()) => format!(
                "Opened the lesson in `{}`. When you are ready, press [Q] for the quiz.",
                self.editor.command()
            ),
            Err(error) => format!("⚠ {error}. The lesson is also shown here; [Q] starts the quiz."),
        });
    }

    /// Q on a lesson: opens its quiz.
    pub fn start_lesson_quiz(&mut self) {
        let track = self.active_track();
        let Some((_, node)) = self.tree.selected(track) else {
            return;
        };
        if let Some(message) = quiz_blocker(&self.profile, track, node) {
            self.status_message = Some(message);
            return;
        }
        let Some(quiz) = node.quiz.clone() else {
            self.status_message =
                Some("Only lessons have a quiz. Katas are checked with [C].".to_string());
            return;
        };
        self.quiz = Some(QuizSession::new(&track.id, &node.id, &node.title, quiz));
        self.screen = Screen::Quiz;
        self.status_message = None;
    }

    /// Esc during a quiz: leaves without recording anything.
    pub fn abandon_quiz(&mut self) {
        self.quiz = None;
        self.screen = Screen::TrackView;
        self.status_message = Some("Quiz closed. Nothing was recorded.".to_string());
    }

    /// Enter on the score screen: records the attempt and completes the lesson on a pass.
    pub fn close_finished_quiz(&mut self) {
        let Some(session) = self.quiz.take() else {
            return;
        };
        self.screen = Screen::TrackView;
        let Some(score) = session.score() else {
            return;
        };
        if session.is_review() {
            self.status_message = Some(self.close_review(&session));
            return;
        }
        let key: NodeKey = (session.track_id.clone(), session.node_id.clone());
        let attempt = QuizAttempt {
            track_id: session.track_id.clone(),
            node_id: session.node_id.clone(),
            score,
            taken_at: Utc::now(),
        };
        if let Err(error) = self.store.append_quiz_attempt(&attempt) {
            self.status_message = Some(format!("⚠ Could not save the quiz: {error}"));
            return;
        }
        self.quiz_history.insert(key, attempt);
        self.status_message = Some(self.quiz_outcome(&session, score));
    }

    fn quiz_outcome(&mut self, session: &QuizSession, score: QuizScore) -> String {
        let summary = format!("{}/{} correct", score.correct, score.total);
        if !score.passed {
            let missed_skills = session.missed_skills();
            let revoked =
                self.revoke_missed_skills(&session.track_id, &session.node_id, &missed_skills);
            return format!(
                "📝 {summary} — {} needed. Reread the lesson and press [Q] to try again.{revoked}",
                session.quiz.needed()
            );
        }
        let decision = self
            .catalog
            .track(&session.track_id)
            .and_then(|track| Some((track, track.node(&session.node_id)?)))
            .map(|(track, node)| {
                let key = (track.id.clone(), node.id.clone());
                let evidence = self.evidence_for(&key, node, Evidence::Quiz(score));
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
        match decision {
            Some(Ok(event)) => format!(
                "✅ Quiz passed: {summary}.{}",
                self.record_completion(event)
            ),
            Some(Err(Refusal::AlreadyCompleted)) | None => {
                format!("✅ Quiz passed again: {summary}.")
            }
            Some(Err(Refusal::NotPassed)) if missing_part.is_some() => {
                format!(
                    "✅ Quiz passed: {summary}. {}",
                    missing_part.unwrap_or_default()
                )
            }
            Some(Err(refusal)) => format!("{summary}. {}", crate::app::refusal_message(refusal)),
        }
    }
}

/// Why the quiz of `node` cannot be taken right now, if it cannot.
fn quiz_blocker(profile: &dojo_core::Profile, track: &Track, node: &Node) -> Option<String> {
    if node.under_construction() {
        return Some(crate::app::refusal_message(Refusal::UnderConstruction));
    }
    if !profile.is_unlocked(track, node) {
        return Some(crate::app::refusal_message(Refusal::Locked));
    }
    None
}
