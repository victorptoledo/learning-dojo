//! Stage 1 contract: `calc "<expression>"` prints the value of well-formed arithmetic.

use std::process::Command;

fn calc(expression: &str) -> (String, i32) {
    let output = Command::new(env!("CARGO_BIN_EXE_calc"))
        .arg(expression)
        .output()
        .expect("the calc binary runs");
    (
        String::from_utf8_lossy(&output.stdout)
            .trim_end()
            .to_string(),
        output.status.code().unwrap_or(-1),
    )
}

fn assert_value(expression: &str, expected: &str) {
    let (stdout, code) = calc(expression);
    assert_eq!(
        (stdout.as_str(), code),
        (expected, 0),
        "calc {expression:?} should print {expected} and exit 0"
    );
}

#[test]
fn adds_two_numbers() {
    assert_value("1 + 2", "3");
}

#[test]
fn subtracts_multiplies_and_divides() {
    assert_value("9 - 4", "5");
    assert_value("6 * 7", "42");
    assert_value("10 / 4", "2.5");
}

#[test]
fn multiplication_binds_tighter_than_addition() {
    assert_value("2 + 3 * 4", "14");
    assert_value("2 * 3 + 4", "10");
    assert_value("20 - 6 / 2", "17");
}

#[test]
fn operators_of_the_same_strength_go_left_to_right() {
    assert_value("7 - 2 - 1", "4");
    assert_value("8 / 2 / 2", "2");
    assert_value("2 * 3 / 4", "1.5");
}

#[test]
fn spaces_are_optional() {
    assert_value("2*3+4", "10");
    assert_value("  12   /3 ", "4");
}

#[test]
fn decimals_are_read_and_printed() {
    assert_value("1.5 * 2", "3");
    assert_value("0.1 + 0.2", "0.30000000000000004");
    assert_value("1 / 3", "0.3333333333333333");
}

#[test]
fn a_single_number_is_its_own_value() {
    assert_value("42", "42");
    assert_value("3.25", "3.25");
}

#[test]
fn long_chains_are_evaluated_correctly() {
    assert_value("1 + 2 * 3 - 4 / 2 + 10 * 0.5", "10");
}
