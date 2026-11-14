//! Only one Dojo at a time: two would both watch the workspace and run every
//! check and auto-commit twice. A lock file records the running instance.

use anyhow::{Context, Result};
use chrono::{DateTime, Local};
use std::fs;
use std::io::{self, BufRead, Write};
use std::path::{Path, PathBuf};
use std::process::Command;
use std::thread;
use std::time::Duration;

/// Held while the TUI runs; removes the lock file when dropped.
pub struct InstanceLock {
    path: PathBuf,
}

impl Drop for InstanceLock {
    fn drop(&mut self) {
        if read_lock(&self.path).is_some_and(|holder| holder.pid == std::process::id()) {
            let _ = fs::remove_file(&self.path);
        }
    }
}

#[derive(Debug, Clone, PartialEq, Eq)]
struct Holder {
    pid: u32,
    started: DateTime<Local>,
}

pub enum Acquired {
    Lock(InstanceLock),
    /// Another instance is running and the learner chose to leave it alone.
    Declined,
}

/// Takes the lock, asking first whether to close an instance that is already running.
pub fn acquire(lock_path: &Path) -> Result<Acquired> {
    if let Some(holder) = read_lock(lock_path).filter(|holder| is_running_dojo(holder.pid)) {
        if !confirm_takeover(&holder)? {
            return Ok(Acquired::Declined);
        }
        stop_process(holder.pid);
    }
    write_lock(lock_path, std::process::id(), Local::now())?;
    Ok(Acquired::Lock(InstanceLock {
        path: lock_path.to_path_buf(),
    }))
}

fn confirm_takeover(holder: &Holder) -> Result<bool> {
    print!(
        "Another Dojo is already open (process {}, since {}).\nClose it and open here? [y/N] ",
        holder.pid,
        holder.started.format("%H:%M")
    );
    io::stdout().flush()?;
    let mut answer = String::new();
    io::stdin().lock().read_line(&mut answer)?;
    Ok(matches!(
        answer.trim().to_lowercase().as_str(),
        "y" | "yes" | "s" | "sim"
    ))
}

fn read_lock(path: &Path) -> Option<Holder> {
    let text = fs::read_to_string(path).ok()?;
    let mut lines = text.lines();
    let pid = lines.next()?.trim().parse().ok()?;
    let started = DateTime::parse_from_rfc3339(lines.next()?.trim())
        .ok()?
        .with_timezone(&Local);
    Some(Holder { pid, started })
}

fn write_lock(path: &Path, pid: u32, started: DateTime<Local>) -> Result<()> {
    if let Some(parent) = path.parent() {
        fs::create_dir_all(parent)?;
    }
    fs::write(path, format!("{pid}\n{}\n", started.to_rfc3339()))
        .with_context(|| format!("cannot write {}", path.display()))
}

/// True only for a live process whose executable is a dojo build, so a stale
/// lock whose PID was reused by another program is ignored.
fn is_running_dojo(pid: u32) -> bool {
    process_name(pid).is_some_and(|name| name.to_lowercase().contains("dojo"))
}

#[cfg(windows)]
fn process_name(pid: u32) -> Option<String> {
    let output = Command::new("tasklist")
        .args(["/FI", &format!("PID eq {pid}"), "/FO", "CSV", "/NH"])
        .output()
        .ok()?;
    let text = String::from_utf8_lossy(&output.stdout);
    let first_field = text
        .lines()
        .find(|line| line.starts_with('"'))?
        .split(',')
        .next()?;
    Some(first_field.trim_matches('"').to_string())
}

#[cfg(not(windows))]
fn process_name(pid: u32) -> Option<String> {
    let output = Command::new("ps")
        .args(["-p", &pid.to_string(), "-o", "comm="])
        .output()
        .ok()?;
    let name = String::from_utf8_lossy(&output.stdout).trim().to_string();
    (!name.is_empty()).then_some(name)
}

fn stop_process(pid: u32) {
    let pid = pid.to_string();
    let _ = if cfg!(windows) {
        Command::new("taskkill").args(["/PID", &pid, "/F"]).output()
    } else {
        Command::new("kill").arg(&pid).output()
    };
    // Give the old instance a moment to release its files.
    thread::sleep(Duration::from_millis(300));
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn lock_round_trips_and_is_removed_by_its_owner() {
        let dir = tempfile::tempdir().unwrap();
        let path = dir.path().join("dojo.lock");
        let started = Local::now();

        write_lock(&path, std::process::id(), started).unwrap();
        let holder = read_lock(&path).unwrap();
        assert_eq!(holder.pid, std::process::id());
        assert_eq!(holder.started.timestamp(), started.timestamp());

        drop(InstanceLock { path: path.clone() });
        assert!(!path.exists());
    }

    #[test]
    fn a_lock_held_by_someone_else_is_not_removed() {
        let dir = tempfile::tempdir().unwrap();
        let path = dir.path().join("dojo.lock");
        write_lock(&path, std::process::id() + 1, Local::now()).unwrap();

        drop(InstanceLock { path: path.clone() });

        assert!(path.exists());
    }

    #[test]
    fn this_test_binary_counts_as_a_dojo_but_a_dead_pid_does_not() {
        // Test binaries are named `dojo-<hash>`.
        assert!(is_running_dojo(std::process::id()));
        assert!(!is_running_dojo(0x7FFF_FFF0));
    }

    #[test]
    fn stale_or_garbled_lock_files_are_ignored() {
        let dir = tempfile::tempdir().unwrap();
        let path = dir.path().join("dojo.lock");
        fs::write(&path, "not a pid\n").unwrap();
        assert!(read_lock(&path).is_none());

        write_lock(&path, 0x7FFF_FFF0, Local::now()).unwrap();
        let Acquired::Lock(lock) = acquire(&path).unwrap() else {
            panic!("a dead holder must not block the lock");
        };
        assert_eq!(read_lock(&path).unwrap().pid, std::process::id());

        drop(lock);
        assert!(!path.exists());
    }
}
