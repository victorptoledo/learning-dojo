use crate::career::Career;
use crate::sidequest::Sidequest;
use crate::track::Track;
use std::collections::BTreeMap;

pub type SkillId = String;

/// An entry of the shared skill taxonomy (`rust.control-flow.loops`, `core.http`, ...).
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct Skill {
    pub id: SkillId,
    pub title: String,
    /// More general skills this one counts towards, usually in the `core` namespace.
    pub maps_to: Vec<SkillId>,
}

/// Everything the dojo can teach: tracks, the skill taxonomy they reference, the
/// career paths built from them and the sidequests.
#[derive(Debug, Clone, Default)]
pub struct Catalog {
    pub tracks: Vec<Track>,
    pub skills: BTreeMap<SkillId, Skill>,
    pub careers: Vec<Career>,
    /// Optional projects, each listed on its main track.
    pub sidequests: Vec<Sidequest>,
}

impl Catalog {
    pub fn track(&self, track_id: &str) -> Option<&Track> {
        self.tracks.iter().find(|track| track.id == track_id)
    }

    pub fn career(&self, career_id: &str) -> Option<&Career> {
        self.careers.iter().find(|career| career.id == career_id)
    }

    pub fn track_index(&self, track_id: &str) -> Option<usize> {
        self.tracks.iter().position(|track| track.id == track_id)
    }

    /// Maps every node's v1 ID to its current ID, for migrating saved progress.
    pub fn legacy_id_map(&self) -> BTreeMap<String, String> {
        self.tracks
            .iter()
            .flat_map(|track| track.nodes())
            .filter_map(|node| Some((node.legacy_id.clone()?, node.id.clone())))
            .collect()
    }
}
