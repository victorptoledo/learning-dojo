use crate::libtest::parse_cargo_test;
use crate::staging::stage_check;
use dojo_core::{CheckOutcome, CheckReport, Kata, KataLanguage, KataRunner, RunnerError};
use std::collections::hash_map::DefaultHasher;
use std::hash::{Hash, Hasher};
use std::io::Read;
use std::path::Path;
use std::process::{Child, Command, Stdio};
use std::thread;
use std::time::{Duration, Instant, SystemTime, UNIX_EPOCH};

pub(crate) const POLL_INTERVAL: Duration = Duration::from_millis(100);

/// Limits applied to every check container.
pub(crate) const SANDBOX_FLAGS: [&str; 11] = [
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
        if kata.language() == KataLanguage::Docker {
            return self.check_image_kata(kata, solution_dir);
        }
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
        let (finished, output) = wait_with_output(&mut child, timeout, || {
            let _ = self.docker_command().args(["kill", &container]).output();
        })?;
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
    pub(crate) fn docker_command(&self) -> Command {
        Command::new(&self.docker)
    }

    /// Fails with an actionable message when Docker or the kata image is missing.
    pub(crate) fn ensure_image(&self, image: &str) -> Result<(), RunnerError> {
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
}

/// Waits for `child` until it exits or `timeout` passes, then calls `kill` and reaps it.
/// The pipes are drained on threads from the start: a check that writes more than the
/// pipe buffer would otherwise block on a full pipe and never exit.
/// `Some(exit_ok)` when it finished, `None` when it was killed; plus stdout and stderr.
fn wait_with_output(
    child: &mut Child,
    timeout: Duration,
    kill: impl FnOnce(),
) -> Result<(Option<bool>, String), RunnerError> {
    let stdout = child.stdout.take().map(spawn_reader);
    let stderr = child.stderr.take().map(spawn_reader);
    let deadline = Instant::now() + timeout;
    let finished = loop {
        if let Some(status) = child.try_wait()? {
            break Some(status.success());
        }
        if Instant::now() >= deadline {
            kill();
            let _ = child.kill();
            let _ = child.wait();
            break None;
        }
        thread::sleep(POLL_INTERVAL);
    };
    let mut text = String::new();
    for reader in [stdout, stderr].into_iter().flatten() {
        text.push_str(&reader.join().unwrap_or_default());
        text.push('\n');
    }
    Ok((finished, text))
}

pub(crate) fn spawn_reader(mut pipe: impl Read + Send + 'static) -> thread::JoinHandle<String> {
    thread::spawn(move || {
        let mut bytes = Vec::new();
        let _ = pipe.read_to_end(&mut bytes);
        String::from_utf8_lossy(&bytes).into_owned()
    })
}

pub(crate) fn unique_container_name() -> String {
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

#[cfg(test)]
mod tests {
    use super::*;

    /// A command that prints the file at `path` to stdout.
    fn print_file(path: &Path) -> Command {
        if cfg!(windows) {
            let mut command = Command::new("cmd");
            command.arg("/C").arg("type").arg(path);
            command
        } else {
            let mut command = Command::new("cat");
            command.arg(path);
            command
        }
    }

    #[test]
    fn output_larger_than_the_pipe_buffer_does_not_block_the_check() {
        let dir = tempfile::tempdir().unwrap();
        let file = dir.path().join("big.txt");
        let line = "x".repeat(99) + "\n";
        std::fs::write(&file, line.repeat(10_000)).unwrap();
        let mut child = print_file(&file)
            .stdout(Stdio::piped())
            .stderr(Stdio::piped())
            .spawn()
            .unwrap();

        let started = Instant::now();
        let (finished, output) =
            wait_with_output(&mut child, Duration::from_secs(30), || {}).unwrap();

        assert_eq!(finished, Some(true));
        assert!(started.elapsed() < Duration::from_secs(20));
        assert!(output.len() >= 1_000_000, "read {} bytes", output.len());
    }
}
