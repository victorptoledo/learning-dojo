//! The placement test on screen: entry question, the questions, and the resulting plan.

use crate::app::{App, Screen};
use dojo_core::{
    Answer, PlacementAnswer, PlacementPlan, PlacementSession, ProgressEvent, QuestionKind,
};

#[derive(Debug, Clone)]
pub enum PlacementStage {
    /// "How familiar are you with ...?"
    Entry,
    /// Answering the drawn questions; `input` holds a typed answer.
    Asking {
        session: Box<PlacementSession>,
        input: String,
    },
    /// The test is over; Enter applies `plan`, Esc throws it away.
    Result {
        session: Box<PlacementSession>,
        plan: PlacementPlan,
    },
}

#[derive(Debug, Clone)]
pub struct PlacementFlow {
    pub track_id: String,
    pub stage: PlacementStage,
}

impl App {
    /// `P` on a track nobody has started: opens the placement test.
    pub fn start_placement(&mut self) {
        let track = self.active_track();
        if track.placement.is_none() {
            self.status_message = Some(format!("{} has no placement test yet.", track.name));
            return;
        }
        if !self.profile.can_take_placement(track) {
            self.status_message =
                Some("The placement test is only for a track you have not started.".to_string());
            return;
        }
        self.placement = Some(PlacementFlow {
            track_id: track.id.clone(),
            stage: PlacementStage::Entry,
        });
        self.screen = Screen::Placement;
        self.status_message = None;
    }

    /// Picks entry option `number` (1-based) and draws the first questions.
    pub fn placement_choose_entry(&mut self, number: usize) {
        let Some(flow) = &self.placement else {
            return;
        };
        let Some(track) = self.catalog.track(&flow.track_id) else {
            return;
        };
        let Some(config) = &track.placement else {
            return;
        };
        if !matches!(flow.stage, PlacementStage::Entry)
            || !(1..=config.entry_options.len()).contains(&number)
        {
            return;
        }
        let session =
            PlacementSession::new(track, config, number - 1, crate::quiz_session::fresh_seed());
        let stage = if session.is_finished() {
            PlacementStage::Result {
                plan: session.plan(track),
                session: Box::new(session),
            }
        } else {
            PlacementStage::Asking {
                session: Box::new(session),
                input: String::new(),
            }
        };
        if let Some(flow) = &mut self.placement {
            flow.stage = stage;
        }
    }

    /// Digit keys: an option of a single-choice question, `0` for "I don't know".
    pub fn placement_choose(&mut self, digit: usize) {
        if matches!(
            self.placement.as_ref().map(|flow| &flow.stage),
            Some(PlacementStage::Entry)
        ) {
            return self.placement_choose_entry(digit);
        }
        let answer = match self.placement_question_kind() {
            Some(QuestionKind::SingleChoice { .. }) if digit == 0 => PlacementAnswer::DontKnow,
            Some(QuestionKind::SingleChoice { options, .. }) if digit <= options.len() => {
                PlacementAnswer::Given(Answer::Choice(digit - 1))
            }
            _ => return,
        };
        self.placement_answer(&answer);
    }

    pub fn placement_type(&mut self, character: char) {
        if let Some(PlacementStage::Asking { input, .. }) =
            self.placement.as_mut().map(|flow| &mut flow.stage)
        {
            input.push(character);
        }
    }

    pub fn placement_backspace(&mut self) {
        if let Some(PlacementStage::Asking { input, .. }) =
            self.placement.as_mut().map(|flow| &mut flow.stage)
        {
            input.pop();
        }
    }

    /// Enter: submits a typed answer (empty means "I don't know") or applies the result.
    pub fn placement_confirm(&mut self) {
        let Some(flow) = &self.placement else {
            return;
        };
        match &flow.stage {
            PlacementStage::Entry => {}
            PlacementStage::Asking { input, .. } => {
                if !matches!(
                    self.placement_question_kind(),
                    Some(QuestionKind::Text { .. })
                ) {
                    return;
                }
                let answer = if input.trim().is_empty() {
                    PlacementAnswer::DontKnow
                } else {
                    PlacementAnswer::Given(Answer::Text(input.clone()))
                };
                self.placement_answer(&answer);
            }
            PlacementStage::Result { .. } => self.apply_placement(),
        }
    }

    /// Esc: leaves the test without recording anything.
    pub fn cancel_placement(&mut self) {
        self.placement = None;
        self.screen = Screen::TrackView;
        self.status_message = Some("Placement test closed. Nothing was recorded.".to_string());
    }

    pub(crate) fn placement_question_kind(&self) -> Option<QuestionKind> {
        match &self.placement.as_ref()?.stage {
            PlacementStage::Asking { session, .. } => {
                Some(session.current()?.question.kind.clone())
            }
            PlacementStage::Entry | PlacementStage::Result { .. } => None,
        }
    }

    fn placement_answer(&mut self, answer: &PlacementAnswer) {
        let Some(flow) = self.placement.take() else {
            return;
        };
        let PlacementStage::Asking { mut session, .. } = flow.stage else {
            self.placement = Some(flow);
            return;
        };
        session.answer(answer);
        let stage = if session.is_finished() {
            let plan = self
                .catalog
                .track(&flow.track_id)
                .map(|track| session.plan(track))
                .unwrap_or_default();
            PlacementStage::Result { session, plan }
        } else {
            PlacementStage::Asking {
                session,
                input: String::new(),
            }
        };
        self.placement = Some(PlacementFlow {
            track_id: flow.track_id,
            stage,
        });
    }

    fn apply_placement(&mut self) {
        let Some(flow) = self.placement.take() else {
            return;
        };
        self.screen = Screen::TrackView;
        let PlacementStage::Result { plan, .. } = flow.stage else {
            return;
        };
        let Some(track) = self.catalog.track(&flow.track_id) else {
            return;
        };
        let events = self.profile.apply_placement(track, &plan, Self::today());
        let exam_title = plan
            .belt_exam
            .as_deref()
            .and_then(|id| track.node(id))
            .map(|node| node.title.clone());
        if events.is_empty() {
            self.status_message =
                Some("Placement done: you start from the beginning of the track.".to_string());
            return;
        }
        for event in events {
            self.record(event);
        }
        let exam = exam_title.map_or(String::new(), |title| {
            format!(" Pass the Belt Exam ({title}) to confirm them and earn their XP.")
        });
        self.status_message = Some(format!(
            "⏭ {} nodes placed out.{exam}",
            plan.placed_out.len()
        ));
    }

    /// Records a completion and, if it was the Belt Exam, confirms the skipped nodes.
    /// Returns the text to show after the completion message.
    pub(crate) fn record_completion(&mut self, event: ProgressEvent) -> String {
        let (track_id, node_id, xp) = match &event {
            ProgressEvent::NodeCompleted {
                track_id,
                node_id,
                xp,
                ..
            } => (track_id.clone(), node_id.clone(), *xp),
            _ => {
                self.record(event);
                return String::new();
            }
        };
        self.record(event);
        let confirmations = self
            .catalog
            .track(&track_id)
            .and_then(|track| Some((track, track.node(&node_id)?)))
            .map(|(track, node)| {
                self.profile
                    .placement_confirmations(track, node, Self::today())
            })
            .unwrap_or_default();
        let next = self.next_step_after(&track_id, &node_id);
        if confirmations.is_empty() {
            return format!(" +{xp} XP.{next}");
        }
        let count = confirmations.len();
        let bonus: u32 = confirmations
            .iter()
            .map(|event| match event {
                ProgressEvent::NodeCompleted { xp, .. } => *xp,
                _ => 0,
            })
            .sum();
        for confirmation in confirmations {
            self.record(confirmation);
        }
        format!(
            " +{xp} XP. 🥋 Belt Exam passed: {count} skipped nodes confirmed, +{bonus} XP.{}",
            self.next_step_after(&track_id, &node_id)
        )
    }

    /// The next-step hint, shown only when a checkpoint closed its phase.
    fn next_step_after(&self, track_id: &str, node_id: &str) -> String {
        let is_checkpoint = self
            .catalog
            .track(track_id)
            .and_then(|track| track.node(node_id))
            .is_some_and(|node| node.kind == dojo_core::NodeKind::Checkpoint);
        if is_checkpoint {
            self.next_step_hint()
        } else {
            String::new()
        }
    }

    /// After a failed quiz: if it was the Belt Exam, skipped nodes that teach a missed
    /// skill go back to study. Returns the text to add to the quiz message.
    pub(crate) fn revoke_missed_skills(
        &mut self,
        track_id: &str,
        node_id: &str,
        missed_skills: &std::collections::HashSet<String>,
    ) -> String {
        let revocations = self
            .catalog
            .track(track_id)
            .and_then(|track| Some((track, track.node(node_id)?)))
            .map(|(track, node)| {
                self.profile
                    .placement_revocations(track, node, missed_skills, Self::today())
            })
            .unwrap_or_default();
        if revocations.is_empty() {
            return String::new();
        }
        let titles: Vec<String> = revocations
            .iter()
            .filter_map(|event| match event {
                ProgressEvent::PlacementRevoked { node_id, .. } => self
                    .catalog
                    .track(track_id)
                    .and_then(|track| track.node(node_id))
                    .map(|node| node.title.clone()),
                _ => None,
            })
            .collect();
        for revocation in revocations {
            self.record(revocation);
        }
        format!(" Back to study: {}.", titles.join(", "))
    }
}
