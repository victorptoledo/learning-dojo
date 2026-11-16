use kata::{Summary, parse_readings};

#[test]
fn two_readings() {
    let readings = parse_readings("1\n2").unwrap();
    assert_eq!(readings.values, [1.0, 2.0]);
    assert_eq!(readings.summary(), "2 readings, max 2.0, mean 1.5");
}
