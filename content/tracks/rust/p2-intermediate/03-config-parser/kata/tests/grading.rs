use kata::{ConfigError, parse_config};

#[test]
fn ignores_comments_and_blank_lines() {
    let config = parse_config("# server\n\n   # indented comment\nname = dojo\n").unwrap();
    assert_eq!(config.len(), 1);
    assert_eq!(config["name"], "dojo");
}

#[test]
fn trims_keys_and_values_and_allows_empty_values() {
    let config = parse_config("  path   =  /tmp  \nempty =").unwrap();
    assert_eq!(config["path"], "/tmp");
    assert_eq!(config["empty"], "");
}

#[test]
fn only_the_first_equals_separates() {
    let config = parse_config("query = a=b=c").unwrap();
    assert_eq!(config["query"], "a=b=c");
}

#[test]
fn empty_input_is_an_empty_map() {
    assert!(parse_config("").unwrap().is_empty());
}

#[test]
fn line_numbers_count_ignored_lines() {
    assert_eq!(parse_config("# c\n\nbroken"), Err(ConfigError::MissingEquals { line: 3 }));
}

#[test]
fn reports_an_empty_key() {
    assert_eq!(parse_config("a = 1\n = 2"), Err(ConfigError::EmptyKey { line: 2 }));
}

#[test]
fn reports_the_second_occurrence_of_a_duplicate_key() {
    assert_eq!(
        parse_config("port = 1\nhost = x\n\nport = 2"),
        Err(ConfigError::DuplicateKey { key: "port".to_string(), line: 4 })
    );
}

#[test]
fn errors_have_readable_messages() {
    assert_eq!(ConfigError::MissingEquals { line: 3 }.to_string(), "line 3: expected key = value");
    assert_eq!(ConfigError::EmptyKey { line: 2 }.to_string(), "line 2: empty key");
    assert_eq!(
        ConfigError::DuplicateKey { key: "port".to_string(), line: 4 }.to_string(),
        "line 4: duplicate key \"port\""
    );
}

#[test]
fn config_error_is_a_std_error() {
    let error: Box<dyn std::error::Error> = Box::new(ConfigError::EmptyKey { line: 1 });
    assert_eq!(error.to_string(), "line 1: empty key");
}
