use kata::{ConfigError, parse_config};

#[test]
fn parses_two_keys() {
    let config = parse_config("host = localhost\nport = 8080").unwrap();
    assert_eq!(config["host"], "localhost");
    assert_eq!(config["port"], "8080");
}

#[test]
fn reports_a_line_without_equals() {
    assert_eq!(parse_config("host localhost"), Err(ConfigError::MissingEquals { line: 1 }));
}
