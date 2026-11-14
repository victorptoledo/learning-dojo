//! Learner progress, rebuilt from an append-only list of events.
//!
//! The store keeps the events; `Profile` is the in-memory projection used by the UI.
//! `Profile::apply` is the only way progress changes, so replaying the stored events
//! always reproduces the same state.

use crate::check::CheckReport;
use crate::quiz::QuizScore;
use crate::review::{Card, REVIEW_XP, ReviewItem};
use crate::track::{Belt, Node, NodeKind, Track};
use chrono::NaiveDate;
use std::collections::{HashMap, HashSet};

/// Share of a kata's XP that each revealed hint costs, in percent.
pub const HINT_COST_PERCENT: u32 = 10;

pub const DEFAULT_SOURCE_LANGUAGE: &str = "java";
pub const DEFAULT_LEARNER_NAME: &str = "Dev Sensei";

/// Streak values carried over from a v1 profile, which stored totals instead of events.
#[derive(Debug, Clone, Default, PartialEq, Eq)]
pub struct StreakBaseline {
    pub current: u32,
    pub best: u32,
    pub last_trained: Option<NaiveDate>,
}

/// Learner preferences that are not progress.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct Settings {
    pub name: String,
    pub active_track_id: String,
    /// Source language used to pick the Rosetta bridge of each node.
    pub bridge_from: String,
    pub streak_baseline: StreakBaseline,
}

impl Settings {
    pub fn new(active_track_id: &str) -> Self {
        Self {
            name: DEFAULT_LEARNER_NAME.to_string(),
            active_track_id: active_track_id.to_string(),
            bridge_from: DEFAULT_SOURCE_LANGUAGE.to_string(),
            streak_baseline: StreakBaseline::default(),
        }
    }
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub enum ProgressEvent {
    NodeCompleted {
        track_id: String,
        node_id: String,
        xp: u32,
        on: NaiveDate,
    },
    /// Undoes a completion. Only produced in dev mode.
    NodeReset {
        track_id: String,
        node_id: String,
        xp: u32,
        on: NaiveDate,
    },
    /// The placement test skipped this node. It counts as done for unlocking, gives no
    /// XP yet, and becomes a completion when the Belt Exam passes.
    NodePlacedOut {
        track_id: String,
        node_id: String,
        on: NaiveDate,
    },
    /// The checkpoint whose pass confirms the track's skipped nodes.
    BeltExamAssigned {
        track_id: String,
        node_id: String,
        on: NaiveDate,
    },
    /// A failed Belt Exam sends a skipped node back to study.
    PlacementRevoked {
        track_id: String,
        node_id: String,
        on: NaiveDate,
    },
    /// The learner opened hint `level` (1-based) of a kata. Each open hint lowers the
    /// XP the kata gives when it is completed.
    HintRevealed {
        track_id: String,
        node_id: String,
        level: u8,
        on: NaiveDate,
    },
    /// One question answered in a spaced review session.
    ReviewAnswered {
        track_id: String,
        node_id: String,
        question_id: String,
        correct: bool,
        on: NaiveDate,
    },
}

/// Why a hint cannot be opened.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum HintRefusal {
    NoHints,
    AllRevealed,
    Locked,
}

/// A finished node: the XP it gave and the day it was done.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
struct Completion {
    xp: u32,
    on: NaiveDate,
}

type CardKey = (String, String, String);

/// Who is asking to change progress. Dev mode bypasses the evidence rules.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum Mode {
    Learner,
    Dev,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum Refusal {
    AlreadyCompleted,
    NotCompleted,
    Locked,
    /// Katas, quizzes, checkpoints and projects are completed by evidence
    /// (tests, scores, reviews), not by ticking them.
    NeedsEvidence(NodeKind),
    /// A lesson is completed by passing its quiz.
    NeedsQuiz,
    /// The node lacks its document, quiz or exercise; it cannot be completed yet.
    UnderConstruction,
    /// Evidence was offered but did not pass (failing checks or quiz score).
    NotPassed,
    DevOnly,
}

/// What proves a node was learned.
#[derive(Debug, Clone, Copy)]
pub enum Evidence<'a> {
    Checks(&'a CheckReport),
    Quiz(QuizScore),
    /// A checkpoint needs both its quiz and its integration kata passed.
    Checkpoint {
        quiz_passed: bool,
        checks_passed: bool,
    },
}

impl Evidence<'_> {
    pub fn is_pass(&self) -> bool {
        match self {
            Self::Checks(report) => report.is_pass(),
            Self::Quiz(score) => score.passed,
            Self::Checkpoint {
                quiz_passed,
                checks_passed,
            } => *quiz_passed && *checks_passed,
        }
    }
}

#[derive(Debug, Clone)]
pub struct Profile {
    pub settings: Settings,
    pub total_xp: u32,
    pub best_streak: u32,
    pub last_trained: Option<NaiveDate>,
    /// XP from spaced reviews; it never counts towards belts.
    pub practice_xp: u32,
    current_streak: u32,
    completed: HashMap<String, HashMap<String, Completion>>,
    placed_out: HashMap<String, HashSet<String>>,
    belt_exams: HashMap<String, String>,
    /// Hints opened per `(track, node)`.
    hints: HashMap<(String, String), u8>,
    /// Review cards per `(track, node, question)` that were answered at least once.
    cards: HashMap<CardKey, Card>,
}

impl Profile {
    pub fn new(settings: Settings) -> Self {
        let baseline = settings.streak_baseline.clone();
        Self {
            settings,
            total_xp: 0,
            best_streak: baseline.best,
            last_trained: baseline.last_trained,
            practice_xp: 0,
            current_streak: baseline.current,
            completed: HashMap::new(),
            placed_out: HashMap::new(),
            belt_exams: HashMap::new(),
            hints: HashMap::new(),
            cards: HashMap::new(),
        }
    }

    pub fn replay(settings: Settings, events: &[ProgressEvent]) -> Self {
        let mut profile = Self::new(settings);
        for event in events {
            profile.apply(event);
        }
        profile
    }

    pub fn apply(&mut self, event: &ProgressEvent) {
        match event {
            ProgressEvent::NodeCompleted {
                track_id,
                node_id,
                xp,
                on,
            } => {
                let nodes = self.completed.entry(track_id.clone()).or_default();
                if !nodes.contains_key(node_id) {
                    nodes.insert(node_id.clone(), Completion { xp: *xp, on: *on });
                    self.total_xp += xp;
                }
                if let Some(placed) = self.placed_out.get_mut(track_id) {
                    placed.remove(node_id);
                }
                self.record_training(*on);
            }
            ProgressEvent::NodePlacedOut {
                track_id, node_id, ..
            } => {
                if !self.is_completed(track_id, node_id) {
                    self.placed_out
                        .entry(track_id.clone())
                        .or_default()
                        .insert(node_id.clone());
                }
            }
            ProgressEvent::BeltExamAssigned {
                track_id, node_id, ..
            } => {
                self.belt_exams.insert(track_id.clone(), node_id.clone());
            }
            ProgressEvent::PlacementRevoked {
                track_id, node_id, ..
            } => {
                if let Some(placed) = self.placed_out.get_mut(track_id) {
                    placed.remove(node_id);
                }
            }
            ProgressEvent::NodeReset {
                track_id, node_id, ..
            } => {
                let removed = self
                    .completed
                    .get_mut(track_id)
                    .and_then(|nodes| nodes.remove(node_id));
                if let Some(completion) = removed {
                    self.total_xp = self.total_xp.saturating_sub(completion.xp);
                }
                self.hints.remove(&(track_id.clone(), node_id.clone()));
            }
            ProgressEvent::HintRevealed {
                track_id,
                node_id,
                level,
                ..
            } => {
                let opened = self
                    .hints
                    .entry((track_id.clone(), node_id.clone()))
                    .or_default();
                *opened = (*opened).max(*level);
            }
            ProgressEvent::ReviewAnswered {
                track_id,
                node_id,
                question_id,
                correct,
                on,
            } => {
                let Some(completed_on) = self.completed_on(track_id, node_id) else {
                    return;
                };
                let key = (track_id.clone(), node_id.clone(), question_id.clone());
                let card = self
                    .cards
                    .get(&key)
                    .copied()
                    .unwrap_or_else(|| Card::new(completed_on));
                self.cards.insert(key, card.answered(*correct, *on));
                if *correct {
                    self.practice_xp += REVIEW_XP;
                }
                self.record_training(*on);
            }
        }
    }

    /// Extends the streak on consecutive days and restarts it after a gap.
    fn record_training(&mut self, day: NaiveDate) {
        match self.last_trained.map(|last| (day - last).num_days()) {
            Some(days) if days <= 0 => {}
            Some(1) => self.current_streak += 1,
            _ => self.current_streak = 1,
        }
        self.best_streak = self.best_streak.max(self.current_streak);
        self.last_trained = Some(self.last_trained.map_or(day, |last| last.max(day)));
    }

    /// Streak as seen on `today`: it is broken once a whole day passes without training.
    pub fn streak_on(&self, today: NaiveDate) -> u32 {
        match self.last_trained {
            Some(last) if (today - last).num_days() <= 1 => self.current_streak,
            _ => 0,
        }
    }

    // ---- decisions ----------------------------------------------------------

    /// Decides whether `node` may be marked as done and returns the event to record.
    pub fn request_completion(
        &self,
        track: &Track,
        node: &Node,
        today: NaiveDate,
        mode: Mode,
    ) -> Result<ProgressEvent, Refusal> {
        if self.is_completed(&track.id, &node.id) {
            return Err(Refusal::AlreadyCompleted);
        }
        if !self.is_unlocked(track, node) {
            return Err(Refusal::Locked);
        }
        if mode == Mode::Learner {
            return Err(learner_refusal(node));
        }
        Ok(ProgressEvent::NodeCompleted {
            track_id: track.id.clone(),
            node_id: node.id.clone(),
            xp: self.xp_for(&track.id, node),
            on: today,
        })
    }

    /// Completes a node whose evidence passed: a kata's checks or a lesson's quiz.
    pub fn complete_by_evidence(
        &self,
        track: &Track,
        node: &Node,
        evidence: Evidence,
        today: NaiveDate,
    ) -> Result<ProgressEvent, Refusal> {
        if self.is_completed(&track.id, &node.id) {
            return Err(Refusal::AlreadyCompleted);
        }
        if !self.is_unlocked(track, node) {
            return Err(Refusal::Locked);
        }
        if !evidence.is_pass() {
            return Err(Refusal::NotPassed);
        }
        Ok(ProgressEvent::NodeCompleted {
            track_id: track.id.clone(),
            node_id: node.id.clone(),
            xp: self.xp_for(&track.id, node),
            on: today,
        })
    }

    /// Decides whether a completion may be undone (dev mode only).
    pub fn request_reset(
        &self,
        track: &Track,
        node: &Node,
        today: NaiveDate,
        mode: Mode,
    ) -> Result<ProgressEvent, Refusal> {
        if mode != Mode::Dev {
            return Err(Refusal::DevOnly);
        }
        if !self.is_completed(&track.id, &node.id) {
            return Err(Refusal::NotCompleted);
        }
        Ok(ProgressEvent::NodeReset {
            track_id: track.id.clone(),
            node_id: node.id.clone(),
            xp: self.earned_xp(&track.id, &node.id),
            on: today,
        })
    }

    // ---- queries ------------------------------------------------------------

    pub fn is_completed(&self, track_id: &str, node_id: &str) -> bool {
        self.completion(track_id, node_id).is_some()
    }

    fn completion(&self, track_id: &str, node_id: &str) -> Option<&Completion> {
        self.completed.get(track_id)?.get(node_id)
    }

    fn completed_on(&self, track_id: &str, node_id: &str) -> Option<NaiveDate> {
        self.completion(track_id, node_id).map(|done| done.on)
    }

    /// The XP a completed node gave (0 when it is not completed).
    pub fn earned_xp(&self, track_id: &str, node_id: &str) -> u32 {
        self.completion(track_id, node_id).map_or(0, |done| done.xp)
    }

    // ---- hints --------------------------------------------------------------

    pub fn hints_revealed(&self, track_id: &str, node_id: &str) -> u8 {
        self.hints
            .get(&(track_id.to_string(), node_id.to_string()))
            .copied()
            .unwrap_or(0)
    }

    /// The XP `node` gives if completed now: each open hint takes `HINT_COST_PERCENT`.
    pub fn xp_for(&self, track_id: &str, node: &Node) -> u32 {
        let cost = HINT_COST_PERCENT * u32::from(self.hints_revealed(track_id, &node.id));
        node.xp * 100u32.saturating_sub(cost) / 100
    }

    /// Opens the next hint of a kata. Once the kata is completed hints are free.
    pub fn request_hint(
        &self,
        track: &Track,
        node: &Node,
        today: NaiveDate,
    ) -> Result<ProgressEvent, HintRefusal> {
        let available = node.kata.as_ref().map_or(0, |kata| kata.hints.len());
        if available == 0 {
            return Err(HintRefusal::NoHints);
        }
        if !self.is_unlocked(track, node) && !self.is_completed(&track.id, &node.id) {
            return Err(HintRefusal::Locked);
        }
        let opened = self.hints_revealed(&track.id, &node.id);
        if usize::from(opened) >= available {
            return Err(HintRefusal::AllRevealed);
        }
        Ok(ProgressEvent::HintRevealed {
            track_id: track.id.clone(),
            node_id: node.id.clone(),
            level: opened + 1,
            on: today,
        })
    }

    // ---- spaced review ------------------------------------------------------

    /// Every review card of the track's completed nodes, answered or not, in track order.
    fn review_cards<'a>(&'a self, track: &'a Track) -> impl Iterator<Item = ReviewItem> + 'a {
        track.nodes().flat_map(move |node| {
            let completed_on = self.completed_on(&track.id, &node.id);
            let questions = node.quiz.iter().flat_map(|quiz| quiz.questions.iter());
            questions.filter_map(move |question| {
                let start = completed_on?;
                let key = (track.id.clone(), node.id.clone(), question.id.clone());
                Some(ReviewItem {
                    node_id: node.id.clone(),
                    question_id: question.id.clone(),
                    card: self
                        .cards
                        .get(&key)
                        .copied()
                        .unwrap_or_else(|| Card::new(start)),
                })
            })
        })
    }

    /// Questions due on `today`: the ones missed or newest in the schedule first,
    /// then the oldest due date.
    pub fn due_reviews(&self, track: &Track, today: NaiveDate) -> Vec<ReviewItem> {
        let mut due: Vec<ReviewItem> = self
            .review_cards(track)
            .filter(|item| item.card.is_due(today))
            .collect();
        due.sort_by_key(|item| (item.card.step, item.card.due));
        due
    }

    /// The next day something comes up for review, if anything does.
    pub fn next_review(&self, track: &Track) -> Option<NaiveDate> {
        self.review_cards(track).map(|item| item.card.due).min()
    }

    /// Skipped by the placement test and not yet confirmed by the Belt Exam.
    pub fn is_placed_out(&self, track_id: &str, node_id: &str) -> bool {
        self.placed_out
            .get(track_id)
            .is_some_and(|nodes| nodes.contains(node_id))
    }

    /// Completed, or skipped by the placement test: either way it no longer blocks.
    pub fn is_satisfied(&self, track_id: &str, node_id: &str) -> bool {
        self.is_completed(track_id, node_id) || self.is_placed_out(track_id, node_id)
    }

    /// The checkpoint that confirms the track's skipped nodes, while some are pending.
    pub fn belt_exam(&self, track_id: &str) -> Option<&str> {
        let pending = self
            .placed_out
            .get(track_id)
            .is_some_and(|nodes| !nodes.is_empty());
        self.belt_exams
            .get(track_id)
            .map(String::as_str)
            .filter(|_| pending)
    }

    /// The placement test is offered only on a track the learner has not started.
    pub fn can_take_placement(&self, track: &Track) -> bool {
        track.placement.is_some()
            && track
                .nodes()
                .all(|node| !self.is_satisfied(&track.id, &node.id))
    }

    /// Events that apply a placement plan: the skipped nodes and their Belt Exam.
    pub fn apply_placement(
        &self,
        track: &Track,
        plan: &crate::placement::PlacementPlan,
        today: NaiveDate,
    ) -> Vec<ProgressEvent> {
        if !self.can_take_placement(track) || plan.is_empty() {
            return Vec::new();
        }
        let mut events: Vec<ProgressEvent> = plan
            .placed_out
            .iter()
            .map(|node_id| ProgressEvent::NodePlacedOut {
                track_id: track.id.clone(),
                node_id: node_id.clone(),
                on: today,
            })
            .collect();
        if let Some(exam) = &plan.belt_exam {
            events.push(ProgressEvent::BeltExamAssigned {
                track_id: track.id.clone(),
                node_id: exam.clone(),
                on: today,
            });
        }
        events
    }

    /// After `node` is completed: if it was the Belt Exam, every skipped node of the
    /// track becomes a completion with its full XP.
    pub fn placement_confirmations(
        &self,
        track: &Track,
        node: &Node,
        today: NaiveDate,
    ) -> Vec<ProgressEvent> {
        if self.belt_exam(&track.id) != Some(node.id.as_str())
            || !self.is_completed(&track.id, &node.id)
        {
            return Vec::new();
        }
        track
            .nodes()
            .filter(|skipped| self.is_placed_out(&track.id, &skipped.id))
            .map(|skipped| ProgressEvent::NodeCompleted {
                track_id: track.id.clone(),
                node_id: skipped.id.clone(),
                xp: self.xp_for(&track.id, skipped),
                on: today,
            })
            .collect()
    }

    /// After a failed Belt Exam quiz: skipped nodes that teach a missed skill go back.
    pub fn placement_revocations(
        &self,
        track: &Track,
        node: &Node,
        missed_skills: &HashSet<String>,
        today: NaiveDate,
    ) -> Vec<ProgressEvent> {
        if self.belt_exam(&track.id) != Some(node.id.as_str()) {
            return Vec::new();
        }
        track
            .nodes()
            .filter(|skipped| self.is_placed_out(&track.id, &skipped.id))
            .filter(|skipped| skipped.skills().any(|skill| missed_skills.contains(skill)))
            .map(|skipped| ProgressEvent::PlacementRevoked {
                track_id: track.id.clone(),
                node_id: skipped.id.clone(),
                on: today,
            })
            .collect()
    }

    pub fn completed_count(&self, track: &Track) -> usize {
        track
            .nodes()
            .filter(|node| self.is_completed(&track.id, &node.id))
            .count()
    }

    /// A node opens when its prerequisites are done and every node of the earlier
    /// phases is done: a phase must be finished before the next one starts.
    pub fn is_unlocked(&self, track: &Track, node: &Node) -> bool {
        let prerequisites_done = node
            .prerequisites
            .iter()
            .all(|prerequisite| self.is_satisfied(&track.id, prerequisite));
        prerequisites_done
            && self.earlier_phases_done(track, node)
            && self.checkpoint_ready(track, node)
    }

    /// A checkpoint closes its phase: it opens once every other node of the phase is done.
    fn checkpoint_ready(&self, track: &Track, node: &Node) -> bool {
        if node.kind != NodeKind::Checkpoint {
            return true;
        }
        track.phase_of(&node.id).is_none_or(|phase| {
            phase
                .nodes
                .iter()
                .filter(|other| other.id != node.id)
                .all(|other| self.is_satisfied(&track.id, &other.id))
        })
    }

    fn earlier_phases_done(&self, track: &Track, node: &Node) -> bool {
        let Some(phase) = track.phase_of(&node.id) else {
            return true;
        };
        track
            .phases
            .iter()
            .filter(|earlier| earlier.order < phase.order)
            .flat_map(|earlier| earlier.nodes.iter())
            .all(|earlier| self.is_satisfied(&track.id, &earlier.id))
    }

    pub fn track_xp(&self, track: &Track) -> u32 {
        track
            .nodes()
            .map(|node| self.earned_xp(&track.id, &node.id))
            .sum()
    }

    pub fn current_belt<'a>(&self, track: &'a Track) -> Option<&'a Belt> {
        let xp = self.track_xp(track);
        track.belts.iter().rev().find(|belt| xp >= belt.required_xp)
    }

    pub fn next_belt<'a>(&self, track: &'a Track) -> Option<&'a Belt> {
        let xp = self.track_xp(track);
        track.belts.iter().find(|belt| xp < belt.required_xp)
    }
}

/// Why a learner cannot simply tick `node`.
fn learner_refusal(node: &Node) -> Refusal {
    if node.under_construction() {
        Refusal::UnderConstruction
    } else if node.kind == NodeKind::Lesson {
        Refusal::NeedsQuiz
    } else {
        Refusal::NeedsEvidence(node.kind)
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::track::fixtures::{node, track};

    fn day(d: u32) -> NaiveDate {
        NaiveDate::from_ymd_opt(2026, 11, d).unwrap()
    }

    fn lesson(id: &str, prerequisites: &[&str], xp: u32) -> Node {
        Node {
            kind: NodeKind::Lesson,
            ..node(id, prerequisites, xp)
        }
    }

    fn two_lesson_track() -> Track {
        track(vec![
            vec![lesson("rust.p0.setup", &[], 100)],
            vec![lesson("rust.p1.loops", &["rust.p0.setup"], 250)],
        ])
    }

    fn with_skill(mut node: Node, skill: &str) -> Node {
        node.objectives = vec![crate::track::Objective {
            id: "o1".into(),
            text: "learn".into(),
            skills: vec![skill.into()],
        }];
        node
    }

    /// p0: setup · p1: loops (skill s.loops), maps (s.maps), checkpoint · p2: traits
    fn placement_track() -> Track {
        let mut rust = track(vec![
            vec![lesson("rust.p0.setup", &[], 50)],
            vec![
                with_skill(lesson("rust.p1.loops", &[], 100), "s.loops"),
                with_skill(lesson("rust.p1.maps", &[], 100), "s.maps"),
                Node {
                    kind: NodeKind::Checkpoint,
                    ..node("rust.p1.checkpoint", &[], 300)
                },
            ],
            vec![lesson("rust.p2.traits", &[], 100)],
        ]);
        rust.placement = Some(crate::placement::PlacementConfig {
            entry_prompt: String::new(),
            entry_options: vec![],
            phases: Default::default(),
            early_stop: crate::placement::EarlyStop { wrong: 2, of: 3 },
            skill_threshold: 0.75,
            weights_by_difficulty: Default::default(),
            max_items: 10,
            items: vec![],
        });
        rust
    }

    fn placed(profile: &mut Profile, rust: &Track) {
        let plan = crate::placement::PlacementPlan {
            placed_out: vec![
                "rust.p0.setup".into(),
                "rust.p1.loops".into(),
                "rust.p1.maps".into(),
            ],
            belt_exam: Some("rust.p1.checkpoint".into()),
        };
        assert!(profile.can_take_placement(rust));
        for event in profile.apply_placement(rust, &plan, day(14)) {
            profile.apply(&event);
        }
    }

    #[test]
    fn placed_out_nodes_unlock_what_follows_but_give_no_xp_until_the_belt_exam() {
        let rust = placement_track();
        let mut profile = fresh_profile();
        placed(&mut profile, &rust);

        let checkpoint = rust.node("rust.p1.checkpoint").unwrap();
        assert!(profile.is_unlocked(&rust, checkpoint));
        assert!(!profile.is_unlocked(&rust, rust.node("rust.p2.traits").unwrap()));
        assert_eq!(profile.total_xp, 0);
        assert_eq!(profile.belt_exam("rust"), Some("rust.p1.checkpoint"));
        assert!(
            !profile.can_take_placement(&rust),
            "only once, on a fresh track"
        );

        complete(&mut profile, &rust, "rust.p1.checkpoint", day(15));
        let confirmations = profile.placement_confirmations(&rust, checkpoint, day(15));
        assert_eq!(confirmations.len(), 3);
        for event in &confirmations {
            profile.apply(event);
        }
        assert_eq!(profile.total_xp, 300 + 50 + 100 + 100);
        assert!(!profile.is_placed_out("rust", "rust.p1.loops"));
        assert!(profile.is_completed("rust", "rust.p1.loops"));
        assert_eq!(profile.belt_exam("rust"), None);
    }

    #[test]
    fn a_failed_belt_exam_sends_back_only_the_nodes_of_the_missed_skills() {
        let rust = placement_track();
        let mut profile = fresh_profile();
        placed(&mut profile, &rust);
        let checkpoint = rust.node("rust.p1.checkpoint").unwrap();

        let missed: HashSet<String> = ["s.maps".to_string()].into();
        for event in profile.placement_revocations(&rust, checkpoint, &missed, day(15)) {
            profile.apply(&event);
        }

        assert!(profile.is_placed_out("rust", "rust.p1.loops"));
        assert!(!profile.is_placed_out("rust", "rust.p1.maps"));
        assert!(!profile.is_unlocked(&rust, checkpoint), "study maps first");
        assert!(profile.is_unlocked(&rust, rust.node("rust.p1.maps").unwrap()));
    }

    #[test]
    fn a_failed_quiz_elsewhere_revokes_nothing() {
        let rust = placement_track();
        let mut profile = fresh_profile();
        placed(&mut profile, &rust);
        let missed: HashSet<String> = ["s.maps".to_string()].into();
        let traits = rust.node("rust.p2.traits").unwrap();
        assert!(
            profile
                .placement_revocations(&rust, traits, &missed, day(15))
                .is_empty()
        );
    }

    #[test]
    fn a_checkpoint_opens_after_its_phase_and_needs_quiz_and_kata() {
        let checkpoint = Node {
            kind: NodeKind::Checkpoint,
            ..node("rust.p1.checkpoint", &[], 200)
        };
        let rust = track(vec![vec![lesson("rust.p1.loops", &[], 100), checkpoint]]);
        let checkpoint = rust.node("rust.p1.checkpoint").unwrap();
        let mut profile = fresh_profile();
        assert!(
            !profile.is_unlocked(&rust, checkpoint),
            "the phase is not finished"
        );

        complete(&mut profile, &rust, "rust.p1.loops", day(14));
        assert!(profile.is_unlocked(&rust, checkpoint));

        let half = Evidence::Checkpoint {
            quiz_passed: true,
            checks_passed: false,
        };
        assert_eq!(
            profile.complete_by_evidence(&rust, checkpoint, half, day(14)),
            Err(Refusal::NotPassed)
        );
        let both = Evidence::Checkpoint {
            quiz_passed: true,
            checks_passed: true,
        };
        assert!(
            profile
                .complete_by_evidence(&rust, checkpoint, both, day(14))
                .is_ok()
        );
    }

    #[test]
    fn a_phase_opens_only_when_every_node_of_the_earlier_phases_is_done() {
        let rust = track(vec![
            vec![
                lesson("rust.p0.setup", &[], 100),
                lesson("rust.p0.hello", &[], 100),
            ],
            vec![lesson("rust.p1.loops", &["rust.p0.setup"], 250)],
        ]);
        let loops = rust.node("rust.p1.loops").unwrap();
        let mut profile = fresh_profile();

        complete(&mut profile, &rust, "rust.p0.setup", day(14));
        assert!(
            !profile.is_unlocked(&rust, loops),
            "rust.p0.hello is still open in the earlier phase"
        );

        complete(&mut profile, &rust, "rust.p0.hello", day(14));
        assert!(profile.is_unlocked(&rust, loops));
    }

    fn fresh_profile() -> Profile {
        Profile::new(Settings::new("rust"))
    }

    fn complete(profile: &mut Profile, track: &Track, node_id: &str, on: NaiveDate) {
        let node = track.node(node_id).unwrap();
        let event = profile
            .complete_by_evidence(track, node, passed_quiz(), on)
            .unwrap();
        profile.apply(&event);
    }

    fn passed_quiz() -> Evidence<'static> {
        Evidence::Quiz(QuizScore {
            correct: 4,
            total: 5,
            passed: true,
        })
    }

    fn kata_with_hints(id: &str, xp: u32, hints: usize) -> Node {
        let mut kata = node(id, &[], xp);
        kata.kata = Some(crate::track::Kata {
            statement: String::new(),
            starter_dir: Default::default(),
            tests_dir: Default::default(),
            reference_dir: Default::default(),
            runner: crate::track::RunnerSpec {
                image: "dojo-rust".into(),
                timeout_secs: 60,
            },
            hints: (1..=hints).map(|level| format!("hint {level}")).collect(),
        });
        kata
    }

    fn reveal(profile: &mut Profile, track: &Track, node_id: &str) -> Result<u8, HintRefusal> {
        let event = profile.request_hint(track, track.node(node_id).unwrap(), day(14))?;
        profile.apply(&event);
        Ok(profile.hints_revealed(&track.id, node_id))
    }

    #[test]
    fn each_open_hint_costs_ten_percent_of_the_kata_xp() {
        let rust = track(vec![vec![kata_with_hints("rust.p0.kata", 200, 3)]]);
        let mut profile = fresh_profile();

        assert_eq!(reveal(&mut profile, &rust, "rust.p0.kata"), Ok(1));
        assert_eq!(reveal(&mut profile, &rust, "rust.p0.kata"), Ok(2));
        assert_eq!(
            profile.xp_for("rust", rust.node("rust.p0.kata").unwrap()),
            160
        );

        complete(&mut profile, &rust, "rust.p0.kata", day(14));
        assert_eq!(profile.total_xp, 160);
        assert_eq!(profile.track_xp(&rust), 160);
        assert_eq!(profile.earned_xp("rust", "rust.p0.kata"), 160);

        assert_eq!(
            reveal(&mut profile, &rust, "rust.p0.kata"),
            Ok(3),
            "after the kata is done, hints are free"
        );
        assert_eq!(profile.total_xp, 160);
        assert_eq!(
            reveal(&mut profile, &rust, "rust.p0.kata"),
            Err(HintRefusal::AllRevealed)
        );
    }

    #[test]
    fn hints_need_an_open_kata_that_has_them() {
        let rust = track(vec![
            vec![lesson("rust.p0.setup", &[], 50)],
            vec![kata_with_hints("rust.p1.kata", 100, 2)],
        ]);
        let mut profile = fresh_profile();
        assert_eq!(
            reveal(&mut profile, &rust, "rust.p0.setup"),
            Err(HintRefusal::NoHints)
        );
        assert_eq!(
            reveal(&mut profile, &rust, "rust.p1.kata"),
            Err(HintRefusal::Locked)
        );
    }

    #[test]
    fn a_reset_returns_the_xp_actually_earned_and_clears_the_hints() {
        let rust = track(vec![vec![kata_with_hints("rust.p0.kata", 100, 3)]]);
        let mut profile = fresh_profile();
        reveal(&mut profile, &rust, "rust.p0.kata").unwrap();
        complete(&mut profile, &rust, "rust.p0.kata", day(14));
        assert_eq!(profile.total_xp, 90);

        let kata = rust.node("rust.p0.kata").unwrap();
        let reset = profile
            .request_reset(&rust, kata, day(14), Mode::Dev)
            .unwrap();
        profile.apply(&reset);
        assert_eq!(profile.total_xp, 0);
        assert_eq!(profile.hints_revealed("rust", "rust.p0.kata"), 0);
    }

    fn quiz_lesson(id: &str, questions: &[&str]) -> Node {
        use crate::quiz::{Question, QuestionKind, Quiz};
        let mut node = lesson(id, &[], 100);
        node.quiz = Some(Quiz {
            pass_percent: 80,
            questions: questions
                .iter()
                .map(|question| Question {
                    id: question.to_string(),
                    prompt: String::new(),
                    code: None,
                    kind: QuestionKind::Text {
                        answers: vec!["x".into()],
                    },
                    explanation: String::new(),
                    skills: vec![],
                })
                .collect(),
        });
        node
    }

    fn answer(
        profile: &mut Profile,
        node_id: &str,
        question_id: &str,
        correct: bool,
        on: NaiveDate,
    ) {
        profile.apply(&ProgressEvent::ReviewAnswered {
            track_id: "rust".into(),
            node_id: node_id.into(),
            question_id: question_id.into(),
            correct,
            on,
        });
    }

    #[test]
    fn finished_questions_come_back_for_review_and_misses_come_back_first() {
        let rust = track(vec![vec![
            quiz_lesson("rust.p0.a", &["a1", "a2"]),
            quiz_lesson("rust.p0.b", &["b1"]),
        ]]);
        let mut profile = fresh_profile();
        complete(&mut profile, &rust, "rust.p0.a", day(1));
        assert!(
            profile.due_reviews(&rust, day(1)).is_empty(),
            "not on the same day"
        );
        assert_eq!(profile.due_reviews(&rust, day(2)).len(), 2, "b is not done");
        assert_eq!(profile.next_review(&rust), Some(day(2)));

        answer(&mut profile, "rust.p0.a", "a1", true, day(2));
        answer(&mut profile, "rust.p0.a", "a2", false, day(2));
        assert_eq!(profile.practice_xp, crate::review::REVIEW_XP);
        assert_eq!(profile.total_xp, 100, "practice XP stays apart");

        let due: Vec<_> = profile
            .due_reviews(&rust, day(3))
            .into_iter()
            .map(|item| item.question_id)
            .collect();
        assert_eq!(due, vec!["a2"], "a1 waits three days");

        complete(&mut profile, &rust, "rust.p0.b", day(4));
        let due: Vec<_> = profile
            .due_reviews(&rust, day(5))
            .into_iter()
            .map(|item| item.question_id)
            .collect();
        assert_eq!(
            due,
            vec!["a2", "b1", "a1"],
            "lowest step first, then the oldest"
        );
    }

    #[test]
    fn a_review_answer_counts_as_training_for_the_streak() {
        let rust = track(vec![vec![quiz_lesson("rust.p0.a", &["a1"])]]);
        let mut profile = fresh_profile();
        complete(&mut profile, &rust, "rust.p0.a", day(1));
        answer(&mut profile, "rust.p0.a", "a1", true, day(2));
        assert_eq!(profile.streak_on(day(2)), 2);
    }

    #[test]
    fn locked_node_is_refused() {
        let rust = two_lesson_track();
        let loops = rust.node("rust.p1.loops").unwrap();

        let result = fresh_profile().request_completion(&rust, loops, day(14), Mode::Learner);

        assert_eq!(result, Err(Refusal::Locked));
    }

    #[test]
    fn completing_a_lesson_unlocks_the_next_and_grants_xp() {
        let rust = two_lesson_track();
        let mut profile = fresh_profile();

        complete(&mut profile, &rust, "rust.p0.setup", day(14));

        assert!(profile.is_unlocked(&rust, rust.node("rust.p1.loops").unwrap()));
        assert_eq!(profile.track_xp(&rust), 100);
        assert_eq!(profile.total_xp, 100);
    }

    #[test]
    fn learner_cannot_tick_anything_and_unfinished_nodes_say_so() {
        use crate::quiz::Quiz;
        let mut ready = lesson("rust.p0.ready", &[], 100);
        ready.lesson = Some("# Ready".into());
        ready.quiz = Some(Quiz {
            pass_percent: 80,
            questions: vec![],
        });
        let rust = track(vec![vec![ready, lesson("rust.p0.draft", &[], 100)]]);
        let profile = fresh_profile();

        let ask = |id: &str| {
            profile.request_completion(&rust, rust.node(id).unwrap(), day(14), Mode::Learner)
        };

        assert_eq!(ask("rust.p0.ready"), Err(Refusal::NeedsQuiz));
        assert_eq!(ask("rust.p0.draft"), Err(Refusal::UnderConstruction));
    }

    #[test]
    fn learner_cannot_tick_a_kata_but_dev_can() {
        let rust = track(vec![vec![node("rust.p0.kata", &[], 100)]]);
        let kata = rust.node("rust.p0.kata").unwrap();
        let profile = fresh_profile();

        assert_eq!(
            profile.request_completion(&rust, kata, day(14), Mode::Learner),
            Err(Refusal::UnderConstruction),
            "a kata node without its exercise is still being written"
        );
        assert!(
            profile
                .request_completion(&rust, kata, day(14), Mode::Dev)
                .is_ok()
        );
    }

    #[test]
    fn kata_is_completed_only_by_a_passing_check() {
        use crate::check::{CheckOutcome, CheckReport};
        let rust = track(vec![vec![node("rust.p0.kata", &[], 120)]]);
        let kata = rust.node("rust.p0.kata").unwrap();
        let mut profile = fresh_profile();
        let failing = CheckReport {
            outcome: CheckOutcome::TestsFailed,
            tests: vec![],
            details: String::new(),
            duration_ms: 5,
        };
        let passing = CheckReport {
            outcome: CheckOutcome::Passed,
            ..failing.clone()
        };

        assert_eq!(
            profile.complete_by_evidence(&rust, kata, Evidence::Checks(&failing), day(14)),
            Err(Refusal::NotPassed)
        );
        let event = profile
            .complete_by_evidence(&rust, kata, Evidence::Checks(&passing), day(14))
            .unwrap();
        profile.apply(&event);

        assert_eq!(profile.total_xp, 120);
        assert_eq!(
            profile.complete_by_evidence(&rust, kata, Evidence::Checks(&passing), day(14)),
            Err(Refusal::AlreadyCompleted)
        );
    }

    #[test]
    fn reset_is_dev_only_and_removes_xp() {
        let rust = two_lesson_track();
        let setup = rust.node("rust.p0.setup").unwrap();
        let mut profile = fresh_profile();
        complete(&mut profile, &rust, "rust.p0.setup", day(14));

        assert_eq!(
            profile.request_reset(&rust, setup, day(14), Mode::Learner),
            Err(Refusal::DevOnly)
        );
        let reset = profile
            .request_reset(&rust, setup, day(14), Mode::Dev)
            .unwrap();
        profile.apply(&reset);

        assert!(!profile.is_completed("rust", "rust.p0.setup"));
        assert_eq!(profile.total_xp, 0);
        assert_eq!(
            profile.request_reset(&rust, setup, day(14), Mode::Dev),
            Err(Refusal::NotCompleted)
        );
    }

    #[test]
    fn completing_twice_is_refused_and_replayed_duplicates_count_once() {
        let rust = two_lesson_track();
        let setup = rust.node("rust.p0.setup").unwrap();
        let mut profile = fresh_profile();
        complete(&mut profile, &rust, "rust.p0.setup", day(14));

        assert_eq!(
            profile.request_completion(&rust, setup, day(14), Mode::Dev),
            Err(Refusal::AlreadyCompleted)
        );
        let duplicate = ProgressEvent::NodeCompleted {
            track_id: "rust".into(),
            node_id: "rust.p0.setup".into(),
            xp: 100,
            on: day(14),
        };
        profile.apply(&duplicate);
        assert_eq!(profile.total_xp, 100);
    }

    #[test]
    fn belts_follow_track_xp() {
        let rust = two_lesson_track();
        let mut profile = fresh_profile();
        assert_eq!(profile.current_belt(&rust).unwrap().name, "White Belt");

        complete(&mut profile, &rust, "rust.p0.setup", day(14));
        complete(&mut profile, &rust, "rust.p1.loops", day(14));

        assert_eq!(profile.current_belt(&rust).unwrap().name, "Yellow Belt");
        assert_eq!(profile.next_belt(&rust).unwrap().name, "Green Belt");
    }

    #[test]
    fn streak_grows_on_consecutive_days_restarts_after_a_gap_and_expires() {
        let rust = two_lesson_track();
        let mut profile = fresh_profile();

        complete(&mut profile, &rust, "rust.p0.setup", day(10));
        complete(&mut profile, &rust, "rust.p1.loops", day(11));
        assert_eq!(profile.streak_on(day(11)), 2);
        assert_eq!(profile.streak_on(day(12)), 2, "still alive the next day");
        assert_eq!(profile.streak_on(day(13)), 0, "broken after a full day off");
        assert_eq!(profile.best_streak, 2);
    }

    #[test]
    fn replay_starts_from_the_imported_v1_streak() {
        let mut settings = Settings::new("rust");
        settings.streak_baseline = StreakBaseline {
            current: 5,
            best: 9,
            last_trained: Some(day(13)),
        };
        let event = ProgressEvent::NodeCompleted {
            track_id: "rust".into(),
            node_id: "rust.p0.setup".into(),
            xp: 100,
            on: day(14),
        };

        let profile = Profile::replay(settings, &[event]);

        assert_eq!(profile.streak_on(day(14)), 6);
        assert_eq!(profile.best_streak, 9);
        assert_eq!(profile.total_xp, 100);
    }
}
