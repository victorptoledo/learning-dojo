//! Pure domain model of the Learning Dojo.
//!
//! No file system, terminal or network access lives here: content loading,
//! storage and UI are adapters built on top of these types and the `ports`.

pub mod career;
pub mod catalog;
pub mod check;
pub mod interview;
pub mod placement;
pub mod ports;
pub mod progress;
pub mod quiz;
pub mod requirement;
pub mod review;
pub mod rng;
pub mod sidequest;
pub mod track;

pub use career::{
    Career, CareerProgress, CareerRequirement, Reason, Recommendation, Stage, recommend,
    skill_mastery,
};
pub use catalog::{Catalog, Skill, SkillId};
pub use check::{CheckOutcome, CheckRecord, CheckReport, TestCase};
pub use interview::{InterviewBank, InterviewLevel, InterviewQuestion};
pub use placement::{
    EarlyStop, EntryOption, PhaseRule, PhaseVerdict, PlacementAnswer, PlacementConfig,
    PlacementItem, PlacementPlan, PlacementSession,
};
pub use ports::{KataRunner, ProgressStore, RunnerError, StoreError};
pub use progress::{
    Evidence, HINT_COST_PERCENT, HintRefusal, Mode, Profile, ProgressEvent, Refusal, Settings,
    StreakBaseline,
};
pub use quiz::{
    Answer, DEFAULT_PASS_PERCENT, Question, QuestionKind, Quiz, QuizAttempt, QuizScore,
};
pub use requirement::{
    Requirement, RequirementKind, merge as merge_requirements, version_at_least,
};
pub use review::{Card, ReviewItem};
pub use sidequest::{
    CheckPart, Milestone, SIDEQUEST_STAGE_XP, Sidequest, SidequestRequirement, SidequestStage,
};
pub use track::{
    Archetype, Belt, Bridge, ContentStatus, Exercise, GENERIC_BRIDGE, Kata, KataLanguage, Node,
    NodeId, NodeKind, Objective, Phase, PhaseId, REMEDIAL_XP, Remedial, RunnerSpec, Track, TrackId,
};
