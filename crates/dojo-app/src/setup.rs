//! The "prepare your machine" step shown before a node: checks every requirement,
//! installs what can be installed, updates on request. Work runs on a thread.

use crate::environment::{Environment, Status};
use dojo_core::Requirement;
use std::sync::mpsc::{self, Receiver, TryRecvError};
use std::thread;

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum SetupJob {
    Check,
    Install,
    Update,
}

#[derive(Debug, Clone)]
pub struct SetupRow {
    pub requirement: Requirement,
    /// `None` until the first check finishes.
    pub status: Option<Status>,
}

struct JobResult {
    statuses: Vec<Status>,
    log: Vec<String>,
}

pub struct SetupSession {
    pub track_id: String,
    pub node_id: String,
    pub title: String,
    pub rows: Vec<SetupRow>,
    /// What is running right now, for the screen.
    pub busy: Option<&'static str>,
    pub log: Vec<String>,
    /// Continue to the node by itself when the first check finds everything ready.
    pub continue_when_ready: bool,
    receiver: Option<Receiver<JobResult>>,
}

impl SetupSession {
    pub fn new(track_id: &str, node_id: &str, title: &str, requirements: Vec<Requirement>) -> Self {
        Self {
            track_id: track_id.to_string(),
            node_id: node_id.to_string(),
            title: title.to_string(),
            rows: requirements
                .into_iter()
                .map(|requirement| SetupRow {
                    requirement,
                    status: None,
                })
                .collect(),
            busy: None,
            log: Vec::new(),
            continue_when_ready: true,
            receiver: None,
        }
    }

    pub fn start(&mut self, environment: &Environment, job: SetupJob) {
        if self.busy.is_some() {
            return;
        }
        if job != SetupJob::Check {
            self.continue_when_ready = false;
        }
        self.busy = Some(match job {
            SetupJob::Check => "Checking your machine…",
            SetupJob::Install => "Installing what is missing…",
            SetupJob::Update => "Updating everything this step uses…",
        });
        let (sender, receiver) = mpsc::channel();
        let environment = environment.clone();
        let rows = self.rows.clone();
        thread::spawn(move || {
            let _ = sender.send(run_job(&environment, &rows, job));
        });
        self.receiver = Some(receiver);
    }

    /// Applies a finished job. Returns true when one finished during this call.
    pub fn poll(&mut self) -> bool {
        let Some(receiver) = &self.receiver else {
            return false;
        };
        match receiver.try_recv() {
            Ok(result) => {
                for (row, status) in self.rows.iter_mut().zip(result.statuses) {
                    row.status = Some(status);
                }
                self.log.extend(result.log);
                self.busy = None;
                self.receiver = None;
                true
            }
            Err(TryRecvError::Empty) => false,
            Err(TryRecvError::Disconnected) => {
                self.busy = None;
                self.receiver = None;
                true
            }
        }
    }

    /// Blocks the node: a required item is missing, outdated or not checked yet.
    pub fn blocks(&self) -> bool {
        self.rows.iter().any(|row| {
            row.requirement.required && !row.status.as_ref().is_some_and(Status::is_satisfied)
        })
    }

    pub fn all_ready(&self) -> bool {
        self.rows
            .iter()
            .all(|row| row.status.as_ref().is_some_and(Status::is_satisfied))
    }

    /// Blocks until the running job finishes (tests).
    #[cfg(test)]
    pub fn wait(&mut self) {
        while self.busy.is_some() {
            self.poll();
            thread::sleep(std::time::Duration::from_millis(20));
        }
    }
}

fn run_job(environment: &Environment, rows: &[SetupRow], job: SetupJob) -> JobResult {
    let mut log = Vec::new();
    for row in rows {
        let requirement = &row.requirement;
        let needs_install = !row.status.as_ref().is_some_and(Status::is_satisfied);
        let outcome = match job {
            SetupJob::Check => continue,
            SetupJob::Install if !needs_install => continue,
            SetupJob::Install if !environment.can_install(requirement) => {
                Err(format!("install it yourself: {}", hint_of(row)))
            }
            SetupJob::Install => environment.install(requirement),
            SetupJob::Update => environment.update(requirement),
        };
        log.push(match outcome {
            Ok(message) => format!("✓ {}: {message}", requirement.name),
            Err(error) => format!("✗ {}: {error}", requirement.name),
        });
    }
    let statuses = rows
        .iter()
        .map(|row| environment.check(&row.requirement))
        .collect();
    JobResult { statuses, log }
}

fn hint_of(row: &SetupRow) -> String {
    match &row.status {
        Some(Status::Missing { hint }) => hint.clone(),
        Some(Status::Outdated { needed, .. }) => format!("version {needed} or newer is needed"),
        _ => "see the step's instructions".to_string(),
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::editor::Editor;
    use dojo_core::RequirementKind;
    use std::path::Path;

    fn tool(command: &str, required: bool) -> Requirement {
        Requirement {
            name: command.into(),
            why: "needed".into(),
            required,
            kind: RequirementKind::Tool {
                command: command.into(),
                version_args: vec![],
                min_version: None,
                install_hint: format!("install {command}"),
                update: None,
            },
        }
    }

    fn environment() -> Environment {
        Environment::new(Editor::new("explorer"), Path::new("runner-images"))
    }

    #[test]
    fn a_missing_required_tool_blocks_and_an_optional_one_does_not() {
        let mut session = SetupSession::new(
            "rust",
            "rust.p0.setup",
            "Setup",
            vec![tool("cargo", true), tool("no-such-tool-xyz", false)],
        );
        assert!(session.blocks(), "nothing checked yet");

        session.start(&environment(), SetupJob::Check);
        session.wait();

        assert!(!session.blocks());
        assert!(!session.all_ready());
    }

    #[test]
    fn installing_a_tool_explains_how_instead_of_running_anything() {
        let mut session = SetupSession::new(
            "rust",
            "rust.p0.setup",
            "Setup",
            vec![tool("no-such-tool-xyz", true)],
        );
        session.start(&environment(), SetupJob::Check);
        session.wait();

        session.start(&environment(), SetupJob::Install);
        session.wait();

        assert!(session.blocks());
        assert_eq!(
            session.log,
            vec!["✗ no-such-tool-xyz: install it yourself: install no-such-tool-xyz"]
        );
        assert!(!session.continue_when_ready);
    }
}
