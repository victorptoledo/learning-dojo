use crate::libtest::parse_cargo_test;
use crate::staging::stage_check;
use dojo_core::{CheckOutcome, CheckReport, Kata, KataRunner, RunnerError};
use std::collections::hash_map::DefaultHasher;
use std::hash::{Hash, Hasher};
use std::io::Read;
use std::path::Path;
use std::process::{Child, Command, Stdio};
use std::thread;
use std::time::{Duration, Instant, SystemTime, UNIX_EPOCH};

const POLL_INTERVAL: Duration = Duration::from_millis(100);

/// Limits applied to every check container.
const SANDBOX_FLAGS: [&str; 11] = [
    "--network",
    "none",
    "--cpus",
    "2",
    "--memory",
    "1g",
    "--pids-limit",
    "256",
    "--cap-drop",
    "ALL",
    "--security-opt=no-new-privileges",
];

/// Runs checks with the local `docker` CLI.
pub struct DockerRunner {
    docker: String,
}

impl Default for DockerRunner {
    fn default() -> Self {
        Self {
            docker: "docker".to_string(),
        }
    }
}

impl KataRunner for DockerRunner {
    fn check(&self, kata: &Kata, solution_dir: &Path) -> Result<CheckReport, RunnerError> {
        self.ensure_image(&kata.runner.image)?;
        let staging = stage_check(kata, solution_dir)?;
        let container = unique_container_name();
        let started = Instant::now();
        let mut child = self
            .docker_command()
            .args(["run", "--rm", "--name", &container])
            .args(SANDBOX_FLAGS)
            .arg("--mount")
            .arg(format!(
                "type=bind,source={},target=/work",
                staging.path().display()
            ))
            .arg("--mount")
            .arg(format!(
                "type=volume,source={},target=/target",
                cache_volume(kata)
            ))
            .arg(&kata.runner.image)
            .stdout(Stdio::piped())
            .stderr(Stdio::piped())
            .spawn()
            .map_err(|error| format!("cannot start docker: {error}"))?;

        let timeout = Duration::from_secs(u64::from(kata.runner.timeout_secs));
        let finished = self.wait_or_kill(&mut child, &container, timeout)?;
        let output = collect_output(&mut child);
        let duration_ms = started.elapsed().as_millis() as u64;
        Ok(match finished {
            Some(exit_ok) => parse_cargo_test(&output, exit_ok, duration_ms),
            None => CheckReport {
                outcome: CheckOutcome::TimedOut,
                tests: Vec::new(),
                details: format!(
                    "Stopped after {} s. Look for an infinite loop.",
                    kata.runner.timeout_secs
                ),
                duration_ms,
            },
        })
    }
}

impl DockerRunner {
    fn docker_command(&self) -> Command {
        Command::new(&self.docker)
    }

    /// Fails with an actionable message when Docker or the kata image is missing.
    fn ensure_image(&self, image: &str) -> Result<(), RunnerError> {
        let output = self
            .docker_command()
            .args(["image", "inspect", "--format", "{{.Id}}", image])
            .output()
            .map_err(|_| "Docker is not installed (the `docker` command was not found).")?;
        if output.status.success() {
            return Ok(());
        }
        let stderr = String::from_utf8_lossy(&output.stderr);
        if stderr.contains("failed to connect") || stderr.contains("daemon") {
            return Err("Docker is not running. Start Docker Desktop and try again.".into());
        }
        Err(format!("The image `{image}` is not built yet. Run `dojo runner build`.").into())
    }

    /// `Some(exit_ok)` when the container finished, `None` when it was killed on timeout.
    fn wait_or_kill(
        &self,
        child: &mut Child,
        container: &str,
        timeout: Duration,
    ) -> Result<Option<bool>, RunnerError> {
        let deadline = Instant::now() + timeout;
        loop {
            if let Some(status) = child.try_wait()? {
                return Ok(Some(status.success()));
            }
            if Instant::now() >= deadline {
                let _ = self.docker_command().args(["kill", container]).output();
                let _ = child.wait();
                return Ok(None);
            }
            thread::sleep(POLL_INTERVAL);
        }
    }
}

/// Reads whatever the container wrote. The pipes are drained on threads so a chatty
/// build cannot block on a full pipe.
fn collect_output(child: &mut Child) -> String {
    let stdout = child.stdout.take().map(spawn_reader);
    let stderr = child.stderr.take().map(spawn_reader);
    let mut text = String::new();
    for reader in [stdout, stderr].into_iter().flatten() {
        text.push_str(&reader.join().unwrap_or_default());
        text.push('\n');
    }
    text
}

fn spawn_reader(mut pipe: impl Read + Send + 'static) -> thread::JoinHandle<String> {
    thread::spawn(move || {
        let mut bytes = Vec::new();
        let _ = pipe.read_to_end(&mut bytes);
        String::from_utf8_lossy(&bytes).into_owned()
    })
}

fn unique_container_name() -> String {
    let nanos = SystemTime::now()
        .duration_since(UNIX_EPOCH)
        .map(|elapsed| elapsed.as_nanos())
        .unwrap_or_default();
    format!("dojo-check-{}-{nanos}", std::process::id())
}

/// One build cache per kata, so re-checks only recompile the learner's crate.
fn cache_volume(kata: &Kata) -> String {
    let mut hasher = DefaultHasher::new();
    kata.starter_dir.hash(&mut hasher);
    format!("dojo-target-{:016x}", hasher.finish())
}

/// Builds every image under `images_dir/<name>/Dockerfile` as `dojo-<name>`.
/// Docker's own progress output goes straight to the terminal.
pub fn build_images(images_dir: &Path) -> Result<Vec<String>, RunnerError> {
    let mut built = Vec::new();
    let mut dirs: Vec<_> = std::fs::read_dir(images_dir)
        .map_err(|error| format!("cannot read {}: {error}", images_dir.display()))?
        .filter_map(|entry| entry.ok().map(|e| e.path()))
        .filter(|path| path.join("Dockerfile").is_file())
        .collect();
    dirs.sort();
    for dir in dirs {
        let name = dir
            .file_name()
            .map(|n| n.to_string_lossy().into_owned())
            .unwrap_or_default();
        let tag = format!("dojo-{name}");
        let status = Command::new("docker")
            .args(["build", "--tag", &tag])
            .arg(&dir)
            .status()
            .map_err(|error| format!("cannot start docker: {error}"))?;
        if !status.success() {
            return Err(format!("building `{tag}` failed").into());
        }
        built.push(tag);
    }
    Ok(built)
}
