use serde::{Deserialize, Serialize};
use std::collections::BTreeMap;
use std::path::PathBuf;

use crate::placement::PlacementConfig;
use crate::quiz::Quiz;
use crate::requirement::{Requirement, merge};

pub type TrackId = String;
pub type PhaseId = String;
pub type NodeId = String;

/// Source-language key of the bridge shown when the learner's language has none.
pub const GENERIC_BRIDGE: &str = "generic";

/// How a track is taught and how its katas are checked.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "lowercase")]
pub enum Archetype {
    Language,
    Platform,
    Concept,
    Role,
}

/// Editorial state of a piece of content. Only `Reviewed` content is meant for learners.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "lowercase")]
pub enum ContentStatus {
    Draft,
    Reviewed,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "lowercase")]
pub enum NodeKind {
    Lesson,
    Kata,
    Quiz,
    Checkpoint,
    Project,
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct Belt {
    pub name: String,
    pub required_xp: u32,
    pub title: String,
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct Objective {
    pub id: String,
    pub text: String,
    pub skills: Vec<String>,
}

/// Rosetta bridge: the node's concept explained from a given source language.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct Bridge {
    pub from: String,
    pub concept: String,
    pub from_way: String,
    pub native_way: String,
    pub sensei_tip: String,
    pub body: String,
}

/// Docker image and limits used to check a kata.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct RunnerSpec {
    pub image: String,
    pub timeout_secs: u32,
}

/// A hands-on exercise: the learner edits a copy of `starter_dir`; `tests_dir`
/// holds the grading tests, copied into the check container only.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct Kata {
    /// Markdown statement shown in the TUI.
    pub statement: String,
    pub starter_dir: PathBuf,
    pub tests_dir: PathBuf,
    pub reference_dir: PathBuf,
    pub runner: RunnerSpec,
    /// The hint ladder, opened one level at a time: a question that points the way,
    /// then the concept and where it applies, then a partial skeleton.
    pub hints: Vec<String>,
}

/// The language a kata is written in, known from its runner image.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum KataLanguage {
    Rust,
    Java,
    /// Bash scripts checked with bats and shellcheck in `dojo-linux`.
    Bash,
}

impl Kata {
    pub fn language(&self) -> KataLanguage {
        match self.runner.image.as_str() {
            "dojo-java" => KataLanguage::Java,
            "dojo-linux" => KataLanguage::Bash,
            _ => KataLanguage::Rust,
        }
    }
}

/// An open question answered in writing and reviewed by an AI the learner chooses.
/// It is feedback only: it never gates progress.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct Exercise {
    /// What to write, in Markdown.
    pub task: String,
    /// What a good answer covers; the review prompt grades against these.
    pub criteria: Vec<String>,
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct Node {
    pub id: NodeId,
    pub legacy_id: Option<String>,
    pub kind: NodeKind,
    pub status: ContentStatus,
    pub title: String,
    pub summary: String,
    pub objectives: Vec<Objective>,
    pub prerequisites: Vec<NodeId>,
    pub difficulty: u8,
    pub estimated_minutes: u32,
    pub xp: u32,
    pub lesson: Option<String>,
    /// The lesson document on disk, opened in the editor.
    pub lesson_file: Option<PathBuf>,
    /// Bridges keyed by source language (`java`, `python`, ..., `generic`).
    pub bridges: BTreeMap<String, Bridge>,
    pub kata: Option<Kata>,
    pub quiz: Option<Quiz>,
    /// An optional written exercise reviewed by an AI.
    pub exercise: Option<Exercise>,
    /// What this node needs on top of the track's requirements.
    pub requirements: Vec<Requirement>,
}

impl Node {
    /// Bridge for the learner's source language, falling back to the generic one.
    pub fn bridge_for(&self, source_language: &str) -> Option<&Bridge> {
        self.bridges
            .get(source_language)
            .or_else(|| self.bridges.get(GENERIC_BRIDGE))
    }

    /// True while the node lacks what a learner needs to complete it: a lesson
    /// needs its document and quiz, a kata its exercise, a checkpoint both a quiz
    /// and a kata. Standalone quiz and project nodes have no engine yet.
    pub fn under_construction(&self) -> bool {
        match self.kind {
            NodeKind::Lesson => self.lesson.is_none() || self.quiz.is_none(),
            NodeKind::Kata => self.kata.is_none(),
            // A checkpoint reviews the phase with a quiz and an integration kata.
            NodeKind::Checkpoint => self.quiz.is_none() || self.kata.is_none(),
            NodeKind::Quiz | NodeKind::Project => true,
        }
    }

    pub fn skills(&self) -> impl Iterator<Item = &str> {
        self.objectives
            .iter()
            .flat_map(|objective| objective.skills.iter().map(String::as_str))
    }
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct Phase {
    pub id: PhaseId,
    pub order: u8,
    pub title: String,
    pub goal: String,
    pub nodes: Vec<Node>,
}

impl Phase {
    pub fn checkpoint(&self) -> Option<&Node> {
        self.nodes
            .iter()
            .find(|node| node.kind == NodeKind::Checkpoint)
    }
}

#[derive(Debug, Clone, PartialEq)]
pub struct Track {
    pub id: TrackId,
    pub name: String,
    pub archetype: Archetype,
    pub category: String,
    pub icon: String,
    pub description: String,
    pub status: ContentStatus,
    pub belts: Vec<Belt>,
    pub phases: Vec<Phase>,
    /// What every node of the track needs.
    pub requirements: Vec<Requirement>,
    /// The placement test, when the track has one.
    pub placement: Option<PlacementConfig>,
    /// Technical interview questions, when the track has them.
    pub interview: Option<crate::InterviewBank>,
}

impl Track {
    pub fn nodes(&self) -> impl Iterator<Item = &Node> {
        self.phases.iter().flat_map(|phase| phase.nodes.iter())
    }

    pub fn node(&self, node_id: &str) -> Option<&Node> {
        self.nodes().find(|node| node.id == node_id)
    }

    pub fn phase_of(&self, node_id: &str) -> Option<&Phase> {
        self.phases
            .iter()
            .find(|phase| phase.nodes.iter().any(|node| node.id == node_id))
    }

    pub fn node_count(&self) -> usize {
        self.phases.iter().map(|phase| phase.nodes.len()).sum()
    }

    /// Everything `node` needs: the track's requirements plus the node's own.
    pub fn requirements_for<'a>(&'a self, node: &'a Node) -> Vec<&'a Requirement> {
        merge(&self.requirements, &node.requirements)
    }

    pub fn total_xp(&self) -> u32 {
        self.nodes().map(|node| node.xp).sum()
    }
}

#[cfg(test)]
pub(crate) mod fixtures {
    use super::*;

    pub fn node(id: &str, prerequisites: &[&str], xp: u32) -> Node {
        Node {
            id: id.to_string(),
            legacy_id: None,
            kind: NodeKind::Kata,
            status: ContentStatus::Draft,
            title: id.to_string(),
            summary: String::new(),
            objectives: vec![],
            prerequisites: prerequisites.iter().map(|p| p.to_string()).collect(),
            difficulty: 1,
            estimated_minutes: 10,
            xp,
            lesson: None,
            lesson_file: None,
            bridges: BTreeMap::new(),
            kata: None,
            quiz: None,
            exercise: None,
            requirements: vec![],
        }
    }

    pub fn bridge(from: &str) -> Bridge {
        Bridge {
            from: from.to_string(),
            concept: format!("concept from {from}"),
            from_way: String::new(),
            native_way: String::new(),
            sensei_tip: String::new(),
            body: String::new(),
        }
    }

    pub fn track(phases: Vec<Vec<Node>>) -> Track {
        Track {
            id: "rust".to_string(),
            name: "Rust".to_string(),
            archetype: Archetype::Language,
            category: "Programming Languages".to_string(),
            icon: "🦀".to_string(),
            description: String::new(),
            status: ContentStatus::Draft,
            belts: vec![
                Belt {
                    name: "White Belt".into(),
                    required_xp: 0,
                    title: "Novice".into(),
                },
                Belt {
                    name: "Yellow Belt".into(),
                    required_xp: 300,
                    title: "Apprentice".into(),
                },
                Belt {
                    name: "Green Belt".into(),
                    required_xp: 800,
                    title: "Adept".into(),
                },
            ],
            phases: phases
                .into_iter()
                .enumerate()
                .map(|(order, nodes)| Phase {
                    id: format!("p{order}"),
                    order: order as u8,
                    title: format!("Phase {order}"),
                    goal: String::new(),
                    nodes,
                })
                .collect(),
            requirements: vec![],
            placement: None,
            interview: None,
        }
    }
}

#[cfg(test)]
mod tests {
    use super::fixtures::{bridge, node, track};
    use super::*;

    #[test]
    fn bridge_prefers_the_learner_language() {
        let mut loops = node("rust.p1.loops", &[], 100);
        loops.bridges.insert("java".into(), bridge("java"));
        loops
            .bridges
            .insert(GENERIC_BRIDGE.into(), bridge(GENERIC_BRIDGE));

        assert_eq!(loops.bridge_for("java").unwrap().from, "java");
    }

    #[test]
    fn bridge_falls_back_to_generic_then_to_none() {
        let mut loops = node("rust.p1.loops", &[], 100);
        assert!(loops.bridge_for("python").is_none());

        loops
            .bridges
            .insert(GENERIC_BRIDGE.into(), bridge(GENERIC_BRIDGE));
        assert_eq!(loops.bridge_for("python").unwrap().from, GENERIC_BRIDGE);
    }

    #[test]
    fn track_finds_nodes_and_their_phase_across_phases() {
        let rust = track(vec![
            vec![node("rust.p0.setup", &[], 100)],
            vec![node("rust.p1.loops", &["rust.p0.setup"], 150)],
        ]);

        assert_eq!(rust.node_count(), 2);
        assert_eq!(rust.total_xp(), 250);
        assert_eq!(rust.phase_of("rust.p1.loops").unwrap().id, "p1");
        assert!(rust.node("rust.p9.missing").is_none());
    }
}
