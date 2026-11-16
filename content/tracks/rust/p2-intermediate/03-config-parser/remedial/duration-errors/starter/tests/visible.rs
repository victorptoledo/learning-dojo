use kata::{DurationError, parse_duration};

#[test]
fn ninety_seconds() {
    assert_eq!(parse_duration("90s"), Ok(90));
}

#[test]
fn days_are_not_a_unit() {
    assert_eq!(parse_duration("3d"), Err(DurationError::UnknownUnit('d')));
}
