use kata::{
    Check, CheckFuture, Config, ConfigError, Elapsed, Level, Outcome, Sleep, block_on, error_chain,
    join_all, load_config, probe, run, sleep, timeout,
};
use std::collections::HashMap;
use std::error::Error;
use std::fmt;
use std::future::Future;
use std::pin::Pin;
use std::sync::atomic::{AtomicBool, AtomicUsize, Ordering};
use std::sync::{Arc, mpsc};
use std::task::{Context, Poll, Wake, Waker};
use std::thread;
use std::time::{Duration, Instant};

fn ms(n: u64) -> Duration {
    Duration::from_millis(n)
}

/// Runs `f` on another thread so a hang fails the test instead of the whole run.
fn within<T: Send + 'static>(seconds: u64, f: impl FnOnce() -> T + Send + 'static) -> T {
    let (sender, receiver) = mpsc::channel();
    thread::spawn(move || {
        let _ = sender.send(f());
    });
    receiver.recv_timeout(Duration::from_secs(seconds)).expect("did not finish in time")
}

fn env(pairs: &[(&str, &str)]) -> impl Fn(&str) -> Option<String> + use<> {
    let map: HashMap<String, String> =
        pairs.iter().map(|(k, v)| (k.to_string(), v.to_string())).collect();
    move |var| map.get(var).cloned()
}

fn config(targets: &[&str], timeout_ms: u64, level: Level) -> Config {
    Config {
        targets: targets.iter().map(|t| t.to_string()).collect(),
        timeout: ms(timeout_ms),
        level,
    }
}

/// A check built from a table: target -> (delay in ms, result).
fn scripted(table: &[(&str, u64, Result<u64, &str>)]) -> Check {
    let table: HashMap<String, (u64, Result<u64, String>)> = table
        .iter()
        .map(|(name, delay, result)| (name.to_string(), (*delay, result.map_err(str::to_string))))
        .collect();
    Arc::new(move |target: &str| -> CheckFuture {
        let (delay, result) = table[target].clone();
        Box::pin(async move {
            sleep(ms(delay)).await;
            result
        })
    })
}

/// Pending until another thread wakes it after `delay`; counts its polls.
struct WokenLater {
    polls: Arc<AtomicUsize>,
    ready: Arc<AtomicBool>,
    started: bool,
    delay: Duration,
}

impl Future for WokenLater {
    type Output = usize;
    fn poll(mut self: Pin<&mut Self>, cx: &mut Context<'_>) -> Poll<usize> {
        let polls = self.polls.fetch_add(1, Ordering::SeqCst) + 1;
        if self.ready.load(Ordering::SeqCst) {
            return Poll::Ready(polls);
        }
        if !self.started {
            self.started = true;
            let (ready, waker, delay) = (Arc::clone(&self.ready), cx.waker().clone(), self.delay);
            thread::spawn(move || {
                thread::sleep(delay);
                ready.store(true, Ordering::SeqCst);
                waker.wake();
            });
        }
        Poll::Pending
    }
}

/// Ready on the first poll; panics if polled again.
struct Once(bool);

impl Future for Once {
    type Output = &'static str;
    fn poll(mut self: Pin<&mut Self>, _cx: &mut Context<'_>) -> Poll<&'static str> {
        assert!(!self.0, "polled again after it returned Ready");
        self.0 = true;
        Poll::Ready("once")
    }
}

struct CountingWaker(AtomicUsize);

impl Wake for CountingWaker {
    fn wake(self: Arc<Self>) {
        self.0.fetch_add(1, Ordering::SeqCst);
    }
}

// ---- Part 1: the runtime ------------------------------------------------------------------

#[test]
fn block_on_returns_the_value_of_a_ready_future() {
    assert_eq!(block_on(async { 6 * 7 }), 42);
}

#[test]
fn block_on_parks_instead_of_spinning() {
    let polls = within(10, || {
        block_on(WokenLater {
            polls: Arc::new(AtomicUsize::new(0)),
            ready: Arc::new(AtomicBool::new(false)),
            started: false,
            delay: ms(150),
        })
    });
    assert!(polls <= 5, "the future was polled {polls} times while it waited: the executor spins");
}

#[test]
fn sleep_waits_at_least_its_duration() {
    let begin = Instant::now();
    within(10, || block_on(sleep(ms(150))));
    assert!(begin.elapsed() >= ms(150));
}

#[test]
fn sleeps_run_concurrently_on_one_thread() {
    let begin = Instant::now();
    within(10, || block_on(join_all(vec![sleep(ms(300)), sleep(ms(300)), sleep(ms(300))])));
    let took = begin.elapsed();
    assert!(took < ms(700), "three 300 ms sleeps took {took:?}: sleep blocks the thread");
}

#[test]
fn sleep_wakes_the_waker_from_the_latest_poll() {
    let first = Arc::new(CountingWaker(AtomicUsize::new(0)));
    let second = Arc::new(CountingWaker(AtomicUsize::new(0)));
    let mut timer = Box::pin(sleep(ms(100)));
    let waker = Waker::from(Arc::clone(&first));
    assert!(timer.as_mut().poll(&mut Context::from_waker(&waker)).is_pending());
    let waker = Waker::from(Arc::clone(&second));
    assert!(timer.as_mut().poll(&mut Context::from_waker(&waker)).is_pending());
    thread::sleep(ms(400));
    assert_eq!(second.0.load(Ordering::SeqCst), 1, "the newest waker was not woken");
    assert_eq!(first.0.load(Ordering::SeqCst), 0, "an old waker was woken");
}

#[test]
fn join_all_keeps_the_input_order() {
    let slow_first = vec![
        Box::pin(async {
            sleep(ms(200)).await;
            "slow"
        }) as Pin<Box<dyn Future<Output = &str> + Send>>,
        Box::pin(async { "fast" }),
        Box::pin(async {
            sleep(ms(50)).await;
            "middle"
        }),
    ];
    assert_eq!(within(10, || block_on(join_all(slow_first))), ["slow", "fast", "middle"]);
}

#[test]
fn join_all_never_polls_a_finished_future_again() {
    let futures = vec![
        Box::pin(Once(false)) as Pin<Box<dyn Future<Output = &str> + Send>>,
        Box::pin(async {
            sleep(ms(100)).await;
            "later"
        }),
    ];
    assert_eq!(within(10, || block_on(join_all(futures))), ["once", "later"]);
}

#[test]
fn join_all_of_nothing_is_ready_at_once() {
    let none: Vec<Sleep> = Vec::new();
    assert!(block_on(join_all(none)).is_empty());
}

#[test]
fn timeout_returns_the_value_when_the_work_is_fast_enough() {
    let result = within(10, || {
        block_on(timeout(ms(300), async {
            sleep(ms(50)).await;
            5
        }))
    });
    assert_eq!(result, Ok(5));
}

#[test]
fn a_ready_future_wins_even_a_zero_timeout() {
    assert_eq!(block_on(timeout(Duration::ZERO, async { 9 })), Ok(9));
}

#[test]
fn timeout_gives_up_on_time_and_drops_the_work() {
    struct Flag(Arc<AtomicBool>);
    impl Drop for Flag {
        fn drop(&mut self) {
            self.0.store(true, Ordering::SeqCst);
        }
    }
    let dropped = Arc::new(AtomicBool::new(false));
    let flag = Flag(Arc::clone(&dropped));
    let seen = Arc::clone(&dropped);
    let begin = Instant::now();
    let (result, dropped_on_return) = within(10, move || {
        let result = block_on(async move {
            let outcome = timeout(ms(100), async move {
                let _flag = flag;
                sleep(ms(5_000)).await;
            })
            .await;
            outcome
        });
        (result, seen.load(Ordering::SeqCst))
    });
    assert_eq!(result, Err(Elapsed));
    assert!(begin.elapsed() < ms(2_000), "the timeout fired after {:?}", begin.elapsed());
    assert!(dropped_on_return, "the cancelled work was not dropped");
}

// ---- Part 2: configuration ----------------------------------------------------------------

#[test]
fn loads_targets_and_applies_the_defaults() {
    let config = load_config(env(&[("PROBE_TARGETS", " api, db ,,cache ")])).unwrap();
    assert_eq!(config, Config {
        targets: vec!["api".into(), "db".into(), "cache".into()],
        timeout: ms(500),
        level: Level::Info,
    });
}

#[test]
fn reads_the_timeout_and_the_level_in_any_case() {
    let config = load_config(env(&[
        ("PROBE_TARGETS", "api"),
        ("PROBE_TIMEOUT_MS", " 1500 "),
        ("PROBE_LOG", "DeBuG"),
    ]))
    .unwrap();
    assert_eq!(config.timeout, ms(1500));
    assert_eq!(config.level, Level::Debug);
}

#[test]
fn targets_are_required() {
    assert_eq!(load_config(env(&[])), Err(ConfigError::Missing { var: "PROBE_TARGETS" }));
    assert!(matches!(
        load_config(env(&[("PROBE_TARGETS", " , ")])),
        Err(ConfigError::Invalid { var: "PROBE_TARGETS", ref reason, .. }) if reason == "no targets"
    ));
}

#[test]
fn a_duplicate_target_is_invalid() {
    let error = load_config(env(&[("PROBE_TARGETS", "api,db,api")])).unwrap_err();
    assert_eq!(error.to_string(), "PROBE_TARGETS=api,db,api is invalid: duplicate target api");
}

#[test]
fn the_timeout_must_be_a_number_in_range() {
    for bad in ["0", "60001"] {
        let error =
            load_config(env(&[("PROBE_TARGETS", "api"), ("PROBE_TIMEOUT_MS", bad)])).unwrap_err();
        assert_eq!(error.to_string(), format!("PROBE_TIMEOUT_MS={bad} is invalid: must be between 1 and 60000"));
    }
    let edge = load_config(env(&[("PROBE_TARGETS", "api"), ("PROBE_TIMEOUT_MS", "60000")]));
    assert_eq!(edge.unwrap().timeout, ms(60_000));
}

#[test]
fn a_timeout_that_is_not_a_number_keeps_the_parse_error_as_its_source() {
    let error =
        load_config(env(&[("PROBE_TARGETS", "api"), ("PROBE_TIMEOUT_MS", "fast")])).unwrap_err();
    assert_eq!(error.to_string(), "PROBE_TIMEOUT_MS=fast is not a number");
    let source = error.source().expect("the parse error is the source");
    assert!(source.downcast_ref::<std::num::ParseIntError>().is_some());
    assert_eq!(error_chain(&error), "PROBE_TIMEOUT_MS=fast is not a number: invalid digit found in string");
}

#[test]
fn an_unknown_level_is_invalid_and_has_no_source() {
    let error = load_config(env(&[("PROBE_TARGETS", "api"), ("PROBE_LOG", "loud")])).unwrap_err();
    assert_eq!(error.to_string(), "PROBE_LOG=loud is invalid: expected error, warn, info or debug");
    assert!(error.source().is_none());
}

#[test]
fn error_chain_walks_every_cause() {
    #[derive(Debug)]
    struct Layer(&'static str, Option<Box<dyn Error + 'static>>);
    impl fmt::Display for Layer {
        fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
            f.write_str(self.0)
        }
    }
    impl Error for Layer {
        fn source(&self) -> Option<&(dyn Error + 'static)> {
            self.1.as_deref()
        }
    }
    let error = Layer("probe failed", Some(Box::new(Layer("bad config", Some(Box::new(Layer("io", None)))))));
    assert_eq!(error_chain(&error), "probe failed: bad config: io");
    assert_eq!(error_chain(&Layer("alone", None)), "alone");
}

// ---- Part 3: the probe --------------------------------------------------------------------

#[test]
fn reports_each_target_in_config_order() {
    let check = scripted(&[("api", 10, Ok(12)), ("db", 10, Err("connection refused")), ("cache", 900, Ok(1))]);
    let result = within(10, move || probe(&config(&["api", "db", "cache"], 200, Level::Info), check));
    assert_eq!(result.results, [
        ("api".to_string(), Outcome::Up { ms: 12 }),
        ("db".to_string(), Outcome::Down { reason: "connection refused".into() }),
        ("cache".to_string(), Outcome::TimedOut),
    ]);
    assert_eq!(result.logs, [
        r#"level=info target=api msg="up in 12 ms""#,
        r#"level=error target=db msg="down: connection refused""#,
        r#"level=warn target=cache msg="timed out after 200 ms""#,
        r#"level=warn msg="1 of 3 targets up""#,
    ]);
}

#[test]
fn checks_run_at_the_same_time() {
    let check = scripted(&[("a", 300, Ok(1)), ("b", 300, Ok(2)), ("c", 300, Ok(3))]);
    let begin = Instant::now();
    let result = within(10, move || probe(&config(&["a", "b", "c"], 1_000, Level::Info), check));
    assert_eq!(result.results.len(), 3);
    let took = begin.elapsed();
    assert!(took < ms(800), "three 300 ms checks took {took:?}: they ran one after another");
}

#[test]
fn a_hanging_check_costs_only_the_timeout() {
    let check = scripted(&[("stuck", 10_000, Ok(1)), ("ok", 20, Ok(4))]);
    let begin = Instant::now();
    let result = within(10, move || probe(&config(&["stuck", "ok"], 150, Level::Info), check));
    assert!(begin.elapsed() < ms(2_000));
    assert_eq!(result.results[0].1, Outcome::TimedOut);
    assert_eq!(result.results[1].1, Outcome::Up { ms: 4 });
}

#[test]
fn the_level_filters_the_logs() {
    let table = [("api", 5, Ok(3)), ("db", 5, Err("refused"))];
    let errors_only = probe(&config(&["api", "db"], 500, Level::Error), scripted(&table));
    assert_eq!(errors_only.logs, [r#"level=error target=db msg="down: refused""#]);
    let warnings = probe(&config(&["api", "db"], 500, Level::Warn), scripted(&table));
    assert_eq!(warnings.logs, [
        r#"level=error target=db msg="down: refused""#,
        r#"level=warn msg="1 of 2 targets up""#,
    ]);
    let debug = probe(&config(&["api", "db"], 500, Level::Debug), scripted(&table));
    assert_eq!(debug.logs, [
        r#"level=debug target=api msg="checking""#,
        r#"level=info target=api msg="up in 3 ms""#,
        r#"level=debug target=db msg="checking""#,
        r#"level=error target=db msg="down: refused""#,
        r#"level=warn msg="1 of 2 targets up""#,
    ]);
}

#[test]
fn the_summary_is_info_when_everything_is_up() {
    let result = probe(&config(&["api"], 500, Level::Info), scripted(&[("api", 1, Ok(2))]));
    assert_eq!(result.logs.last().unwrap(), r#"level=info msg="1 of 1 targets up""#);
}

#[test]
fn messages_are_escaped_so_each_log_stays_one_line() {
    let check = scripted(&[("db", 1, Err("said \"no\"\nthen C:\\fail"))]);
    let result = probe(&config(&["db"], 500, Level::Error), check);
    assert_eq!(result.logs, [r#"level=error target=db msg="down: said \"no\"\nthen C:\\fail""#]);
}

#[test]
fn run_exits_0_1_or_2() {
    let up = scripted(&[("api", 1, Ok(1)), ("db", 1, Ok(1))]);
    let (code, _) = run(env(&[("PROBE_TARGETS", "api,db")]), up);
    assert_eq!(code, 0);

    let down = scripted(&[("api", 1, Ok(1)), ("db", 1, Err("refused"))]);
    let (code, logs) = run(env(&[("PROBE_TARGETS", "api,db"), ("PROBE_LOG", "error")]), down);
    assert_eq!(code, 1);
    assert_eq!(logs, [r#"level=error target=db msg="down: refused""#]);

    let (code, logs) = run(env(&[("PROBE_TARGETS", "api"), ("PROBE_TIMEOUT_MS", "x")]), scripted(&[]));
    assert_eq!(code, 2);
    assert_eq!(logs, [r#"level=error msg="PROBE_TIMEOUT_MS=x is not a number: invalid digit found in string""#]);
}
