//! Content adapter: reads `content/` (YAML manifests, Markdown, JSON Schemas)
//! into the `dojo-core` catalog and reports everything that is wrong with it.

mod front_matter;
mod loader;
mod manifest;
mod report;
mod schema;
mod validate;

pub use report::{Issue, Report, Severity};

use anyhow::Result;
use dojo_core::Catalog;
use std::path::Path;

/// A catalog plus every problem found while loading and validating it.
#[derive(Debug)]
pub struct LoadedContent {
    pub catalog: Catalog,
    pub report: Report,
}

/// Loads `root` (the `content/` folder) and runs all validation rules.
///
/// Fails only when the content cannot be read at all (missing folder or schemas);
/// problems inside individual files end up in the report.
pub fn load(root: &Path) -> Result<LoadedContent> {
    let mut report = Report::default();
    let catalog = loader::load_catalog(root, &mut report)?;
    validate::check_catalog(&catalog, &mut report);
    Ok(LoadedContent { catalog, report })
}
