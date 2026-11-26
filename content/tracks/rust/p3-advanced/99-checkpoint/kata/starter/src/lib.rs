//! Checkpoint 3: Build Planner. See README.md for the rules.

use std::fmt;
use std::sync::Arc;

// ---- Part 1: parsing without copying ------------------------------------------------------

/// One line of a build file: a target and the targets it depends on.
/// Every `&str` must point into the source text: no `String` here.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct Target<'a> {
    pub name: &'a str,
    pub deps: Vec<&'a str>,
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub enum ParseError {
    MissingColon { line: usize },
    EmptyName { line: usize },
    Duplicate { line: usize, name: String },
}

impl fmt::Display for ParseError {
    fn fmt(&self, _f: &mut fmt::Formatter<'_>) -> fmt::Result {
        todo!("the three messages in the README")
    }
}

impl std::error::Error for ParseError {}

pub fn parse(_source: &str) -> Result<Vec<Target<'_>>, ParseError> {
    todo!("one Target per line, borrowing the names from the source")
}

// ---- Part 2: the plan, still borrowed -----------------------------------------------------

#[derive(Debug, Clone, PartialEq, Eq)]
pub enum PlanError {
    UnknownDependency { target: String, dependency: String },
    Cycle { targets: Vec<String> },
}

impl fmt::Display for PlanError {
    fn fmt(&self, _f: &mut fmt::Formatter<'_>) -> fmt::Result {
        todo!("the two messages in the README")
    }
}

impl std::error::Error for PlanError {}

/// Groups the targets into waves: each wave only depends on earlier ones.
/// Read the signature: the names live as long as the source (`'a`), not as long as `targets`.
pub fn plan<'a>(_targets: &[Target<'a>]) -> Result<Vec<Vec<&'a str>>, PlanError> {
    todo!("waves of target names, each wave sorted")
}

// ---- Part 3: the build, in the background -------------------------------------------------

/// The work for one target. Shared by every worker thread, so it lives behind an `Arc`.
pub type Job = Arc<dyn Fn(&str) -> Result<u64, String> + Send + Sync>;

#[derive(Debug, Clone, PartialEq, Eq, Default)]
pub struct Report {
    /// Targets whose job returned `Ok`, sorted by name.
    pub built: Vec<(String, u64)>,
    /// Targets whose job returned `Err` or panicked, sorted by name.
    pub failed: Vec<(String, String)>,
    /// Targets whose job never started, sorted.
    pub skipped: Vec<String>,
}

/// A running build. Replace the placeholder field with what the build needs to share
/// with its threads.
pub struct Build {
    _todo: (),
}

pub fn start(_waves: Vec<Vec<String>>, _workers: usize, _job: Job) -> Build {
    todo!("start the build on background threads and return at once")
}

impl Build {
    /// How many jobs have returned so far (built or failed).
    pub fn finished(&self) -> usize {
        todo!()
    }

    /// Jobs already running finish; jobs not started yet are skipped.
    pub fn cancel(&self) {
        todo!()
    }

    /// Waits for the build to end and returns its report.
    pub fn wait(self) -> Report {
        todo!()
    }
}
