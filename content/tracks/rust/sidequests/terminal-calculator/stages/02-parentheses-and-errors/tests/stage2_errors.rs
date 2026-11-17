//! Stage 2 contract: parentheses, negation, and errors on stderr with exit code 1.

use std::process::Command;

struct Run {
    stdout: String,
    stderr: String,
    code: i32,
}

fn calc(expression: &str) -> Run {
    let output = Command::new(env!("CARGO_BIN_EXE_calc"))
        .arg(expression)
        .output()
        .expect("the calc binary runs");
    Run {
        stdout: String::from_utf8_lossy(&output.stdout)
            .trim_end()
            .to_string(),
        stderr: String::from_utf8_lossy(&output.stderr)
            .trim_end()
            .to_string(),
        code: output.status.code().unwrap_or(-1),
    }
}

fn assert_value(expression: &str, expected: &str) {
    let run = calc(expression);
    assert_eq!(
        (run.stdout.as_str(), run.code),
        (expected, 0),
        "calc {expression:?} should print {expected} (stderr: {})",
        run.stderr
    );
}

fn assert_error(expression: &str, message: &str) {
    let run = calc(expression);
    assert_eq!(run.code, 1, "calc {expression:?} should exit 1");
    assert!(
        run.stdout.is_empty(),
        "calc {expression:?} should print nothing on stdout, printed {:?}",
        run.stdout
    );
    assert!(
        run.stderr.starts_with("error: ") && run.stderr.contains(message),
        "calc {expression:?}: stderr should be `error: ...{message}...`, was {:?}",
        run.stderr
    );
}

#[test]
fn parentheses_group_and_nest() {
    assert_value("(1 + 2) * 3", "9");
    assert_value("2 * (3 + (4 - 1))", "12");
    assert_value("((7))", "7");
}

#[test]
fn a_minus_sign_negates_numbers_and_groups() {
    assert_value("-4 + 10", "6");
    assert_value("2 * -3", "-6");
    assert_value("-(2 + 3)", "-5");
    assert_value("--2", "2");
}

#[test]
fn an_empty_expression_is_an_error() {
    assert_error("", "empty expression");
    assert_error("   ", "empty expression");
}

#[test]
fn division_by_zero_is_an_error_not_infinity() {
    assert_error("1 / 0", "division by zero");
    assert_error("5 / (2 - 2)", "division by zero");
}

#[test]
fn an_unexpected_character_names_it_and_its_position() {
    assert_error("2 $ 3", "unexpected character '$' at position 3");
    assert_error("1+x", "unexpected character 'x' at position 3");
}

#[test]
fn an_unclosed_parenthesis_is_an_error() {
    assert_error("(1 + 2", "expected ')'");
    assert_error("((1)", "expected ')'");
}

#[test]
fn a_missing_operand_is_an_error() {
    assert_error("1 +", "expected a number");
    assert_error("* 2", "expected a number");
}

#[test]
fn two_numbers_in_a_row_are_an_error() {
    assert_error("1 2", "unexpected '2' at position 3");
}

#[test]
fn a_stray_closing_parenthesis_is_an_error() {
    let run = calc("1 + 2)");
    assert_eq!(run.code, 1);
    assert!(run.stderr.starts_with("error: "), "{:?}", run.stderr);
}
