//! Parses the human output of `cargo test` (stable libtest) into a check report.

use dojo_core::{CheckOutcome, CheckReport, TestCase};
use regex::Regex;
use std::sync::LazyLock;

const MAX_DETAIL_LINES: usize = 40;

static TEST_LINE: LazyLock<Regex> =
    LazyLock::new(|| Regex::new(r"^test (\S+) \.\.\. (ok|FAILED|ignored)").expect("valid regex"));
static COMPILE_ERROR: LazyLock<Regex> =
    LazyLock::new(|| Regex::new(r"^error(\[E\d{4}\])?: ").expect("valid regex"));

/// Turns the combined stdout/stderr of `cargo test` into a report.
///
/// `exit_ok` is whether the process exited with status 0.
pub fn parse_cargo_test(output: &str, exit_ok: bool, duration_ms: u64) -> CheckReport {
    let tests = parse_test_lines(output);
    let any_failed = tests.iter().any(|test| !test.passed);
    let outcome = if any_failed {
        CheckOutcome::TestsFailed
    } else if exit_ok && !tests.is_empty() {
        CheckOutcome::Passed
    } else {
        CheckOutcome::CompileError
    };
    let details = match outcome {
        CheckOutcome::TestsFailed => failure_details(output),
        CheckOutcome::CompileError => compile_details(output),
        CheckOutcome::Passed | CheckOutcome::TimedOut => String::new(),
    };
    CheckReport {
        outcome,
        tests,
        details,
        duration_ms,
    }
}

fn parse_test_lines(output: &str) -> Vec<TestCase> {
    output
        .lines()
        .filter_map(|line| TEST_LINE.captures(line.trim_end()))
        .filter(|caps| &caps[2] != "ignored")
        .map(|caps| TestCase {
            name: caps[1].to_string(),
            passed: &caps[2] == "ok",
        })
        .collect()
}

/// The `---- name stdout ----` blocks: panic messages with expected and actual values.
fn failure_details(output: &str) -> String {
    let lines: Vec<&str> = output
        .lines()
        .skip_while(|line| !line.starts_with("---- "))
        .take_while(|line| line.trim_end() != "failures:")
        .filter(|line| !line.starts_with("note: run with `RUST_BACKTRACE"))
        .collect();
    cap_lines(&lines)
}

/// From the first compiler error to the end of the compiler's messages.
fn compile_details(output: &str) -> String {
    let lines: Vec<&str> = output
        .lines()
        .skip_while(|line| !COMPILE_ERROR.is_match(line))
        .collect();
    if lines.is_empty() {
        let tail: Vec<&str> = output.lines().rev().take(MAX_DETAIL_LINES).collect();
        return tail.into_iter().rev().collect::<Vec<_>>().join("\n");
    }
    cap_lines(&lines)
}

fn cap_lines(lines: &[&str]) -> String {
    let mut text = lines
        .iter()
        .take(MAX_DETAIL_LINES)
        .copied()
        .collect::<Vec<_>>()
        .join("\n");
    if lines.len() > MAX_DETAIL_LINES {
        text.push_str(&format!(
            "\n… {} more lines",
            lines.len() - MAX_DETAIL_LINES
        ));
    }
    text.trim().to_string()
}

#[cfg(test)]
mod tests {
    use super::*;

    const PASSING: &str = "\
   Compiling kata v0.1.0 (/work)
    Finished `test` profile [unoptimized + debuginfo] target(s) in 1.20s
     Running tests/grading.rs (/target/debug/deps/grading-1)

running 2 tests
test greets_another_name ... ok
test ignores_surrounding_spaces ... ok

test result: ok. 2 passed; 0 failed; 0 ignored; 0 measured; 0 filtered out; finished in 0.00s

     Running tests/visible.rs (/target/debug/deps/visible-2)

running 1 test
test greets_by_name ... ok

test result: ok. 1 passed; 0 failed; 0 ignored; 0 measured; 0 filtered out; finished in 0.00s
";

    const FAILING: &str = "\
running 2 tests
test greets_by_name ... FAILED
test slow_one ... ignored
test greets_another_name ... ok

failures:

---- greets_by_name stdout ----

thread 'greets_by_name' panicked at tests/visible.rs:5:5:
assertion `left == right` failed
  left: \"Hello Ferris\"
 right: \"Welcome to the dojo, Ferris!\"
note: run with `RUST_BACKTRACE=1` environment variable to display a backtrace


failures:
    greets_by_name

test result: FAILED. 1 passed; 1 failed; 1 ignored; 0 measured; 0 filtered out
error: test failed, to rerun pass `--test visible`
";

    const NOT_COMPILING: &str = "\
   Compiling kata v0.1.0 (/work)
error[E0308]: mismatched types
 --> src/lib.rs:2:5
  |
2 |     42
  |     ^^ expected `String`, found integer

error: could not compile `kata` (lib) due to 1 previous error
";

    #[test]
    fn all_tests_ok_is_a_pass() {
        let report = parse_cargo_test(PASSING, true, 900);

        assert_eq!(report.outcome, CheckOutcome::Passed);
        assert_eq!(report.tests.len(), 3);
        assert_eq!(report.summary(), "3/3 tests passed");
        assert!(report.details.is_empty());
    }

    #[test]
    fn failed_tests_keep_the_panic_message_and_skip_ignored_ones() {
        let report = parse_cargo_test(FAILING, false, 900);

        assert_eq!(report.outcome, CheckOutcome::TestsFailed);
        assert_eq!(report.summary(), "1/2 tests passed");
        assert!(
            report
                .details
                .starts_with("---- greets_by_name stdout ----")
        );
        assert!(
            report
                .details
                .contains("right: \"Welcome to the dojo, Ferris!\"")
        );
        assert!(!report.details.contains("RUST_BACKTRACE"));
    }

    #[test]
    fn compiler_errors_are_reported_from_the_first_error() {
        let report = parse_cargo_test(NOT_COMPILING, false, 900);

        assert_eq!(report.outcome, CheckOutcome::CompileError);
        assert!(report.tests.is_empty());
        assert!(report.details.starts_with("error[E0308]: mismatched types"));
    }

    #[test]
    fn zero_tests_with_success_is_not_a_pass() {
        let report = parse_cargo_test("running 0 tests\n\ntest result: ok. 0 passed", true, 10);
        assert_eq!(report.outcome, CheckOutcome::CompileError);
    }

    #[test]
    fn long_details_are_capped() {
        let noisy = format!("error: boom\n{}", "line\n".repeat(100));
        let report = parse_cargo_test(&noisy, false, 10);
        assert!(report.details.ends_with("more lines"));
        assert!(report.details.lines().count() <= MAX_DETAIL_LINES + 1);
    }
}
