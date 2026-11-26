//! Final exam: Health Probe (reference solution).

use std::error::Error;
use std::fmt;
use std::future::Future;
use std::num::ParseIntError;
use std::pin::Pin;
use std::sync::{Arc, Mutex};
use std::task::{Context, Poll, Wake, Waker};
use std::thread::{self, Thread};
use std::time::{Duration, Instant};

// ---- Part 1: a runtime from the standard library ------------------------------------------

struct ThreadWaker(Thread);

impl Wake for ThreadWaker {
    fn wake(self: Arc<Self>) {
        self.0.unpark();
    }
}

/// Runs a future to completion on the current thread, parking it while the future waits.
pub fn block_on<F: Future>(future: F) -> F::Output {
    let mut future = std::pin::pin!(future);
    let waker = Waker::from(Arc::new(ThreadWaker(thread::current())));
    let mut cx = Context::from_waker(&waker);
    loop {
        match future.as_mut().poll(&mut cx) {
            Poll::Ready(value) => return value,
            Poll::Pending => thread::park(),
        }
    }
}

/// A future that is ready once `duration` has passed, without blocking the thread that polls it.
pub struct Sleep {
    deadline: Instant,
    waker: Option<Arc<Mutex<Option<Waker>>>>,
}

pub fn sleep(duration: Duration) -> Sleep {
    Sleep { deadline: Instant::now() + duration, waker: None }
}

impl Future for Sleep {
    type Output = ();

    fn poll(mut self: Pin<&mut Self>, cx: &mut Context<'_>) -> Poll<()> {
        if Instant::now() >= self.deadline {
            return Poll::Ready(());
        }
        match &self.waker {
            Some(slot) => *slot.lock().unwrap() = Some(cx.waker().clone()),
            None => {
                let slot = Arc::new(Mutex::new(Some(cx.waker().clone())));
                let timer = Arc::clone(&slot);
                let deadline = self.deadline;
                thread::spawn(move || {
                    thread::sleep(deadline.saturating_duration_since(Instant::now()));
                    if let Some(waker) = timer.lock().unwrap().take() {
                        waker.wake();
                    }
                });
                self.waker = Some(slot);
            }
        }
        Poll::Pending
    }
}

/// Polls every future until all are done; the outputs keep the input order.
pub struct JoinAll<F: Future> {
    futures: Vec<Option<Pin<Box<F>>>>,
    outputs: Vec<Option<F::Output>>,
}

pub fn join_all<F: Future>(futures: Vec<F>) -> JoinAll<F> {
    let outputs = futures.iter().map(|_| None).collect();
    JoinAll { futures: futures.into_iter().map(|f| Some(Box::pin(f))).collect(), outputs }
}

impl<F: Future> Unpin for JoinAll<F> {}

impl<F: Future> Future for JoinAll<F> {
    type Output = Vec<F::Output>;

    fn poll(mut self: Pin<&mut Self>, cx: &mut Context<'_>) -> Poll<Self::Output> {
        let this = &mut *self;
        for (slot, output) in this.futures.iter_mut().zip(this.outputs.iter_mut()) {
            if let Some(future) = slot {
                if let Poll::Ready(value) = future.as_mut().poll(cx) {
                    *output = Some(value);
                    *slot = None; // never poll a finished future again
                }
            }
        }
        if this.futures.iter().all(Option::is_none) {
            Poll::Ready(this.outputs.iter_mut().map(|o| o.take().unwrap()).collect())
        } else {
            Poll::Pending
        }
    }
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub struct Elapsed;

impl fmt::Display for Elapsed {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        f.write_str("deadline elapsed")
    }
}

impl Error for Elapsed {}

/// Gives up on `future` after `duration`, dropping it.
pub struct Timeout<F: Future> {
    future: Option<Pin<Box<F>>>,
    timer: Sleep,
}

pub fn timeout<F: Future>(duration: Duration, future: F) -> Timeout<F> {
    Timeout { future: Some(Box::pin(future)), timer: sleep(duration) }
}

impl<F: Future> Unpin for Timeout<F> {}

impl<F: Future> Future for Timeout<F> {
    type Output = Result<F::Output, Elapsed>;

    fn poll(mut self: Pin<&mut Self>, cx: &mut Context<'_>) -> Poll<Self::Output> {
        let this = &mut *self;
        let Some(future) = this.future.as_mut() else { return Poll::Ready(Err(Elapsed)) };
        if let Poll::Ready(value) = future.as_mut().poll(cx) {
            this.future = None;
            return Poll::Ready(Ok(value));
        }
        if Pin::new(&mut this.timer).poll(cx).is_ready() {
            this.future = None; // cancel: drop the work now
            return Poll::Ready(Err(Elapsed));
        }
        Poll::Pending
    }
}

// ---- Part 2: configuration and errors -----------------------------------------------------

#[derive(Debug, Clone, Copy, PartialEq, Eq, PartialOrd, Ord)]
pub enum Level {
    Error,
    Warn,
    Info,
    Debug,
}

impl Level {
    fn name(self) -> &'static str {
        match self {
            Level::Error => "error",
            Level::Warn => "warn",
            Level::Info => "info",
            Level::Debug => "debug",
        }
    }
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct Config {
    pub targets: Vec<String>,
    pub timeout: Duration,
    pub level: Level,
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub enum ConfigError {
    Missing { var: &'static str },
    Invalid { var: &'static str, value: String, reason: String },
    NotANumber { var: &'static str, value: String, source: ParseIntError },
}

impl fmt::Display for ConfigError {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        match self {
            ConfigError::Missing { var } => write!(f, "{var} is not set"),
            ConfigError::Invalid { var, value, reason } => {
                write!(f, "{var}={value} is invalid: {reason}")
            }
            ConfigError::NotANumber { var, value, .. } => write!(f, "{var}={value} is not a number"),
        }
    }
}

impl Error for ConfigError {
    fn source(&self) -> Option<&(dyn Error + 'static)> {
        match self {
            ConfigError::NotANumber { source, .. } => Some(source),
            _ => None,
        }
    }
}

/// Reads the configuration through `lookup` (in production, `|var| std::env::var(var).ok()`).
pub fn load_config(lookup: impl Fn(&str) -> Option<String>) -> Result<Config, ConfigError> {
    const TARGETS: &str = "PROBE_TARGETS";
    const TIMEOUT: &str = "PROBE_TIMEOUT_MS";
    const LOG: &str = "PROBE_LOG";

    let raw = lookup(TARGETS).ok_or(ConfigError::Missing { var: TARGETS })?;
    let mut targets: Vec<String> = Vec::new();
    for name in raw.split(',').map(str::trim).filter(|name| !name.is_empty()) {
        if targets.iter().any(|known| known == name) {
            return Err(ConfigError::Invalid {
                var: TARGETS,
                value: raw.clone(),
                reason: format!("duplicate target {name}"),
            });
        }
        targets.push(name.to_string());
    }
    if targets.is_empty() {
        return Err(ConfigError::Invalid { var: TARGETS, value: raw, reason: "no targets".into() });
    }

    let timeout = match lookup(TIMEOUT) {
        None => Duration::from_millis(500),
        Some(value) => {
            let ms: u64 = value.trim().parse().map_err(|source| ConfigError::NotANumber {
                var: TIMEOUT,
                value: value.clone(),
                source,
            })?;
            if !(1..=60_000).contains(&ms) {
                return Err(ConfigError::Invalid {
                    var: TIMEOUT,
                    value,
                    reason: "must be between 1 and 60000".into(),
                });
            }
            Duration::from_millis(ms)
        }
    };

    let level = match lookup(LOG) {
        None => Level::Info,
        Some(value) => match value.trim().to_ascii_lowercase().as_str() {
            "error" => Level::Error,
            "warn" => Level::Warn,
            "info" => Level::Info,
            "debug" => Level::Debug,
            _ => {
                return Err(ConfigError::Invalid {
                    var: LOG,
                    value,
                    reason: "expected error, warn, info or debug".into(),
                });
            }
        },
    };

    Ok(Config { targets, timeout, level })
}

/// The error and all its causes, joined with ": ".
pub fn error_chain(error: &dyn Error) -> String {
    let mut text = error.to_string();
    let mut cause = error.source();
    while let Some(next) = cause {
        text.push_str(": ");
        text.push_str(&next.to_string());
        cause = next.source();
    }
    text
}

// ---- Part 3: the probe --------------------------------------------------------------------

pub type CheckFuture = Pin<Box<dyn Future<Output = Result<u64, String>> + Send>>;
pub type Check = Arc<dyn Fn(&str) -> CheckFuture + Send + Sync>;

#[derive(Debug, Clone, PartialEq, Eq)]
pub enum Outcome {
    Up { ms: u64 },
    Down { reason: String },
    TimedOut,
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct Probe {
    pub results: Vec<(String, Outcome)>,
    pub logs: Vec<String>,
}

fn escape(message: &str) -> String {
    let mut out = String::with_capacity(message.len());
    for c in message.chars() {
        match c {
            '\\' => out.push_str("\\\\"),
            '"' => out.push_str("\\\""),
            '\n' => out.push_str("\\n"),
            _ => out.push(c),
        }
    }
    out
}

fn log_line(level: Level, target: Option<&str>, message: &str) -> String {
    match target {
        Some(target) => format!("level={} target={target} msg=\"{}\"", level.name(), escape(message)),
        None => format!("level={} msg=\"{}\"", level.name(), escape(message)),
    }
}

/// Checks every target at the same time, each under the configured timeout.
pub fn probe(config: &Config, check: Check) -> Probe {
    let checks: Vec<_> =
        config.targets.iter().map(|target| timeout(config.timeout, check(target))).collect();
    let outcomes = block_on(join_all(checks));

    let mut logs = Vec::new();
    let mut emit = |level: Level, target: Option<&str>, message: &str| {
        if level <= config.level {
            logs.push(log_line(level, target, message));
        }
    };
    let mut results = Vec::new();
    let mut up = 0;
    for (target, outcome) in config.targets.iter().zip(outcomes) {
        emit(Level::Debug, Some(target), "checking");
        let outcome = match outcome {
            Ok(Ok(ms)) => {
                up += 1;
                emit(Level::Info, Some(target), &format!("up in {ms} ms"));
                Outcome::Up { ms }
            }
            Ok(Err(reason)) => {
                emit(Level::Error, Some(target), &format!("down: {reason}"));
                Outcome::Down { reason }
            }
            Err(Elapsed) => {
                let ms = config.timeout.as_millis();
                emit(Level::Warn, Some(target), &format!("timed out after {ms} ms"));
                Outcome::TimedOut
            }
        };
        results.push((target.clone(), outcome));
    }
    let total = results.len();
    let summary = format!("{up} of {total} targets up");
    emit(if up == total { Level::Info } else { Level::Warn }, None, &summary);
    Probe { results, logs }
}

/// What `main` does: 0 when every target is up, 1 when some are not, 2 when the configuration
/// is wrong. Returns the exit code and the log lines to print.
pub fn run(lookup: impl Fn(&str) -> Option<String>, check: Check) -> (i32, Vec<String>) {
    match load_config(lookup) {
        Err(error) => (2, vec![log_line(Level::Error, None, &error_chain(&error))]),
        Ok(config) => {
            let probe = probe(&config, check);
            let all_up = probe.results.iter().all(|(_, o)| matches!(o, Outcome::Up { .. }));
            (if all_up { 0 } else { 1 }, probe.logs)
        }
    }
}
