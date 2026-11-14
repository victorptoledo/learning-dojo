//! Storage adapter: implements `dojo_core::ProgressStore` on SQLite and imports v1 profiles.

mod legacy;
mod migrations;
mod sqlite;

pub use legacy::{ImportOutcome, import_v1_profile};
pub use sqlite::SqliteStore;
