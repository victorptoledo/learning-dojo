use kata::{Check, CheckFuture, Config, Level, Outcome, block_on, join_all, load_config, probe, sleep};
use std::sync::Arc;
use std::time::Duration;

#[test]
fn block_on_runs_an_async_block() {
    assert_eq!(block_on(async { 1 + 1 }), 2);
}

#[test]
fn two_sleeps_join() {
    let done = block_on(join_all(vec![sleep(Duration::from_millis(20)), sleep(Duration::from_millis(10))]));
    assert_eq!(done.len(), 2);
}

#[test]
fn loads_a_minimal_config() {
    let config = load_config(|var| (var == "PROBE_TARGETS").then(|| "api".to_string())).unwrap();
    assert_eq!(config.targets, ["api"]);
    assert_eq!(config.timeout, Duration::from_millis(500));
    assert_eq!(config.level, Level::Info);
}

#[test]
fn probes_one_target() {
    let check: Check = Arc::new(|_: &str| -> CheckFuture { Box::pin(async { Ok(7) }) });
    let config = Config { targets: vec!["api".into()], timeout: Duration::from_millis(200), level: Level::Info };
    let result = probe(&config, check);
    assert_eq!(result.results, [("api".to_string(), Outcome::Up { ms: 7 })]);
    assert_eq!(result.logs[0], r#"level=info target=api msg="up in 7 ms""#);
}
