use kata::{Job, ParseError, Target, parse, plan, start};
use std::sync::Arc;

#[test]
fn parses_a_target_and_its_dependencies() {
    assert_eq!(
        parse("app: core utils\n").unwrap(),
        [Target { name: "app", deps: vec!["core", "utils"] }]
    );
}

#[test]
fn reports_a_missing_colon() {
    assert_eq!(parse("app core"), Err(ParseError::MissingColon { line: 1 }));
}

#[test]
fn a_diamond_has_three_waves() {
    let targets = parse("app: left right\nleft: base\nright: base\nbase:\n").unwrap();
    assert_eq!(plan(&targets).unwrap(), [vec!["base"], vec!["left", "right"], vec!["app"]]);
}

#[test]
fn builds_a_single_target() {
    let job: Job = Arc::new(|name: &str| Ok(name.len() as u64));
    let report = start(vec![vec!["core".to_string()]], 2, job).wait();
    assert_eq!(report.built, [("core".to_string(), 4)]);
    assert!(report.failed.is_empty() && report.skipped.is_empty());
}
