//! Checkpoint 3: Build Planner (reference solution).

use std::collections::{HashMap, HashSet, VecDeque};
use std::fmt;
use std::panic::{AssertUnwindSafe, catch_unwind};
use std::sync::atomic::{AtomicBool, AtomicUsize, Ordering};
use std::sync::{Arc, Mutex, mpsc};
use std::thread::{self, JoinHandle};

// ---- Part 1: parsing without copying ------------------------------------------------------

/// One line of a build file: a target and the targets it depends on.
/// Every `&str` points into the source text.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct Target<'a> {
    pub name: &'a str,
    pub deps: Vec<&'a str>,
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub enum ParseError {
    MissingColon { line: usize },
    EmptyName { line: usize },
    Duplicate { line: usize, name: String },
}

impl fmt::Display for ParseError {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        match self {
            ParseError::MissingColon { line } => write!(f, "line {line}: missing ':'"),
            ParseError::EmptyName { line } => write!(f, "line {line}: empty target name"),
            ParseError::Duplicate { line, name } => {
                write!(f, "line {line}: target `{name}` is defined twice")
            }
        }
    }
}

impl std::error::Error for ParseError {}

pub fn parse(source: &str) -> Result<Vec<Target<'_>>, ParseError> {
    let mut targets = Vec::new();
    let mut seen = HashSet::new();
    for (index, raw) in source.lines().enumerate() {
        let line = index + 1;
        let text = raw.trim();
        if text.is_empty() || text.starts_with('#') {
            continue;
        }
        let (name, rest) = text.split_once(':').ok_or(ParseError::MissingColon { line })?;
        let name = name.trim();
        if name.is_empty() {
            return Err(ParseError::EmptyName { line });
        }
        if !seen.insert(name) {
            return Err(ParseError::Duplicate { line, name: name.to_string() });
        }
        let mut deps: Vec<&str> = Vec::new();
        for dep in rest.split_whitespace() {
            if !deps.contains(&dep) {
                deps.push(dep);
            }
        }
        targets.push(Target { name, deps });
    }
    Ok(targets)
}

// ---- Part 2: the plan, still borrowed -----------------------------------------------------

#[derive(Debug, Clone, PartialEq, Eq)]
pub enum PlanError {
    UnknownDependency { target: String, dependency: String },
    Cycle { targets: Vec<String> },
}

impl fmt::Display for PlanError {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        match self {
            PlanError::UnknownDependency { target, dependency } => {
                write!(f, "`{target}` depends on unknown target `{dependency}`")
            }
            PlanError::Cycle { targets } => write!(f, "dependency cycle among {}", targets.join(", ")),
        }
    }
}

impl std::error::Error for PlanError {}

/// Groups the targets into waves: each wave only depends on earlier ones.
/// The names borrow from the source, not from `targets`, so the plan outlives the list.
pub fn plan<'a>(targets: &[Target<'a>]) -> Result<Vec<Vec<&'a str>>, PlanError> {
    let names: HashSet<&str> = targets.iter().map(|target| target.name).collect();
    for target in targets {
        if let Some(dep) = target.deps.iter().find(|dep| !names.contains(*dep)) {
            return Err(PlanError::UnknownDependency {
                target: target.name.to_string(),
                dependency: dep.to_string(),
            });
        }
    }

    let mut waiting: HashMap<&'a str, &[&'a str]> =
        targets.iter().map(|target| (target.name, target.deps.as_slice())).collect();
    let mut done: HashSet<&'a str> = HashSet::new();
    let mut waves = Vec::new();
    while !waiting.is_empty() {
        let mut wave: Vec<&'a str> = waiting
            .iter()
            .filter(|(_, deps)| deps.iter().all(|dep| done.contains(dep)))
            .map(|(name, _)| *name)
            .collect();
        if wave.is_empty() {
            let mut stuck: Vec<String> = waiting.keys().map(|name| name.to_string()).collect();
            stuck.sort();
            return Err(PlanError::Cycle { targets: stuck });
        }
        wave.sort_unstable();
        for name in &wave {
            waiting.remove(name);
            done.insert(name);
        }
        waves.push(wave);
    }
    Ok(waves)
}

// ---- Part 3: the build, in the background -------------------------------------------------

/// The work for one target. Shared by every worker thread, so it lives behind an `Arc`.
pub type Job = Arc<dyn Fn(&str) -> Result<u64, String> + Send + Sync>;

#[derive(Debug, Clone, PartialEq, Eq, Default)]
pub struct Report {
    /// Targets whose job returned `Ok`, sorted by name.
    pub built: Vec<(String, u64)>,
    /// Targets whose job returned `Err` or panicked, sorted by name.
    pub failed: Vec<(String, String)>,
    /// Targets whose job never started, sorted.
    pub skipped: Vec<String>,
}

struct Shared {
    finished: AtomicUsize,
    cancelled: AtomicBool,
}

/// A running build. Dropping it without `wait` lets the build finish on its own.
pub struct Build {
    shared: Arc<Shared>,
    coordinator: JoinHandle<Report>,
}

pub fn start(waves: Vec<Vec<String>>, workers: usize, job: Job) -> Build {
    let shared = Arc::new(Shared { finished: AtomicUsize::new(0), cancelled: AtomicBool::new(false) });
    let state = Arc::clone(&shared);
    let workers = workers.max(1);
    let coordinator = thread::spawn(move || {
        let mut report = Report::default();
        let mut stop = false;
        for wave in waves {
            if stop || state.cancelled.load(Ordering::SeqCst) {
                report.skipped.extend(wave);
                continue;
            }
            let failed_before = report.failed.len();
            run_wave(wave, workers, &job, &state, &mut report);
            stop = report.failed.len() > failed_before;
        }
        report.built.sort();
        report.failed.sort();
        report.skipped.sort();
        report
    });
    Build { shared, coordinator }
}

fn run_wave(wave: Vec<String>, workers: usize, job: &Job, state: &Shared, report: &mut Report) {
    let threads = workers.min(wave.len());
    let queue = Mutex::new(VecDeque::from(wave));
    let (sender, results) = mpsc::channel();
    thread::scope(|scope| {
        for _ in 0..threads {
            let sender = sender.clone();
            let queue = &queue;
            scope.spawn(move || {
                loop {
                    if state.cancelled.load(Ordering::SeqCst) {
                        break;
                    }
                    let Some(name) = queue.lock().unwrap().pop_front() else { break };
                    let outcome = catch_unwind(AssertUnwindSafe(|| job(&name)))
                        .unwrap_or_else(|_| Err("panicked".to_string()));
                    state.finished.fetch_add(1, Ordering::SeqCst);
                    sender.send((name, outcome)).unwrap();
                }
            });
        }
    });
    drop(sender);
    for (name, outcome) in results {
        match outcome {
            Ok(value) => report.built.push((name, value)),
            Err(message) => report.failed.push((name, message)),
        }
    }
    report.skipped.extend(queue.into_inner().unwrap());
}

impl Build {
    /// How many jobs have returned so far (built or failed).
    pub fn finished(&self) -> usize {
        self.shared.finished.load(Ordering::SeqCst)
    }

    /// Jobs already running finish; jobs not started yet are skipped.
    pub fn cancel(&self) {
        self.shared.cancelled.store(true, Ordering::SeqCst);
    }

    /// Waits for the build to end and returns its report.
    pub fn wait(self) -> Report {
        self.coordinator.join().expect("the coordinator thread does not panic")
    }
}
