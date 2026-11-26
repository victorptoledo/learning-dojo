use kata::{block_on, countdown, parse_port};

#[test]
fn counts_down() {
    assert_eq!(block_on(countdown(2)), 3);
}

#[test]
fn parses_a_port() {
    assert_eq!(parse_port("8080"), Ok(8080));
}
