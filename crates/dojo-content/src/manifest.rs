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
