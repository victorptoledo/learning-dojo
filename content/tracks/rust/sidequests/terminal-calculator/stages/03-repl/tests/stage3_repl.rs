//! Stage 3 contract: with no argument, calc reads expressions from stdin.

use std::io::Write;
use std::process::{Command, Stdio};

struct Run {
    stdout: Vec<String>,
    stderr: Vec<String>,
    code: i32,
}

fn repl(input: &str) -> Run {
    let mut child = Command::new(env!("CARGO_BIN_EXE_calc"))
        .stdin(Stdio::piped())
        .stdout(Stdio::piped())
        .stderr(Stdio::piped())
        .spawn()
        .expect("the calc binary runs");
    child
        .stdin
        .take()
        .unwrap()
        .write_all(input.as_bytes())
        .unwrap();
    let output = child.wait_with_output().unwrap();
    let lines = |bytes: &[u8]| -> Vec<String> {
        String::from_utf8_lossy(bytes)
            .lines()
            .map(|line| line.trim_end().to_string())
            .filter(|line| !line.is_empty())
            .collect()
    };
    Run {
        stdout: lines(&output.stdout),
        stderr: lines(&output.stderr),
        code: output.status.code().unwrap_or(-1),
    }
}

#[test]
fn each_line_prints_its_result() {
    let run = repl("1 + 2\n2 * (3 + 4)\n");
    assert_eq!(run.stdout, ["3", "14"]);
    assert_eq!(run.code, 0);
}

#[test]
fn blank_lines_are_skipped() {
    let run = repl("\n4 / 2\n\n   \n1\n");
    assert_eq!(run.stdout, ["2", "1"]);
    assert_eq!(run.code, 0);
}

#[test]
fn let_stores_a_variable_for_later_lines() {
    let run = repl("let rate = 2\nrate * 21\n");
    assert_eq!(run.stdout, ["rate = 2", "42"]);
}

#[test]
fn let_again_replaces_the_value() {
    let run = repl("let x = 1\nlet x = x + 10\nx\n");
    assert_eq!(run.stdout, ["x = 1", "x = 11", "11"]);
}

#[test]
fn names_may_hold_digits_and_underscores() {
    let run = repl("let total_2 = 5\ntotal_2 * total_2\n");
    assert_eq!(run.stdout, ["total_2 = 5", "25"]);
}

#[test]
fn ans_is_the_last_result() {
    let run = repl("1 + 2\nans * 10\nlet y = 4\nans + 1\n");
    assert_eq!(run.stdout, ["3", "30", "y = 4", "5"]);
}

#[test]
fn a_bad_line_goes_to_stderr_and_the_repl_goes_on() {
    let run = repl("1 / 0\n2 + 2\n");
    assert_eq!(run.stdout, ["4"]);
    assert_eq!(run.stderr.len(), 1, "{:?}", run.stderr);
    assert!(run.stderr[0].starts_with("error: "));
    assert!(run.stderr[0].contains("division by zero"));
    assert_eq!(run.code, 1, "a failed line makes the exit code 1");
}

#[test]
fn an_unknown_variable_is_an_error() {
    let run = repl("y + 1\n");
    assert!(run.stdout.is_empty(), "{:?}", run.stdout);
    assert!(
        run.stderr
            .first()
            .is_some_and(|line| line.contains("unknown variable 'y'")),
        "{:?}",
        run.stderr
    );
    assert_eq!(run.code, 1);
}

#[test]
fn ans_before_any_result_is_unknown() {
    let run = repl("ans\n");
    assert!(
        run.stderr
            .first()
            .is_some_and(|line| line.contains("unknown variable 'ans'")),
        "{:?}",
        run.stderr
    );
}

#[test]
fn a_failed_line_keeps_the_previous_ans() {
    let run = repl("5\n1 / 0\nans\n");
    assert_eq!(run.stdout, ["5", "5"]);
}

#[test]
fn an_argument_still_works() {
    let output = Command::new(env!("CARGO_BIN_EXE_calc"))
        .arg("(2 + 3) * -2")
        .output()
        .unwrap();
    assert_eq!(String::from_utf8_lossy(&output.stdout).trim(), "-10");
    assert_eq!(output.status.code(), Some(0));
}
