use kata::{DurationError, parse_duration};

#[test]
fn every_unit_is_converted() {
    assert_eq!(parse_duration("45s"), Ok(45));
    assert_eq!(parse_duration("5m"), Ok(300));
    assert_eq!(parse_duration("2h"), Ok(7200));
    assert_eq!(parse_duration("0m"), Ok(0));
}

#[test]
fn surrounding_spaces_are_ignored() {
    assert_eq!(parse_duration("  5m \n"), Ok(300));
}

#[test]
fn empty_and_blank_are_empty() {
    assert_eq!(parse_duration(""), Err(DurationError::Empty));
    assert_eq!(parse_duration("   "), Err(DurationError::Empty));
}

#[test]
fn a_bare_number_has_no_unit() {
    assert_eq!(parse_duration("42"), Err(DurationError::MissingUnit));
}

#[test]
fn unknown_units_are_named() {
    assert_eq!(parse_duration("3d"), Err(DurationError::UnknownUnit('d')));
    assert_eq!(parse_duration("3µ"), Err(DurationError::UnknownUnit('µ')));
}

#[test]
fn bad_numbers_are_reported_as_written() {
    assert_eq!(parse_duration("xs"), Err(DurationError::BadNumber("x".into())));
    assert_eq!(parse_duration("h"), Err(DurationError::BadNumber(String::new())));
    assert_eq!(parse_duration("-5m"), Err(DurationError::BadNumber("-5".into())));
    assert_eq!(parse_duration("1.5h"), Err(DurationError::BadNumber("1.5".into())));
}

#[test]
fn an_overflow_is_a_bad_number() {
    let huge = format!("{}h", u64::MAX);
    assert_eq!(
        parse_duration(&huge),
        Err(DurationError::BadNumber(u64::MAX.to_string()))
    );
}

#[test]
fn errors_explain_themselves() {
    assert_eq!(DurationError::Empty.to_string(), "empty duration");
    assert_eq!(
        DurationError::MissingUnit.to_string(),
        "missing unit (use s, m or h)"
    );
    assert_eq!(DurationError::UnknownUnit('d').to_string(), "unknown unit `d`");
    assert_eq!(
        DurationError::BadNumber("1.5".into()).to_string(),
        "`1.5` is not a whole number"
    );
}

#[test]
fn the_error_works_behind_a_box() {
    fn total(parts: &[&str]) -> Result<u64, Box<dyn std::error::Error>> {
        let mut sum = 0;
        for part in parts {
            sum += parse_duration(part)?;
        }
        Ok(sum)
    }
    assert_eq!(total(&["1m", "30s"]).unwrap(), 90);
    assert_eq!(total(&["1m", "45"]).unwrap_err().to_string(), "missing unit (use s, m or h)");
}
