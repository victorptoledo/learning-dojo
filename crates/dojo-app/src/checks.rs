//! Runs kata checks on a background thread so the TUI stays responsive.

use dojo_core::{CheckReport, Kata, KataRunner};
use std::path::PathBuf;
use std::sync::Arc;
use std::sync::mpsc::{self, Receiver, Sender, TryRecvError};
use std::thread;

/// Which node a check belongs to and where its solution lives.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct CheckTarget {
    pub track_id: String,
    /// The node id, or the practice kata id when `practice` is set.
    pub node_id: String,
    pub kata_dir: PathBuf,
    /// A practice (remedial) kata: passing it gives practice XP, never a completion.
    pub practice: bool,
}

pub struct FinishedCheck {
    pub target: CheckTarget,
    /// `Err` when the checks could not run at all (no Docker, image missing).
    pub result: Result<CheckReport, String>,
}

pub struct CheckQueue {
    runner: Arc<dyn KataRunner + Send + Sync>,
    sender: Sender<FinishedCheck>,
    receiver: Receiver<FinishedCheck>,
    running: Option<CheckTarget>,
}

impl CheckQueue {
    pub fn new(runner: Arc<dyn KataRunner + Send + Sync>) -> Self {
        let (sender, receiver) = mpsc::channel();
        Self {
            runner,
            sender,
            receiver,
            running: None,
        }
    }

    pub fn running(&self) -> Option<&CheckTarget> {
        self.running.as_ref()
    }

    /// Starts a check unless one is already running. Returns whether it started.
    pub fn start(&mut self, target: CheckTarget, kata: Kata) -> bool {
        if self.running.is_some() {
            return false;
        }
        self.running = Some(target.clone());
        let runner = Arc::clone(&self.runner);
        let sender = self.sender.clone();
        thread::spawn(move || {
            let result = runner
                .check(&kata, &target.kata_dir)
                .map_err(|error| error.to_string());
            let _ = sender.send(FinishedCheck { target, result });
        });
        true
    }

    /// The finished check, if the background run completed since the last call.
    pub fn poll(&mut self) -> Option<FinishedCheck> {
        match self.receiver.try_recv() {
            Ok(finished) => {
                self.running = None;
                Some(finished)
            }
            Err(TryRecvError::Empty | TryRecvError::Disconnected) => None,
        }
    }

    /// Blocks until the running check finishes.
    #[cfg(test)]
    pub fn wait(&mut self) -> Option<FinishedCheck> {
        self.running.as_ref()?;
        let finished = self.receiver.recv().ok()?;
        self.running = None;
        Some(finished)
    }
}
