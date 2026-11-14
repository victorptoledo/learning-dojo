//! Mentoring without AI: the kata hint ladder and spaced review of finished nodes.

use crate::app::{App, Screen};
use crate::quiz_session::QuizSession;
use dojo_core::{HINT_COST_PERCENT, HintRefusal, ProgressEvent, Quiz, review};

impl App {
    /// H on a kata: opens its next hint.
    pub fn reveal_hint(&mut self) {
        let track = self.active_track();
        let Some((_, node)) = self.tree.selected(track) else {
            return;
        };
        let total = node.kata.as_ref().map_or(0, |kata| kata.hints.len());
        let done = self.profile.is_completed(&track.id, &node.id);
        let decision = self.profile.request_hint(track, node, Self::today());
        let event = match decision {
            Ok(event) => event,
            Err(refusal) => {
                self.status_message = Some(hint_refusal_message(refusal, total));
                return;
            }
        };
        let level = match &event {
            ProgressEvent::HintRevealed { level, .. } => *level,
            _ => 0,
        };
        let (track_id, node_id) = (track.id.clone(), node.id.clone());
        self.record(event);
        let cost = if done {
            "free, the kata is already done".to_string()
        } else {
            let xp = self
                .catalog
                .track(&track_id)
                .and_then(|track| track.node(&node_id))
                .map_or(0, |node| self.profile.xp_for(&track_id, node));
            format!("the kata now gives {xp} XP")
        };
        self.status_message = Some(format!(
            "💡 Hint {level} of {total} is open in the kata panel ({cost})."
        ));
    }

    /// R: reviews the questions of finished nodes that are due today.
    pub fn start_review(&mut self) {
        let track = self.active_track();
        let today = Self::today();
        let due = self.profile.due_reviews(track, today);
        if due.is_empty() {
            self.status_message = Some(match self.profile.next_review(track) {
                Some(day) => format!(
                    "🔁 Nothing to review today. Next review: {}.",
                    day.format("%Y-%m-%d")
                ),
                None => "🔁 Finish a lesson or checkpoint first: its questions come back here for review.".to_string(),
            });
            return;
        }
        let picked: Vec<_> = due.into_iter().take(review::SESSION_SIZE).collect();
        let questions = picked
            .iter()
            .filter_map(|item| {
                track
                    .node(&item.node_id)?
                    .quiz
                    .as_ref()?
                    .questions
                    .iter()
                    .find(|question| question.id == item.question_id)
                    .cloned()
            })
            .collect();
        let sources = picked.into_iter().map(|item| item.node_id).collect();
        let quiz = Quiz {
            pass_percent: 0,
            questions,
        };
        self.quiz = Some(QuizSession::review(&track.id, &track.name, quiz, sources));
        self.screen = Screen::Quiz;
        self.status_message = None;
    }

    /// Records every answer of a finished review and says how it went.
    pub(crate) fn close_review(&mut self, session: &QuizSession) -> String {
        let today = Self::today();
        let mut right = 0;
        for (index, question) in session.quiz.questions.iter().enumerate() {
            let correct = !session.missed.contains(&index);
            right += u32::from(correct);
            self.record(ProgressEvent::ReviewAnswered {
                track_id: session.track_id.clone(),
                node_id: session.review_sources[index].clone(),
                question_id: question.id.clone(),
                correct,
                on: today,
            });
        }
        let total = session.quiz.questions.len();
        let left = self
            .catalog
            .track(&session.track_id)
            .map_or(0, |track| self.profile.due_reviews(track, today).len());
        let more = if left > 0 {
            format!(" {left} more due today: press [R] again.")
        } else {
            String::new()
        };
        format!(
            "🔁 Review done: {right}/{total} right, +{} practice XP. Missed questions come back tomorrow.{more}",
            right * review::REVIEW_XP
        )
    }
}

fn hint_refusal_message(refusal: HintRefusal, total: usize) -> String {
    match refusal {
        HintRefusal::NoHints => "This node has no hints.".to_string(),
        HintRefusal::AllRevealed => format!("All {total} hints are already open."),
        HintRefusal::Locked => "🔒 Complete the prerequisites first.".to_string(),
    }
}

/// The footer text for the hint key of a kata, e.g. "[H] Hint 2/3 (−10% XP)".
pub(crate) fn hint_key_label(opened: u8, total: usize, done: bool) -> Option<String> {
    if total == 0 || usize::from(opened) >= total {
        return None;
    }
    let cost = if done {
        String::new()
    } else {
        format!(" (−{HINT_COST_PERCENT}% XP)")
    };
    Some(format!("[H] Hint {}/{total}{cost}", opened + 1))
}
