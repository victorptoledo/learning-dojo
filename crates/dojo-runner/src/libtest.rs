//! Parses the human output of `cargo test` (stable libtest) into a check report.

use dojo_core::{CheckOutcome, CheckReport, TestCase};
use regex::Regex;
use std::collections::HashMap;
use std::sync::LazyLock;

const MAX_DETAIL_LINES: usize = 40;

/// `#[should_panic]` tests get ` - should panic` (older toolchains: ` - should
/// panic with message`) after the name; the name is kept without it, as in the
/// `---- name stdout ----` header.
static TEST_LINE: LazyLock<Regex> = LazyLock::new(|| {
    Regex::new(r"^test (\S+)(?: - should panic(?: with message)?)? \.\.\. (ok|FAILED|ignored)")
        .expect("valid regex")
});
static FAILURE_HEADER: LazyLock<Regex> =
    LazyLock::new(|| Regex::new(r"^---- (\S+) stdout ----").expect("valid regex"));
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
    let mut messages = failure_messages(output);
    output
        .lines()
        .filter_map(|line| TEST_LINE.captures(line.trim_end()))
        .filter(|caps| &caps[2] != "ignored")
        .map(|caps| {
            let passed = &caps[2] == "ok";
            TestCase {
                name: caps[1].to_string(),
                passed,
                message: if passed {
                    None
                } else {
                    messages.remove(&caps[1])
                },
            }
        })
        .collect()
}

/// Message of each failing test, from its `---- name stdout ----` block: the
/// lines after `panicked at ...:` up to the first blank line, without the
/// `RUST_BACKTRACE` hint. A `#[should_panic]` test that did not panic has no
/// such line, only `note: test did not panic as expected at ...`.
fn failure_messages(output: &str) -> HashMap<String, String> {
    let mut messages = HashMap::new();
    let mut current: Option<(String, Vec<&str>)> = None;
    for line in output.lines() {
        let header = FAILURE_HEADER.captures(line);
        if (header.is_some() || line.trim_end() == "failures:")
            && let Some((name, block)) = current.take()
        {
            messages.insert(name, block_message(&block));
        }
        if let Some(caps) = header {
            current = Some((caps[1].to_string(), Vec::new()));
        } else if let Some((_, block)) = current.as_mut() {
            block.push(line.trim_end());
        }
    }
    if let Some((name, block)) = current {
        messages.insert(name, block_message(&block));
    }
    messages
}

fn block_message(block: &[&str]) -> String {
    let message: Vec<&str> = match block.iter().position(|line| line.contains("panicked at")) {
        Some(start) => block[start + 1..]
            .iter()
            .take_while(|line| !line.trim().is_empty())
            .filter(|line| !line.starts_with("note: run with `RUST_BACKTRACE"))
            .copied()
            .collect(),
        None => block
            .iter()
            .filter_map(|line| line.strip_prefix("note: "))
            .collect(),
    };
    message.join("\n")
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

    /// What the dojo-java image prints (`runner-images/java/DojoReport.java`).
    const JAVA_FAILING: &str = "
running 2 tests
test GreeterGradingTest::greets_a_stranger ... FAILED
test GreeterGradingTest::greets_by_name ... ok

failures:

---- GreeterGradingTest::greets_a_stranger stdout ----
thread 'GreeterGradingTest::greets_a_stranger' panicked at GreeterGradingTest::greets_a_stranger:
expected: <Welcome to the dojo, stranger!> but was: <Welcome to the dojo, !>

failures:

test result: FAILED. 1 passed; 1 failed
";

    /// What the dojo-linux image prints (`runner-images/linux/tap2cargo.awk`).
    const LINUX_FAILING: &str = "
running 3 tests
test greets_by_name ... FAILED
test greets_a_stranger ... ok
test shellcheck::greet_sh ... FAILED

failures:

---- greets_by_name stdout ----
thread 'greets_by_name' panicked at greets_by_name:
todo: write greet.sh
`assert_success' failed

---- shellcheck::greet_sh stdout ----
thread 'shellcheck::greet_sh' panicked at shellcheck::greet_sh:
greet.sh:3:6: warning: Double quote to prevent globbing and word splitting. [SC2086]

failures:

test result: FAILED. 1 passed; 2 failed
";

    #[test]
    fn reads_the_linux_image_output_like_cargo_test() {
        let report = parse_cargo_test(LINUX_FAILING, false, 7);
        assert_eq!(report.outcome, CheckOutcome::TestsFailed);
        assert_eq!(report.tests.len(), 3);
        assert!(
            report.tests[0]
                .message
                .as_deref()
                .unwrap()
                .starts_with("todo: write greet.sh")
        );
        assert_eq!(report.tests[2].name, "shellcheck::greet_sh");
        assert!(
            report.tests[2]
                .message
                .as_deref()
                .unwrap()
                .contains("SC2086")
        );

        let syntax = parse_cargo_test(
            "error: greet.sh has a syntax error
greet.sh: line 1: syntax error near unexpected token `then'
",
            false,
            7,
        );
        assert_eq!(syntax.outcome, CheckOutcome::CompileError);
        assert!(syntax.details.contains("greet.sh: line 1"));
    }

    /// What the dojo-go image prints (`runner-images/go/report/main.go`).
    const GO_FAILING: &str = "
running 4 tests
test TestGreetsByName ... FAILED
test TestGreet/blank_name ... ok
test go_vet ... ok
test gofmt::greet_go ... FAILED

failures:

---- TestGreetsByName stdout ----
thread 'TestGreetsByName' panicked at TestGreetsByName:
todo: write Greet

---- gofmt::greet_go stdout ----
thread 'gofmt::greet_go' panicked at gofmt::greet_go:
greet.go is not formatted: run `gofmt -w greet.go` (VS Code's Go extension formats on save)

failures:

test result: FAILED. 2 passed; 2 failed
";

    #[test]
    fn reads_the_go_image_output_like_cargo_test() {
        let report = parse_cargo_test(GO_FAILING, false, 7);
        assert_eq!(report.outcome, CheckOutcome::TestsFailed);
        assert_eq!(report.tests.len(), 4);
        assert_eq!(
            report.tests[0].message.as_deref(),
            Some("todo: write Greet")
        );
        assert_eq!(report.tests[1].name, "TestGreet/blank_name");
        assert!(report.tests[1].passed);

        let broken = parse_cargo_test(
            "error: the code does not compile
./greet.go:7:2: undefined: who
",
            false,
            7,
        );
        assert_eq!(broken.outcome, CheckOutcome::CompileError);
        assert!(broken.details.contains("undefined: who"));
    }

    /// What the dojo-cpp image prints (`runner-images/cpp/report/dojo_report.cpp`).
    const CPP_FAILING: &str = "
running 5 tests
test Greet.UsesTheName ... FAILED
test Buffer.MovedFromIsEmpty ... FAILED
test leak_sanitizer ... ok
test gcc_warnings ... ok
test clang_tidy::greet_cpp ... FAILED

failures:

---- Greet.UsesTheName stdout ----
thread 'Greet.UsesTheName' panicked at Greet.UsesTheName:
todo: write greet

---- Buffer.MovedFromIsEmpty stdout ----
thread 'Buffer.MovedFromIsEmpty' panicked at Buffer.MovedFromIsEmpty:
AddressSanitizer: heap-use-after-free at buffer.cpp:31 in dojo::Buffer::~Buffer()

---- clang_tidy::greet_cpp stdout ----
thread 'clang_tidy::greet_cpp' panicked at clang_tidy::greet_cpp:
greet.cpp:9:10: warning: use nullptr [modernize-use-nullptr]

failures:

test result: FAILED. 2 passed; 3 failed
";

    #[test]
    fn reads_the_cpp_image_output_like_cargo_test() {
        let report = parse_cargo_test(CPP_FAILING, false, 9);
        assert_eq!(report.outcome, CheckOutcome::TestsFailed);
        assert_eq!(report.tests.len(), 5);
        assert_eq!(
            report.tests[0].message.as_deref(),
            Some("todo: write greet")
        );
        assert!(
            report.tests[1]
                .message
                .as_deref()
                .is_some_and(|message| message.starts_with("AddressSanitizer: heap-use-after-free"))
        );
        assert_eq!(report.tests[4].name, "clang_tidy::greet_cpp");

        let broken = parse_cargo_test(
            "error: the code does not compile
greet.cpp:12:5: error: 'who' was not declared in this scope
",
            false,
            9,
        );
        assert_eq!(broken.outcome, CheckOutcome::CompileError);
        assert!(broken.details.contains("'who' was not declared"));
    }

    /// What the dojo-python image prints (`runner-images/python/junit2cargo.py`).
    const PYTHON_FAILING: &str = "
running 3 tests
test test_grading::test_main_returns_zero ... FAILED
test test_visible::test_greets_by_name ... FAILED
test ruff::greet_py ... FAILED

failures:

---- test_grading::test_main_returns_zero stdout ----
thread 'test_grading::test_main_returns_zero' panicked at test_grading::test_main_returns_zero:
todo: write main()

---- test_visible::test_greets_by_name stdout ----
thread 'test_visible::test_greets_by_name' panicked at test_visible::test_greets_by_name:
assert 'Hi Ada' == 'Welcome to the dojo, Ada!'
- Welcome to the dojo, Ada!
+ Hi Ada

---- ruff::greet_py stdout ----
thread 'ruff::greet_py' panicked at ruff::greet_py:
greet.py:8:12: F401 [*] `os` imported but unused

failures:

test result: FAILED. 0 passed; 3 failed
";

    #[test]
    fn reads_the_python_image_output_like_cargo_test() {
        let report = parse_cargo_test(PYTHON_FAILING, false, 7);
        assert_eq!(report.outcome, CheckOutcome::TestsFailed);
        assert_eq!(report.tests.len(), 3);
        assert_eq!(
            report.tests[0].message.as_deref(),
            Some("todo: write main()")
        );
        assert!(
            report.tests[1]
                .message
                .as_deref()
                .unwrap()
                .contains("+ Hi Ada")
        );
        assert_eq!(report.tests[2].name, "ruff::greet_py");

        let syntax = parse_cargo_test(
            "error: the code does not compile
  File \"greet.py\", line 1
    def f(:
          ^
SyntaxError: invalid syntax
",
            false,
            7,
        );
        assert_eq!(syntax.outcome, CheckOutcome::CompileError);
        assert!(syntax.details.contains("SyntaxError: invalid syntax"));
    }

    /// What the dojo-node image prints (`runner-images/node/report.mjs`).
    const NODE_FAILING: &str = "
running 3 tests
test types::grading::rejects_wrong_shape ... FAILED
test visible::adds_two_numbers ... ok
test visible::group::todo_case ... FAILED

failures:

---- types::grading::rejects_wrong_shape stdout ----
thread 'types::grading::rejects_wrong_shape' panicked at types::grading::rejects_wrong_shape:
grading.types.ts:7:25 TS2344: Type 'false' does not satisfy the constraint 'true'.

---- visible::group::todo_case stdout ----
thread 'visible::group::todo_case' panicked at visible::group::todo_case:
todo: write todo()

failures:

test result: FAILED. 1 passed; 2 failed
";

    #[test]
    fn reads_the_node_image_output_like_cargo_test() {
        let report = parse_cargo_test(NODE_FAILING, false, 7);
        assert_eq!(report.outcome, CheckOutcome::TestsFailed);
        assert_eq!(report.tests.len(), 3);
        assert!(
            report.tests[0]
                .message
                .as_deref()
                .unwrap()
                .contains("TS2344")
        );
        assert!(report.tests[1].passed);
        assert_eq!(
            report.tests[2].message.as_deref(),
            Some("todo: write todo()")
        );

        let types = parse_cargo_test(
            "error: the code does not compile (tsc found type errors)
lib.ts:2:3 TS2322: Type 'string' is not assignable to type 'number'.
",
            false,
            7,
        );
        assert_eq!(types.outcome, CheckOutcome::CompileError);
        assert!(types.details.contains("TS2322"));
    }

    #[test]
    fn reads_the_java_image_output_like_cargo_test() {
        let report = parse_cargo_test(JAVA_FAILING, false, 7);
        assert_eq!(report.outcome, CheckOutcome::TestsFailed);
        assert_eq!(report.tests.len(), 2);
        let failed = &report.tests[0];
        assert_eq!(failed.name, "GreeterGradingTest::greets_a_stranger");
        assert_eq!(
            failed.message.as_deref(),
            Some("expected: <Welcome to the dojo, stranger!> but was: <Welcome to the dojo, !>")
        );

        let compile = parse_cargo_test(
            "error: the code does not compile\nsrc/main/java/dojo/Greeter.java:6: error: ';' expected\n",
            false,
            7,
        );
        assert_eq!(compile.outcome, CheckOutcome::CompileError);
        assert!(compile.details.contains("Greeter.java:6"));
    }

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
        let failed = report.tests.iter().find(|test| !test.passed).unwrap();
        assert_eq!(
            failed.message.as_deref(),
            Some(
                "assertion `left == right` failed
  left: \"Hello Ferris\"
 right: \"Welcome to the dojo, Ferris!\""
            )
        );
        assert!(
            report
                .tests
                .iter()
                .filter(|test| test.passed)
                .all(|test| test.message.is_none())
        );
    }

    /// Real `cargo test` output for `#[should_panic]` tests (dojo-rust image, Rust 1.100):
    /// libtest adds ` - should panic` to the name, and a test that did not
    /// panic has a `note:` instead of a `panicked at` line.
    const SHOULD_PANIC: &str = "\
running 6 tests
test does_not_panic - should panic ... FAILED
test ignored_sp ... ignored
test panics_ok - should panic ... ok
test panics_with_msg_ok - should panic ... ok
test panics_wrong_msg - should panic ... FAILED
test plain ... ok

failures:

---- does_not_panic stdout ----
note: test did not panic as expected at tests/visible.rs:6:4
---- panics_wrong_msg stdout ----

thread 'panics_wrong_msg' (30768) panicked at tests/visible.rs:12:25:
boom
note: panic did not contain expected string
      panic message: \"boom\"
 expected substring: \"zzz\"

failures:
    does_not_panic
    panics_wrong_msg

test result: FAILED. 3 passed; 2 failed; 1 ignored; 0 measured; 0 filtered out; finished in 0.00s

error: test failed, to rerun pass `--test visible`
";

    #[test]
    fn should_panic_tests_are_counted_by_their_plain_name() {
        let report = parse_cargo_test(SHOULD_PANIC, false, 5);

        assert_eq!(report.outcome, CheckOutcome::TestsFailed);
        let names: Vec<&str> = report.tests.iter().map(|test| test.name.as_str()).collect();
        assert_eq!(
            names,
            [
                "does_not_panic",
                "panics_ok",
                "panics_with_msg_ok",
                "panics_wrong_msg",
                "plain"
            ]
        );
        assert_eq!(report.summary(), "3/5 tests passed");
        assert_eq!(
            report.tests[0].message.as_deref(),
            Some("test did not panic as expected at tests/visible.rs:6:4")
        );
        assert_eq!(
            report.tests[3].message.as_deref(),
            Some(
                "boom
note: panic did not contain expected string
      panic message: \"boom\"
 expected substring: \"zzz\""
            )
        );
    }

    #[test]
    fn older_should_panic_with_message_suffix_is_also_read() {
        let output = "test a - should panic with message ... ok\ntest b - should panic ... ok\n";
        let report = parse_cargo_test(output, true, 1);
        assert_eq!(report.outcome, CheckOutcome::Passed);
        let names: Vec<&str> = report.tests.iter().map(|test| test.name.as_str()).collect();
        assert_eq!(names, ["a", "b"]);
    }

    #[test]
    fn todo_panics_are_captured_per_test() {
        let output = "test a ... FAILED
test b ... FAILED

failures:

---- a stdout ----

thread 'a' (155) panicked at src/lib.rs:3:5:
not yet implemented: greet
note: run with `RUST_BACKTRACE=1`

---- b stdout ----

thread 'b' (156) panicked at src/lib.rs:3:5:
not yet implemented: greet


failures:
    a
    b
";
        let report = parse_cargo_test(output, false, 1);

        assert!(
            report
                .tests
                .iter()
                .all(|test| test.message.as_deref() == Some("not yet implemented: greet"))
        );
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
