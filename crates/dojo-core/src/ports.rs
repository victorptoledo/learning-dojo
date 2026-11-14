//! Ports implemented by adapters: storage, kata runner (and the reviewer later).

use crate::check::{CheckRecord, CheckReport};
use crate::progress::{ProgressEvent, Settings};
use crate::quiz::QuizAttempt;
use crate::track::Kata;
use std::path::Path;

pub type StoreError = Box<dyn std::error::Error + Send + Sync>;
pub type RunnerError = Box<dyn std::error::Error + Send + Sync>;

/// Durable home of learner progress: settings, an append-only event log and check history.
pub trait ProgressStore {
    /// `None` when nothing has been saved yet (first run).
    fn load_settings(&self) -> Result<Option<Settings>, StoreError>;
    fn save_settings(&mut self, settings: &Settings) -> Result<(), StoreError>;
    /// Events in the order they were recorded.
    fn load_events(&self) -> Result<Vec<ProgressEvent>, StoreError>;
    fn append_event(&mut self, event: &ProgressEvent) -> Result<(), StoreError>;
    fn append_check(&mut self, record: &CheckRecord) -> Result<(), StoreError>;
    fn latest_check(
        &self,
        track_id: &str,
        node_id: &str,
    ) -> Result<Option<CheckRecord>, StoreError>;
    fn check_count(&self, track_id: &str, node_id: &str) -> Result<u32, StoreError>;
    fn append_quiz_attempt(&mut self, attempt: &QuizAttempt) -> Result<(), StoreError>;
    fn latest_quiz_attempt(
        &self,
        track_id: &str,
        node_id: &str,
    ) -> Result<Option<QuizAttempt>, StoreError>;
}

/// Runs a kata's grading tests against a solution folder.
///
/// `Err` means the checks could not run at all (no Docker, missing image);
/// a failing solution is an `Ok` report with a non-passing outcome.
pub trait KataRunner {
    fn check(&self, kata: &Kata, solution_dir: &Path) -> Result<CheckReport, RunnerError>;
}
