//! Placement test: finds the phases a learner already masters so they can skip them.
//!
//! Milestone-based per phase and adaptive within a phase (D004): an entry question picks
//! the first phase to test, each phase draws a few questions stratified by skill, a
//! phase that is mastered moves the test on, and anything else ends it. Mastery is
//! tracked per skill with Beta counts. Skips are provisional: the plan names a
//! checkpoint (the Belt Exam) whose pass confirms them.
//!
//! Pure and deterministic: the same seed and answers always give the same plan.

use crate::quiz::{Answer, Question};
use crate::track::{NodeId, NodeKind, PhaseId, Track};
use std::collections::{BTreeMap, HashMap};

/// Parameters and question bank of a track's placement test (`placement.yaml`).
#[derive(Debug, Clone, PartialEq)]
pub struct PlacementConfig {
    pub entry_prompt: String,
    pub entry_options: Vec<EntryOption>,
    /// Rules for the phases that can be tested; other phases are never tested.
    pub phases: BTreeMap<PhaseId, PhaseRule>,
    pub early_stop: EarlyStop,
    /// Minimum skill mastery for a node of a partial phase to be skipped.
    pub skill_threshold: f64,
    /// Weight of an answer by item difficulty (1 to 5).
    pub weights_by_difficulty: BTreeMap<u8, f64>,
    pub max_items: usize,
    pub items: Vec<PlacementItem>,
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct EntryOption {
    pub label: String,
    /// Phases before this one are skipped without a test.
    pub start_phase: PhaseId,
    /// False: no test at all, the learner starts at `start_phase`.
    pub test: bool,
}

#[derive(Debug, Clone, Copy, PartialEq)]
pub struct PhaseRule {
    pub items: usize,
    /// Weighted score from which the phase counts as mastered.
    pub pass: f64,
    /// Weighted score from which the phase counts as partially known.
    pub partial: f64,
}

/// `wrong` wrong answers among the first `of` items of a phase end the test there.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub struct EarlyStop {
    pub wrong: usize,
    pub of: usize,
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct PlacementItem {
    pub phase: PhaseId,
    pub difficulty: u8,
    /// The question; its `skills` say what a right or wrong answer is evidence of.
    pub question: Question,
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub enum PlacementAnswer {
    Given(Answer),
    /// No penalty beyond counting as not known; it discourages guessing.
    DontKnow,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum PhaseVerdict {
    Mastered,
    Partial,
    NotYet,
}

/// What the test decided: nodes to skip and the checkpoint that confirms them.
#[derive(Debug, Clone, Default, PartialEq, Eq)]
pub struct PlacementPlan {
    pub placed_out: Vec<NodeId>,
    pub belt_exam: Option<NodeId>,
}

impl PlacementPlan {
    pub fn is_empty(&self) -> bool {
        self.placed_out.is_empty()
    }
}

/// Beta(α, β) counts per skill, starting from a neutral Beta(1, 1).
#[derive(Debug, Clone, Default)]
struct SkillMastery {
    counts: HashMap<String, (f64, f64)>,
}

impl SkillMastery {
    fn record(&mut self, skill: &str, correct: bool, weight: f64) {
        let (alpha, beta) = self.counts.entry(skill.to_string()).or_insert((1.0, 1.0));
        if correct {
            *alpha += weight;
        } else {
            *beta += weight;
        }
    }

    fn was_tested(&self, skill: &str) -> bool {
        self.counts.contains_key(skill)
    }

    fn mastery(&self, skill: &str) -> f64 {
        let (alpha, beta) = self.counts.get(skill).copied().unwrap_or((1.0, 1.0));
        alpha / (alpha + beta)
    }
}

#[derive(Debug, Clone, Copy)]
struct Response {
    correct: bool,
    weight: f64,
}

/// One run of the placement test for one track.
#[derive(Debug, Clone)]
pub struct PlacementSession {
    config: PlacementConfig,
    /// Ordered ids of the track's phases.
    phase_ids: Vec<PhaseId>,
    /// Index in `phase_ids` of the first phase that is not skipped by the entry answer.
    start: usize,
    /// Index in `phase_ids` of the phase being tested.
    current: usize,
    queue: Vec<usize>,
    position: usize,
    responses: Vec<Response>,
    verdicts: Vec<(PhaseId, PhaseVerdict)>,
    mastery: SkillMastery,
    asked: usize,
    rng: SplitMix64,
    finished: bool,
}

impl PlacementSession {
    /// Starts the test after the entry question; `entry` indexes `entry_options`.
    pub fn new(track: &Track, config: &PlacementConfig, entry: usize, seed: u64) -> Self {
        let phase_ids: Vec<PhaseId> = track.phases.iter().map(|phase| phase.id.clone()).collect();
        let option = config.entry_options.get(entry);
        let start = option
            .and_then(|option| phase_ids.iter().position(|id| *id == option.start_phase))
            .unwrap_or(0);
        let mut session = Self {
            config: config.clone(),
            phase_ids,
            start,
            current: start,
            queue: Vec::new(),
            position: 0,
            responses: Vec::new(),
            verdicts: Vec::new(),
            mastery: SkillMastery::default(),
            asked: 0,
            rng: SplitMix64(seed),
            finished: !option.is_some_and(|option| option.test),
        };
        if !session.finished {
            session.open_testable_phase();
        }
        session
    }

    /// The question to answer now, or `None` when the test is over.
    pub fn current(&self) -> Option<&PlacementItem> {
        if self.finished {
            return None;
        }
        self.queue
            .get(self.position)
            .map(|&index| &self.config.items[index])
    }

    pub fn is_finished(&self) -> bool {
        self.finished
    }

    /// Id of the phase being tested.
    pub fn current_phase(&self) -> Option<&str> {
        (!self.finished).then(|| self.phase_ids[self.current].as_str())
    }

    /// Questions answered so far, over all phases.
    pub fn asked(&self) -> usize {
        self.asked
    }

    /// Position in the current phase (1-based) and how many questions it has.
    pub fn progress_in_phase(&self) -> (usize, usize) {
        (self.position + 1, self.queue.len())
    }

    pub fn verdicts(&self) -> &[(PhaseId, PhaseVerdict)] {
        &self.verdicts
    }

    /// Records the answer to the current question and moves on. Returns whether it was right.
    pub fn answer(&mut self, answer: &PlacementAnswer) -> bool {
        let Some(item) = self.current().cloned() else {
            return false;
        };
        let correct = match answer {
            PlacementAnswer::Given(given) => item.question.is_correct(given),
            PlacementAnswer::DontKnow => false,
        };
        let weight = self.weight(item.difficulty);
        for skill in &item.question.skills {
            self.mastery.record(skill, correct, weight);
        }
        self.responses.push(Response { correct, weight });
        self.asked += 1;
        self.position += 1;
        self.advance();
        correct
    }

    fn weight(&self, difficulty: u8) -> f64 {
        self.config
            .weights_by_difficulty
            .get(&difficulty)
            .copied()
            .unwrap_or(1.0)
    }

    fn advance(&mut self) {
        let wrong = self.responses.iter().filter(|r| !r.correct).count();
        let early_stop = self.responses.len() <= self.config.early_stop.of
            && wrong >= self.config.early_stop.wrong;
        if early_stop {
            return self.finish_phase(PhaseVerdict::NotYet);
        }
        if self.asked >= self.config.max_items || self.position >= self.queue.len() {
            let verdict = self.score_verdict();
            let out_of_items = self.asked >= self.config.max_items;
            self.finish_phase(verdict);
            if out_of_items {
                self.finished = true;
            }
        }
    }

    fn score_verdict(&self) -> PhaseVerdict {
        let total: f64 = self.responses.iter().map(|r| r.weight).sum();
        if total == 0.0 {
            return PhaseVerdict::NotYet;
        }
        let right: f64 = self
            .responses
            .iter()
            .filter(|r| r.correct)
            .map(|r| r.weight)
            .sum();
        let score = right / total;
        let rule = self.config.phases[&self.phase_ids[self.current]];
        if score >= rule.pass {
            PhaseVerdict::Mastered
        } else if score >= rule.partial {
            PhaseVerdict::Partial
        } else {
            PhaseVerdict::NotYet
        }
    }

    fn finish_phase(&mut self, verdict: PhaseVerdict) {
        self.verdicts
            .push((self.phase_ids[self.current].clone(), verdict));
        if verdict != PhaseVerdict::Mastered {
            self.finished = true;
            return;
        }
        self.current += 1;
        self.open_testable_phase();
    }

    /// Draws the questions of the current phase. A phase that cannot be tested (no rule
    /// or no questions) ends the test: it is where the learner starts.
    fn open_testable_phase(&mut self) {
        let queue = self
            .phase_ids
            .get(self.current)
            .cloned()
            .and_then(|phase| {
                let rule = self.config.phases.get(&phase).copied()?;
                Some(self.draw(&phase, rule.items))
            })
            .unwrap_or_default();
        if queue.is_empty() {
            self.finished = true;
            return;
        }
        self.queue = queue;
        self.position = 0;
        self.responses.clear();
    }

    /// Up to `count` items of `phase`, taken in turn from each skill so every skill is asked.
    fn draw(&mut self, phase: &str, count: usize) -> Vec<usize> {
        let mut by_skill: BTreeMap<&str, Vec<usize>> = BTreeMap::new();
        for (index, item) in self.config.items.iter().enumerate() {
            if item.phase == phase {
                let skill = item.question.skills.first().map_or("", String::as_str);
                by_skill.entry(skill).or_default().push(index);
            }
        }
        let mut groups: Vec<Vec<usize>> = by_skill.into_values().collect();
        for group in &mut groups {
            self.rng.shuffle(group);
        }
        self.rng.shuffle(&mut groups);
        let mut drawn = Vec::new();
        for round in 0.. {
            let before = drawn.len();
            for group in &groups {
                if drawn.len() == count {
                    return drawn;
                }
                if let Some(&index) = group.get(round) {
                    drawn.push(index);
                }
            }
            if drawn.len() == before {
                break;
            }
        }
        drawn
    }

    /// A node of a partial phase is skipped when every skill the test measured is
    /// mastered. Skills the test never asked about neither help nor block.
    fn knows(&self, node: &crate::track::Node) -> bool {
        let mut tested = node
            .skills()
            .filter(|skill| self.mastery.was_tested(skill))
            .peekable();
        tested.peek().is_some()
            && tested.all(|skill| self.mastery.mastery(skill) >= self.config.skill_threshold)
    }

    /// Nodes to skip and the Belt Exam that confirms them, from the verdicts so far.
    pub fn plan(&self, track: &Track) -> PlacementPlan {
        let verdict_of = |phase: &str| {
            self.verdicts
                .iter()
                .find(|(id, _)| id == phase)
                .map(|(_, verdict)| *verdict)
        };
        let mut placed_out: Vec<NodeId> = Vec::new();
        let mut fully_skipped_checkpoints: Vec<NodeId> = Vec::new();
        let mut last_skipped_order = None;
        for (index, phase) in track.phases.iter().enumerate() {
            let skipped_whole =
                index < self.start || verdict_of(&phase.id) == Some(PhaseVerdict::Mastered);
            let partial = verdict_of(&phase.id) == Some(PhaseVerdict::Partial);
            for node in &phase.nodes {
                let skip = skipped_whole
                    || (partial && node.kind != NodeKind::Checkpoint && self.knows(node));
                if skip {
                    placed_out.push(node.id.clone());
                    last_skipped_order = Some(phase.order);
                    if node.kind == NodeKind::Checkpoint {
                        fully_skipped_checkpoints.push(node.id.clone());
                    }
                }
            }
        }
        let belt_exam = fully_skipped_checkpoints.last().cloned().or_else(|| {
            let after = last_skipped_order?;
            track
                .phases
                .iter()
                .filter(|phase| phase.order >= after)
                .find_map(|phase| phase.checkpoint())
                .map(|node| node.id.clone())
        });
        placed_out.retain(|id| Some(id) != belt_exam.as_ref());
        PlacementPlan {
            placed_out,
            belt_exam: belt_exam.filter(|_| last_skipped_order.is_some()),
        }
    }
}

/// Small deterministic generator (SplitMix64), enough to shuffle a question bank.
#[derive(Debug, Clone)]
struct SplitMix64(u64);

impl SplitMix64 {
    fn next(&mut self) -> u64 {
        self.0 = self.0.wrapping_add(0x9E37_79B9_7F4A_7C15);
        let mut z = self.0;
        z = (z ^ (z >> 30)).wrapping_mul(0xBF58_476D_1CE4_E5B9);
        z = (z ^ (z >> 27)).wrapping_mul(0x94D0_49BB_1331_11EB);
        z ^ (z >> 31)
    }

    fn shuffle<T>(&mut self, items: &mut [T]) {
        for i in (1..items.len()).rev() {
            let j = (self.next() % (i as u64 + 1)) as usize;
            items.swap(i, j);
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::quiz::QuestionKind;
    use crate::track::fixtures::{node, track};
    use crate::track::{Node, Objective};

    fn lesson(id: &str, prerequisites: &[&str], xp: u32) -> Node {
        Node {
            kind: NodeKind::Lesson,
            ..node(id, prerequisites, xp)
        }
    }

    fn with_skill(mut node: Node, skill: &str) -> Node {
        node.objectives = vec![Objective {
            id: "o1".into(),
            text: "learn it".into(),
            skills: vec![skill.into()],
        }];
        node
    }

    fn checkpoint(id: &str) -> Node {
        Node {
            kind: NodeKind::Checkpoint,
            ..node(id, &[], 300)
        }
    }

    /// p0: setup · p1: a (skill s.a), b (skill s.b), checkpoint · p2: c (s.c), checkpoint · p3: d (s.d)
    fn sample_track() -> Track {
        track(vec![
            vec![lesson("t.p0.setup", &[], 50)],
            vec![
                with_skill(lesson("t.p1.a", &[], 100), "s.a"),
                with_skill(lesson("t.p1.b", &[], 100), "s.b"),
                checkpoint("t.p1.checkpoint"),
            ],
            vec![
                with_skill(lesson("t.p2.c", &[], 100), "s.c"),
                checkpoint("t.p2.checkpoint"),
            ],
            vec![with_skill(lesson("t.p3.d", &[], 100), "s.d")],
        ])
    }

    fn item(id: &str, phase: &str, skill: &str) -> PlacementItem {
        PlacementItem {
            phase: phase.into(),
            difficulty: 2,
            question: Question {
                id: id.into(),
                prompt: format!("{id}?"),
                code: None,
                kind: QuestionKind::SingleChoice {
                    options: vec!["no".into(), "yes".into()],
                    answer: 1,
                },
                explanation: String::new(),
                skills: vec![skill.into()],
            },
        }
    }

    fn config() -> PlacementConfig {
        let rule = PhaseRule {
            items: 4,
            pass: 0.8,
            partial: 0.5,
        };
        let mut items = Vec::new();
        for n in 0..3 {
            items.push(item(&format!("a{n}"), "p1", "s.a"));
            items.push(item(&format!("b{n}"), "p1", "s.b"));
            items.push(item(&format!("c{n}"), "p2", "s.c"));
            items.push(item(&format!("d{n}"), "p3", "s.d"));
        }
        PlacementConfig {
            entry_prompt: "How familiar are you?".into(),
            entry_options: vec![
                EntryOption {
                    label: "never".into(),
                    start_phase: "p0".into(),
                    test: false,
                },
                EntryOption {
                    label: "a bit".into(),
                    start_phase: "p1".into(),
                    test: true,
                },
            ],
            phases: [
                ("p1".to_string(), rule),
                ("p2".to_string(), rule),
                ("p3".to_string(), rule),
            ]
            .into(),
            early_stop: EarlyStop { wrong: 2, of: 3 },
            skill_threshold: 0.75,
            weights_by_difficulty: [(1, 0.5), (2, 1.0), (3, 1.5)].into(),
            max_items: 25,
            items,
        }
    }

    const RIGHT: PlacementAnswer = PlacementAnswer::Given(Answer::Choice(1));
    const WRONG: PlacementAnswer = PlacementAnswer::Given(Answer::Choice(0));

    /// Answers each question with `answer(question id)` until the test ends.
    fn run(session: &mut PlacementSession, answer: impl Fn(&str) -> PlacementAnswer) {
        while let Some(item) = session.current() {
            let response = answer(&item.question.id);
            session.answer(&response);
        }
    }

    #[test]
    fn never_used_it_skips_nothing_and_asks_nothing() {
        let track = sample_track();
        let session = PlacementSession::new(&track, &config(), 0, 7);
        assert!(session.is_finished());
        assert!(session.plan(&track).is_empty());
    }

    #[test]
    fn each_phase_draws_every_skill_and_the_same_seed_draws_the_same_items() {
        let track = sample_track();
        let first = PlacementSession::new(&track, &config(), 1, 42);
        let again = PlacementSession::new(&track, &config(), 1, 42);
        let ids = |s: &PlacementSession| -> Vec<String> {
            s.queue
                .iter()
                .map(|&i| s.config.items[i].question.id.clone())
                .collect()
        };
        assert_eq!(ids(&first), ids(&again));
        assert_eq!(first.queue.len(), 4);
        let skills: Vec<_> = ids(&first)
            .iter()
            .map(|id| id.chars().next().unwrap())
            .collect();
        assert_eq!(skills.iter().filter(|s| **s == 'a').count(), 2);
        assert_eq!(skills.iter().filter(|s| **s == 'b').count(), 2);
    }

    #[test]
    fn two_wrong_in_the_first_three_stop_the_test_and_only_p0_is_skipped() {
        let track = sample_track();
        let mut session = PlacementSession::new(&track, &config(), 1, 1);
        session.answer(&WRONG);
        session.answer(&PlacementAnswer::DontKnow);
        assert!(session.is_finished());
        assert_eq!(
            session.verdicts(),
            [("p1".to_string(), PhaseVerdict::NotYet)]
        );
        let plan = session.plan(&track);
        assert_eq!(plan.placed_out, ["t.p0.setup"]);
        assert_eq!(plan.belt_exam.as_deref(), Some("t.p1.checkpoint"));
    }

    #[test]
    fn mastered_phases_are_skipped_and_the_last_skipped_checkpoint_is_the_belt_exam() {
        let track = sample_track();
        let mut session = PlacementSession::new(&track, &config(), 1, 3);
        run(&mut session, |id| {
            if id.starts_with('d') { WRONG } else { RIGHT }
        });
        assert_eq!(
            session.verdicts(),
            [
                ("p1".to_string(), PhaseVerdict::Mastered),
                ("p2".to_string(), PhaseVerdict::Mastered),
                ("p3".to_string(), PhaseVerdict::NotYet),
            ]
        );
        let plan = session.plan(&track);
        assert_eq!(
            plan.placed_out,
            [
                "t.p0.setup",
                "t.p1.a",
                "t.p1.b",
                "t.p1.checkpoint",
                "t.p2.c"
            ]
        );
        assert_eq!(plan.belt_exam.as_deref(), Some("t.p2.checkpoint"));
    }

    #[test]
    fn a_partial_phase_skips_only_nodes_whose_skills_are_mastered() {
        let track = sample_track();
        let mut session = PlacementSession::new(&track, &config(), 1, 9);
        // Right on s.a, wrong on s.b after the first three: 3 of 4 → partial (0.75).
        let mut b_seen = 0;
        while let Some(item) = session.current() {
            let id = item.question.id.clone();
            let response = if id.starts_with('b') {
                b_seen += 1;
                if b_seen == 2 { WRONG } else { RIGHT }
            } else {
                RIGHT
            };
            session.answer(&response);
        }
        assert_eq!(
            session.verdicts(),
            [("p1".to_string(), PhaseVerdict::Partial)]
        );
        let plan = session.plan(&track);
        assert_eq!(plan.placed_out, ["t.p0.setup", "t.p1.a"]);
        assert_eq!(plan.belt_exam.as_deref(), Some("t.p1.checkpoint"));
    }

    #[test]
    fn the_item_cap_ends_the_test() {
        let track = sample_track();
        let mut capped = config();
        capped.max_items = 6;
        let mut session = PlacementSession::new(&track, &capped, 1, 5);
        run(&mut session, |_| RIGHT);
        assert_eq!(session.asked(), 6);
        assert_eq!(session.verdicts().len(), 2);
    }
}
