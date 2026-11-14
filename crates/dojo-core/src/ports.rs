//! Ports implemented by adapters (storage today; runner and reviewer later).

use crate::progress::{ProgressEvent, Settings};

pub type StoreError = Box<dyn std::error::Error + Send + Sync>;

/// Durable home of learner progress: settings plus an append-only event log.
pub trait ProgressStore {
    /// `None` when nothing has been saved yet (first run).
    fn load_settings(&self) -> Result<Option<Settings>, StoreError>;
    fn save_settings(&mut self, settings: &Settings) -> Result<(), StoreError>;
    /// Events in the order they were recorded.
    fn load_events(&self) -> Result<Vec<ProgressEvent>, StoreError>;
    fn append_event(&mut self, event: &ProgressEvent) -> Result<(), StoreError>;
}
