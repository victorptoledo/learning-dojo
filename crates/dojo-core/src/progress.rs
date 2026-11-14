//! Learner progress, rebuilt from an append-only list of events.
//!
//! The store keeps the events; `Profile` is the in-memory projection used by the UI.
//! `Profile::apply` is the only way progress changes, so replaying the stored events
//! always reproduces the same state.

use crate::check::CheckReport;
use crate::quiz::QuizScore;
use crate::track::{Belt, Node, NodeKind, Track};
use chrono::NaiveDate;
use std::collections::{HashMap, HashSet};

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
}

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
    current_streak: u32,
    completed: HashMap<String, HashSet<String>>,
    placed_out: HashMap<String, HashSet<String>>,
    belt_exams: HashMap<String, String>,
}

impl Profile {
    pub fn new(settings: Settings) -> Self {
        let baseline = settings.streak_baseline.clone();
        Self {
            settings,
            total_xp: 0,
            best_streak: baseline.best,
            last_trained: baseline.last_trained,
            current_streak: baseline.current,
            completed: HashMap::new(),
            placed_out: HashMap::new(),
            belt_exams: HashMap::new(),
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
                let newly_completed = self
                    .completed
                    .entry(track_id.clone())
                    .or_default()
                    .insert(node_id.clone());
                if newly_completed {
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
                track_id,
                node_id,
                xp,
                ..
            } => {
                let removed = self
                    .completed
                    .get_mut(track_id)
                    .is_some_and(|nodes| nodes.remove(node_id));
                if removed {
                    self.total_xp = self.total_xp.saturating_sub(*xp);
                }
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
            xp: node.xp,
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
            xp: node.xp,
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
            xp: node.xp,
            on: today,
        })
    }

    // ---- queries ------------------------------------------------------------

    pub fn is_completed(&self, track_id: &str, node_id: &str) -> bool {
        self.completed
            .get(track_id)
            .is_some_and(|nodes| nodes.contains(node_id))
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
                xp: skipped.xp,
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
            .filter(|node| self.is_completed(&track.id, &node.id))
            .map(|node| node.xp)
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
