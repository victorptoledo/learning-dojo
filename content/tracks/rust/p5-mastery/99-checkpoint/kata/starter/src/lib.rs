//! Final exam: Health Probe. See README.md for the rules.
#![allow(unused)]

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

/// Runs a future to completion on the current thread, parking it while the future waits.
pub fn block_on<F: Future>(future: F) -> F::Output {
    todo!("pin the future, build a waker that unparks this thread, poll and park")
}

/// A future that is ready once `duration` has passed, without blocking the thread that polls it.
/// Add the fields you need.
pub struct Sleep {
    deadline: Instant,
}

pub fn sleep(duration: Duration) -> Sleep {
    Sleep { deadline: Instant::now() + duration }
}

impl Future for Sleep {
    type Output = ();

    fn poll(self: Pin<&mut Self>, cx: &mut Context<'_>) -> Poll<()> {
        todo!("ready after the deadline; otherwise arrange a wake-up of the newest waker")
    }
}

/// Polls every future until all are done; the outputs keep the input order.
/// Add the fields you need.
pub struct JoinAll<F: Future> {
    futures: Vec<F>,
}

pub fn join_all<F: Future>(futures: Vec<F>) -> JoinAll<F> {
    JoinAll { futures }
}

impl<F: Future> Future for JoinAll<F> {
    type Output = Vec<F::Output>;

    fn poll(self: Pin<&mut Self>, cx: &mut Context<'_>) -> Poll<Self::Output> {
        todo!()
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

/// Gives up on `future` after `duration`, dropping it. Add the fields you need.
pub struct Timeout<F: Future> {
    future: F,
    duration: Duration,
}

pub fn timeout<F: Future>(duration: Duration, future: F) -> Timeout<F> {
    Timeout { future, duration }
}

impl<F: Future> Future for Timeout<F> {
    type Output = Result<F::Output, Elapsed>;

    fn poll(self: Pin<&mut Self>, cx: &mut Context<'_>) -> Poll<Self::Output> {
        todo!()
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
        todo!("the three messages in the README")
    }
}

impl Error for ConfigError {
    // TODO: source() for NotANumber
}

/// Reads the configuration through `lookup` (in production, `|var| std::env::var(var).ok()`).
pub fn load_config(lookup: impl Fn(&str) -> Option<String>) -> Result<Config, ConfigError> {
    todo!()
}

/// The error and all its causes, joined with ": ".
pub fn error_chain(error: &dyn Error) -> String {
    todo!()
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

/// Checks every target at the same time, each under the configured timeout.
pub fn probe(config: &Config, check: Check) -> Probe {
    todo!()
}

/// What `main` does: 0 when every target is up, 1 when some are not, 2 when the configuration
/// is wrong. Returns the exit code and the log lines to print.
pub fn run(lookup: impl Fn(&str) -> Option<String>, check: Check) -> (i32, Vec<String>) {
    todo!()
}
