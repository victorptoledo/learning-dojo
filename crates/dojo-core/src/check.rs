//! Results of checking a kata solution: the evidence that completes a kata.

use chrono::{DateTime, Utc};

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum CheckOutcome {
    Passed,
    TestsFailed,
    CompileError,
    TimedOut,
}

impl CheckOutcome {
    pub fn as_str(self) -> &'static str {
        match self {
            Self::Passed => "passed",
            Self::TestsFailed => "tests_failed",
            Self::CompileError => "compile_error",
            Self::TimedOut => "timed_out",
        }
    }

    pub fn parse(text: &str) -> Option<Self> {
        match text {
            "passed" => Some(Self::Passed),
            "tests_failed" => Some(Self::TestsFailed),
            "compile_error" => Some(Self::CompileError),
            "timed_out" => Some(Self::TimedOut),
            _ => None,
        }
    }
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct TestCase {
    pub name: String,
    pub passed: bool,
    /// Panic or assertion message of a failing test.
    pub message: Option<String>,
}

/// What one run of a kata's checks produced.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct CheckReport {
    pub outcome: CheckOutcome,
    pub tests: Vec<TestCase>,
    /// Compiler errors or failing-test output, trimmed for display.
    pub details: String,
    pub duration_ms: u64,
}

impl CheckReport {
    pub fn passed_count(&self) -> usize {
        self.tests.iter().filter(|test| test.passed).count()
    }

    pub fn is_pass(&self) -> bool {
        self.outcome == CheckOutcome::Passed
    }

    /// One-line summary such as `4/6 tests passed`.
    pub fn summary(&self) -> String {
        match self.outcome {
            CheckOutcome::CompileError => "does not compile".to_string(),
            CheckOutcome::TimedOut => "timed out".to_string(),
            CheckOutcome::Passed | CheckOutcome::TestsFailed => {
                format!("{}/{} tests passed", self.passed_count(), self.tests.len())
            }
        }
    }
}

/// A stored check run (attempt history).
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct CheckRecord {
    pub track_id: String,
    pub node_id: String,
    pub outcome: CheckOutcome,
    pub passed_tests: u32,
    pub total_tests: u32,
    pub duration_ms: u64,
    pub ran_at: DateTime<Utc>,
}

impl CheckRecord {
    pub fn from_report(
        track_id: &str,
        node_id: &str,
        report: &CheckReport,
        ran_at: DateTime<Utc>,
    ) -> Self {
        Self {
            track_id: track_id.to_string(),
            node_id: node_id.to_string(),
            outcome: report.outcome,
            passed_tests: report.passed_count() as u32,
            total_tests: report.tests.len() as u32,
            duration_ms: report.duration_ms,
            ran_at,
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    fn report(outcome: CheckOutcome, results: &[bool]) -> CheckReport {
        CheckReport {
            outcome,
            tests: results
                .iter()
                .enumerate()
                .map(|(i, passed)| TestCase {
                    name: format!("t{i}"),
                    passed: *passed,
                    message: None,
                })
                .collect(),
            details: String::new(),
            duration_ms: 10,
        }
    }

    #[test]
    fn summary_counts_passing_tests_or_names_the_failure() {
        assert_eq!(
            report(CheckOutcome::TestsFailed, &[true, false, true]).summary(),
            "2/3 tests passed"
        );
        assert_eq!(
            report(CheckOutcome::CompileError, &[]).summary(),
            "does not compile"
        );
        assert_eq!(report(CheckOutcome::TimedOut, &[]).summary(), "timed out");
    }

    #[test]
    fn outcome_round_trips_through_text() {
        for outcome in [
            CheckOutcome::Passed,
            CheckOutcome::TestsFailed,
            CheckOutcome::CompileError,
            CheckOutcome::TimedOut,
        ] {
            assert_eq!(CheckOutcome::parse(outcome.as_str()), Some(outcome));
        }
        assert_eq!(CheckOutcome::parse("nope"), None);
    }
}
