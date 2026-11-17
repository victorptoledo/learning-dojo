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

impl CheckReport {
    /// One report for a check made of several runs (a sidequest stage checked in more
    /// than one image). The worst outcome wins: a compile error, then a timeout, then a
    /// failing test. Each run's details get a header naming it.
    pub fn combine(parts: Vec<(String, CheckReport)>) -> CheckReport {
        if parts.len() == 1
            && let Some((_, report)) = parts.first()
        {
            return report.clone();
        }
        let rank = |outcome: CheckOutcome| match outcome {
            CheckOutcome::CompileError => 3,
            CheckOutcome::TimedOut => 2,
            CheckOutcome::TestsFailed => 1,
            CheckOutcome::Passed => 0,
        };
        let mut combined = CheckReport {
            outcome: CheckOutcome::Passed,
            tests: Vec::new(),
            details: String::new(),
            duration_ms: 0,
        };
        for (label, report) in parts {
            if rank(report.outcome) > rank(combined.outcome) {
                combined.outcome = report.outcome;
            }
            if !report.details.trim().is_empty() {
                if !combined.details.is_empty() {
                    combined.details.push('\n');
                }
                combined
                    .details
                    .push_str(&format!("── {label} ──\n{}", report.details));
            }
            combined.tests.extend(report.tests);
            combined.duration_ms += report.duration_ms;
        }
        combined
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
    fn combined_reports_keep_every_test_and_the_worst_outcome() {
        let mut rust = report(CheckOutcome::TestsFailed, &[true, false]);
        rust.details = "assertion failed".into();
        let devops = report(CheckOutcome::Passed, &[true]);
        let combined = CheckReport::combine(vec![
            ("dojo-rust".into(), rust.clone()),
            ("dojo-devops".into(), devops.clone()),
        ]);
        assert_eq!(combined.outcome, CheckOutcome::TestsFailed);
        assert_eq!(combined.summary(), "2/3 tests passed");
        assert_eq!(combined.duration_ms, 20);
        assert!(
            combined
                .details
                .starts_with("── dojo-rust ──\nassertion failed")
        );

        let broken = report(CheckOutcome::CompileError, &[]);
        let combined =
            CheckReport::combine(vec![("a".into(), devops.clone()), ("b".into(), broken)]);
        assert_eq!(combined.outcome, CheckOutcome::CompileError);

        let single = CheckReport::combine(vec![("a".into(), rust.clone())]);
        assert_eq!(single, rust, "one run is reported as it is");
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
