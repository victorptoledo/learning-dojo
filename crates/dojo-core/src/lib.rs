//! Pure domain model of the Learning Dojo.
//!
//! No file system, terminal or network access lives here: content loading,
//! storage and UI are adapters built on top of these types and the `ports`.

pub mod catalog;
pub mod check;
pub mod ports;
pub mod progress;
pub mod track;

pub use catalog::{Catalog, Skill, SkillId};
pub use check::{CheckOutcome, CheckRecord, CheckReport, TestCase};
pub use ports::{KataRunner, ProgressStore, RunnerError, StoreError};
pub use progress::{Mode, Profile, ProgressEvent, Refusal, Settings, StreakBaseline};
pub use track::{
    Archetype, Belt, Bridge, ContentStatus, GENERIC_BRIDGE, Kata, Node, NodeId, NodeKind,
    Objective, Phase, PhaseId, RunnerSpec, Track, TrackId,
};
