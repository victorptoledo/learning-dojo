//! Pure domain model of the Learning Dojo.
//!
//! No file system, terminal or network access lives here: content loading,
//! storage and UI are adapters built on top of these types and the `ports`.

pub mod catalog;
pub mod ports;
pub mod progress;
pub mod track;

pub use catalog::{Catalog, Skill, SkillId};
pub use ports::{ProgressStore, StoreError};
pub use progress::{Mode, Profile, ProgressEvent, Refusal, Settings, StreakBaseline};
pub use track::{
    Archetype, Belt, Bridge, ContentStatus, GENERIC_BRIDGE, Node, NodeId, NodeKind, Objective,
    Phase, PhaseId, Track, TrackId,
};
