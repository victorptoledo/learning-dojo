//! Pure domain model of the Learning Dojo.
//!
//! No file system, terminal or network access lives here: content loading,
//! storage and UI are adapters built on top of these types and the `ports`.

pub mod catalog;
pub mod check;
pub mod placement;
pub mod ports;
pub mod progress;
pub mod quiz;
pub mod requirement;
pub mod track;

pub use catalog::{Catalog, Skill, SkillId};
pub use check::{CheckOutcome, CheckRecord, CheckReport, TestCase};
pub use placement::{
    EarlyStop, EntryOption, PhaseRule, PhaseVerdict, PlacementAnswer, PlacementConfig,
    PlacementItem, PlacementPlan, PlacementSession,
};
pub use ports::{KataRunner, ProgressStore, RunnerError, StoreError};
pub use progress::{Evidence, Mode, Profile, ProgressEvent, Refusal, Settings, StreakBaseline};
pub use quiz::{
    Answer, DEFAULT_PASS_PERCENT, Question, QuestionKind, Quiz, QuizAttempt, QuizScore,
};
pub use requirement::{
    Requirement, RequirementKind, merge as merge_requirements, version_at_least,
};
pub use track::{
    Archetype, Belt, Bridge, ContentStatus, GENERIC_BRIDGE, Kata, Node, NodeId, NodeKind,
    Objective, Phase, PhaseId, RunnerSpec, Track, TrackId,
};
