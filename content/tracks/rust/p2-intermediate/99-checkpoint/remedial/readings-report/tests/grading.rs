use kata::{ParseError, Readings, Summary, parse_readings};

#[test]
fn blank_lines_and_spaces_are_skipped() {
    let readings = parse_readings("20\n\n  21.5 \n18.5\n").unwrap();
    assert_eq!(readings.values, [20.0, 21.5, 18.5]);
}

#[test]
fn max_and_mean() {
    let readings = parse_readings("20\n21.5\n18.5").unwrap();
    assert_eq!(readings.max(), Some(21.5));
    assert_eq!(readings.mean(), Some(20.0));
    assert_eq!(readings.summary(), "3 readings, max 21.5, mean 20.0");
}

#[test]
fn negative_readings_work() {
    let readings = parse_readings("-3\n-1").unwrap();
    assert_eq!(readings.max(), Some(-1.0));
    assert_eq!(readings.summary(), "2 readings, max -1.0, mean -2.0");
}

#[test]
fn the_first_bad_line_is_reported() {
    assert_eq!(
        parse_readings("1\nabc\n2\nxyz"),
        Err(ParseError {
            line: 2,
            text: "abc".into(),
        })
    );
}

#[test]
fn line_numbers_count_blank_lines() {
    assert_eq!(parse_readings("1\n\n oops ").unwrap_err().line, 3);
}

#[test]
fn the_error_explains_itself() {
    let error = parse_readings("1\nabc").unwrap_err();
    assert_eq!(error.to_string(), "line 2: `abc` is not a number");
}

#[test]
fn no_readings() {
    let readings = parse_readings("").unwrap();
    assert!(readings.values.is_empty());
    assert_eq!(readings.max(), None);
    assert_eq!(readings.mean(), None);
    assert_eq!(readings.summary(), "no readings");
}

#[test]
fn a_single_reading() {
    let readings = Readings { values: vec![4.0] };
    assert_eq!(readings.summary(), "1 reading, max 4.0, mean 4.0");
}

#[test]
fn summary_works_through_the_trait() {
    fn describe(item: &dyn Summary) -> String {
        item.summary()
    }
    fn describe_all<T: Summary>(items: &[T]) -> Vec<String> {
        items.iter().map(Summary::summary).collect()
    }
    let readings = parse_readings("10\n30").unwrap();
    assert_eq!(describe(&readings), "2 readings, max 30.0, mean 20.0");
    assert_eq!(describe_all(&[readings]), ["2 readings, max 30.0, mean 20.0"]);
}
