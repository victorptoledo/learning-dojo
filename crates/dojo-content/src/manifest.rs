//! On-disk file formats. They mirror the JSON Schemas in `content/schemas/`
//! and are converted into `dojo-core` types by the loader.

use dojo_core::{Archetype, ContentStatus, NodeKind};
use serde::Deserialize;

#[derive(Debug, Deserialize)]
pub struct TrackManifest {
    pub id: String,
    pub name: String,
    pub archetype: Archetype,
    pub category: String,
    pub icon: String,
    pub description: String,
    pub status: ContentStatus,
    pub belts: Vec<BeltManifest>,
    pub phases: Vec<PhaseManifest>,
    #[serde(default)]
    pub requirements: Vec<RequirementManifest>,
}

#[derive(Debug, Deserialize)]
pub struct BeltManifest {
    pub name: String,
    pub required_xp: u32,
    pub title: String,
}

#[derive(Debug, Deserialize)]
pub struct PhaseManifest {
    pub id: String,
    pub dir: String,
    pub title: String,
    pub goal: String,
}

#[derive(Debug, Deserialize)]
pub struct NodeManifest {
    pub id: String,
    #[serde(default)]
    pub legacy_id: Option<String>,
    pub kind: NodeKind,
    pub status: ContentStatus,
    pub title: String,
    pub summary: String,
    pub objectives: Vec<ObjectiveManifest>,
    pub prerequisites: Vec<String>,
    pub difficulty: u8,
    pub estimated_minutes: u32,
    pub xp: u32,
    #[serde(default)]
    pub lesson: Option<String>,
    #[serde(default)]
    pub bridges: Option<String>,
    #[serde(default)]
    pub kata: Option<KataManifest>,
    #[serde(default)]
    pub quiz: Option<String>,
    #[serde(default)]
    pub exercise: Option<ExerciseManifest>,
    #[serde(default)]
    pub requirements: Vec<RequirementManifest>,
}

#[derive(Debug, Deserialize)]
pub struct ExerciseManifest {
    pub task: String,
    pub criteria: Vec<String>,
}

#[derive(Debug, Deserialize)]
pub struct RequirementManifest {
    pub kind: String,
    pub name: String,
    pub why: String,
    #[serde(default = "required_by_default")]
    pub required: bool,
    #[serde(default)]
    pub command: Option<String>,
    #[serde(default)]
    pub version_args: Vec<String>,
    #[serde(default)]
    pub min_version: Option<String>,
    #[serde(default)]
    pub install_hint: Option<String>,
    #[serde(default)]
    pub update: Option<Vec<String>>,
    #[serde(default)]
    pub id: Option<String>,
    #[serde(default)]
    pub image: Option<String>,
}

fn required_by_default() -> bool {
    true
}

#[derive(Debug, Deserialize)]
pub struct QuizManifest {
    #[serde(default)]
    pub pass_percent: Option<u8>,
    pub questions: Vec<QuestionManifest>,
}

#[derive(Debug, Deserialize)]
pub struct QuestionManifest {
    pub id: String,
    pub prompt: String,
    #[serde(default)]
    pub code: Option<String>,
    #[serde(default)]
    pub options: Option<Vec<String>>,
    #[serde(default)]
    pub answer: Option<usize>,
    #[serde(default)]
    pub answers: Option<Vec<String>>,
    pub explanation: String,
    #[serde(default)]
    pub skills: Vec<String>,
}

#[derive(Debug, Deserialize)]
pub struct PlacementManifest {
    pub entry_question: EntryQuestionManifest,
    pub phases: std::collections::BTreeMap<String, PhaseRuleManifest>,
    pub early_stop: EarlyStopManifest,
    pub skill_placed_out_threshold: f64,
    pub weights_by_difficulty: std::collections::BTreeMap<u8, f64>,
    pub max_items: usize,
    pub items: Vec<PlacementItemManifest>,
}

#[derive(Debug, Deserialize)]
pub struct EntryQuestionManifest {
    pub prompt: String,
    pub options: Vec<EntryOptionManifest>,
}

#[derive(Debug, Deserialize)]
pub struct EntryOptionManifest {
    pub label: String,
    pub start_phase: String,
    pub test: bool,
}

#[derive(Debug, Deserialize)]
pub struct PhaseRuleManifest {
    pub items_per_phase: usize,
    pub pass: f64,
    pub partial: f64,
}

#[derive(Debug, Deserialize)]
pub struct EarlyStopManifest {
    pub wrong_in_first: usize,
    pub of: usize,
}

#[derive(Debug, Deserialize)]
pub struct PlacementItemManifest {
    pub phase: String,
    pub difficulty: u8,
    #[serde(flatten)]
    pub question: QuestionManifest,
}

#[derive(Debug, Deserialize)]
pub struct CareerManifest {
    pub id: String,
    pub title: String,
    pub summary: String,
    #[serde(default)]
    pub roles: Vec<String>,
    pub stages: Vec<StageManifest>,
    #[serde(default)]
    pub next_paths: Vec<String>,
}

#[derive(Debug, Deserialize)]
pub struct StageManifest {
    pub id: String,
    pub title: String,
    pub requires: Vec<CareerRequirementManifest>,
}

#[derive(Debug, Deserialize)]
#[serde(untagged)]
pub enum CareerRequirementManifest {
    TrackPhase {
        track: String,
        phase: String,
    },
    Skill {
        skill: String,
        min: f64,
    },
    AnyOf {
        any_of: Vec<CareerRequirementManifest>,
    },
}

#[derive(Debug, Deserialize)]
pub struct KataManifest {
    pub statement: String,
    pub starter: String,
    pub tests: String,
    pub reference: String,
    pub runner: RunnerManifest,
    #[serde(default)]
    pub hints: Vec<String>,
}

#[derive(Debug, Deserialize)]
pub struct RunnerManifest {
    pub image: String,
    pub timeout_secs: u32,
}

#[derive(Debug, Deserialize)]
pub struct ObjectiveManifest {
    pub id: String,
    pub text: String,
    pub skills: Vec<String>,
}

#[derive(Debug, Deserialize)]
pub struct SkillsManifest {
    pub namespace: String,
    pub skills: Vec<SkillManifest>,
}

#[derive(Debug, Deserialize)]
pub struct SkillManifest {
    pub id: String,
    pub title: String,
    #[serde(default)]
    pub maps_to: Vec<String>,
}

#[derive(Debug, Deserialize)]
pub struct BridgeFrontMatter {
    pub from: String,
    pub concept: String,
    pub from_way: String,
    pub native_way: String,
    pub sensei_tip: String,
}
