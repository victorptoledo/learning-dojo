//! The Docker kata profile (`image: dojo-docker`): the solution is a Dockerfile, maybe
//! with a compose file.
//!
//! 1. The kata folder is built on the host engine with `docker build --network none`
//!    and a throwaway tag. Only `FROM` images are pulled; `RUN` steps get no network.
//!    A failed build is the kata's "does not compile".
//! 2. Facts about the image go to a folder the grader reads at `/facts`: `image.json`
//!    (`docker image inspect`), `history.jsonl`, `files.txt` (every path in the image,
//!    read from `docker export`), `commands/<name>.{out,err,status}` for each line
//!    `name: args...` of `tests/commands.txt` (and of each `tests/*.commands.txt`, one per
//!    sidequest stage), and, after starting the image once, `container.json` and
//!    `logs.txt`.
//! 3. The `dojo-docker` image runs hadolint and the bats tests over the kata (`/work`)
//!    and the facts. While the learner's container is still running, the grader joins
//!    its network namespace, so tests reach its ports on `localhost` and nothing else.
//! 4. The container and the image are removed, whatever happened. The build cache
//!    stays, so a re-check only rebuilds the layers that changed.
//!
//! A kata without a Dockerfile (a compose file only) skips steps 1, 2 and 4. Learner
//! containers run with no network, no capabilities and tight limits; nothing is
//! privileged and the Docker socket is never mounted anywhere.

use crate::docker::{
    DockerRunner, POLL_INTERVAL, SANDBOX_FLAGS, spawn_reader, unique_container_name,
};
use crate::libtest::parse_cargo_test;
use crate::staging::stage_check;
use crate::tar_list::tar_paths;
use dojo_core::{CheckOutcome, CheckReport, Kata, RunnerError};
use std::fs;
use std::path::Path;
use std::process::{Command, Stdio};
use std::thread;
use std::time::{Duration, Instant};
use tempfile::TempDir;

/// Limits for the learner's own containers.
const LEARNER_FLAGS: [&str; 11] = [
    "--network",
    "none",
    "--cpus",
    "1",
    "--memory",
    "256m",
    "--pids-limit",
    "128",
    "--cap-drop",
    "ALL",
    "--security-opt=no-new-privileges",
];

/// A container with no health check counts as started after this long.
const SETTLE: Duration = Duration::from_secs(2);
/// Longest wait for a health check to leave `starting`.
const HEALTH_WAIT: Duration = Duration::from_secs(60);
/// Longest run of one line of `tests/commands.txt`.
const COMMAND_TIMEOUT: Duration = Duration::from_secs(30);
const BUILD_LOG_LINES: usize = 40;

/// Throwaway names for one check.
struct Names {
    base: String,
    image: String,
    container: String,
}

/// Removes the learner's container and image when the check ends, however it ends.
struct Cleanup<'a> {
    runner: &'a DockerRunner,
    names: &'a Names,
}

impl Drop for Cleanup<'_> {
    fn drop(&mut self) {
        let _ = self
            .runner
            .docker_command()
            .args(["rm", "--force", "--volumes", &self.names.container])
            .output();
        let _ = self
            .runner
            .docker_command()
            .args(["image", "rm", "--force", &self.names.image])
            .output();
    }
}

enum Prepared {
    /// Facts written; `running` says whether the learner's container is still up.
    Ready {
        running: bool,
    },
    /// Output in the `cargo test` format with a leading `error:` line.
    Failed(String),
    TimedOut(&'static str),
}

/// What a finished process printed. `code` is `None` when it was killed by a signal.
struct Captured {
    code: Option<i32>,
    stdout: String,
    stderr: String,
}

impl DockerRunner {
    pub(crate) fn check_image_kata(
        &self,
        kata: &Kata,
        solution_dir: &Path,
    ) -> Result<CheckReport, RunnerError> {
        self.ensure_image(&kata.runner.image)?;
        let staging = stage_check(kata, solution_dir)?;
        let facts = TempDir::with_prefix("dojo-facts-")?;
        let started = Instant::now();
        let deadline = started + Duration::from_secs(u64::from(kata.runner.timeout_secs));
        let base = unique_container_name();
        let names = Names {
            image: format!("{base}:kata"),
            container: format!("{base}-app"),
            base,
        };
        let _cleanup = Cleanup {
            runner: self,
            names: &names,
        };

        let network =
            match self.prepare(solution_dir, staging.path(), facts.path(), &names, deadline)? {
                Prepared::Ready { running: true } => format!("container:{}", names.container),
                Prepared::Ready { running: false } => "none".to_string(),
                Prepared::Failed(output) => {
                    return Ok(parse_cargo_test(&output, false, elapsed_ms(started)));
                }
                Prepared::TimedOut(step) => return Ok(timed_out(kata, step, started)),
            };

        let grader = format!("{}-grader", names.base);
        let mut command = self.docker_command();
        command
            .args(["run", "--rm", "--name", &grader, "--network", &network])
            .args(&SANDBOX_FLAGS[2..])
            .arg("--mount")
            .arg(bind(staging.path(), "/work"))
            .arg("--mount")
            .arg(bind(facts.path(), "/facts"))
            .arg(&kata.runner.image);
        Ok(
            match self.run_captured(&mut command, deadline, Some(&grader))? {
                Some(run) => parse_cargo_test(
                    &format!("{}\n{}", run.stdout, run.stderr),
                    run.code == Some(0),
                    elapsed_ms(started),
                ),
                None => timed_out(kata, "running the tests", started),
            },
        )
    }

    fn prepare(
        &self,
        context: &Path,
        staging: &Path,
        facts: &Path,
        names: &Names,
        deadline: Instant,
    ) -> Result<Prepared, RunnerError> {
        if !context.join("Dockerfile").is_file() {
            return Ok(Prepared::Ready { running: false });
        }
        let mut build = self.docker_command();
        build
            .args(["build", "--network", "none", "--progress", "plain"])
            .args(["--tag", &names.image])
            .arg(context);
        let Some(built) = self.run_captured(&mut build, deadline, None)? else {
            return Ok(Prepared::TimedOut("building the image"));
        };
        let log = format!("{}{}", built.stdout, built.stderr);
        fs::write(facts.join("build.log"), &log)?;
        if built.code != Some(0) {
            return Ok(Prepared::Failed(format!(
                "error: the image does not build\n{}",
                tail(&log, BUILD_LOG_LINES)
            )));
        }

        self.save_output(
            &["image", "inspect", &names.image],
            &facts.join("image.json"),
        )?;
        self.save_output(
            &[
                "history",
                "--no-trunc",
                "--format",
                "{{json .}}",
                &names.image,
            ],
            &facts.join("history.jsonl"),
        )?;
        let created = self
            .docker_command()
            .args(["create", "--name", &names.container])
            .args(LEARNER_FLAGS)
            .arg(&names.image)
            .output()?;
        if !created.status.success() {
            return Ok(Prepared::Failed(format!(
                "error: the container cannot be created\n{}",
                String::from_utf8_lossy(&created.stderr)
            )));
        }
        self.save_file_list(&names.container, &facts.join("files.txt"))?;
        if !self.run_commands(staging, facts, names, deadline)? {
            return Ok(Prepared::TimedOut("running tests/commands.txt"));
        }

        let _ = self
            .docker_command()
            .args(["start", &names.container])
            .output()?;
        let running = self.wait_until_settled(&names.container, deadline);
        self.save_output(
            &["inspect", &names.container],
            &facts.join("container.json"),
        )?;
        let logs = self
            .docker_command()
            .args(["logs", &names.container])
            .output()?;
        fs::write(
            facts.join("logs.txt"),
            [logs.stdout.as_slice(), logs.stderr.as_slice()].concat(),
        )?;
        Ok(Prepared::Ready { running })
    }

    /// Runs each line of the commands files (`tests/commands.txt`, then each
    /// `tests/*.commands.txt`) in a fresh container of the image.
    /// False when the deadline passed.
    fn run_commands(
        &self,
        staging: &Path,
        facts: &Path,
        names: &Names,
        deadline: Instant,
    ) -> Result<bool, RunnerError> {
        let text = commands_text(&staging.join("tests"));
        if text.is_empty() {
            return Ok(true);
        }
        let dir = facts.join("commands");
        fs::create_dir_all(&dir)?;
        for (index, (name, args)) in parse_commands(&text).into_iter().enumerate() {
            let container = format!("{}-cmd{index}", names.base);
            let mut command = self.docker_command();
            command
                .args(["run", "--rm", "--name", &container])
                .args(LEARNER_FLAGS)
                .arg(&names.image)
                .args(&args);
            let until = deadline.min(Instant::now() + COMMAND_TIMEOUT);
            let run = self.run_captured(&mut command, until, Some(&container))?;
            let status = match &run {
                Some(Captured {
                    code: Some(code), ..
                }) => code.to_string(),
                Some(_) => "signal".to_string(),
                None => "timeout".to_string(),
            };
            let (stdout, stderr) = run.map(|run| (run.stdout, run.stderr)).unwrap_or_default();
            fs::write(dir.join(format!("{name}.out")), stdout)?;
            fs::write(dir.join(format!("{name}.err")), stderr)?;
            fs::write(dir.join(format!("{name}.status")), status)?;
            if Instant::now() >= deadline {
                return Ok(false);
            }
        }
        Ok(true)
    }

    /// Polls the container until it stopped, its health check decided, or it has run
    /// long enough. True when it is still running.
    fn wait_until_settled(&self, container: &str, deadline: Instant) -> bool {
        let started = Instant::now();
        loop {
            let state = self
                .docker_command()
                .args(["inspect", "--format"])
                .arg("{{.State.Status}} {{if .State.Health}}{{.State.Health.Status}}{{end}}")
                .arg(container)
                .output()
                .map(|output| String::from_utf8_lossy(&output.stdout).trim().to_string())
                .unwrap_or_default();
            let (status, health) = state.split_once(' ').unwrap_or((state.as_str(), ""));
            match settle(status, health, started.elapsed()) {
                Settled::Running => return true,
                Settled::Stopped => return false,
                Settled::Wait if Instant::now() >= deadline => return status == "running",
                Settled::Wait => thread::sleep(POLL_INTERVAL * 2),
            }
        }
    }

    fn save_output(&self, args: &[&str], file: &Path) -> Result<(), RunnerError> {
        let output = self.docker_command().args(args).output()?;
        fs::write(file, output.stdout)?;
        Ok(())
    }

    /// Every path in the container's file system, one per line.
    fn save_file_list(&self, container: &str, file: &Path) -> Result<(), RunnerError> {
        let mut export = self
            .docker_command()
            .args(["export", container])
            .stdout(Stdio::piped())
            .stderr(Stdio::null())
            .spawn()?;
        let paths = match export.stdout.take() {
            Some(stdout) => tar_paths(stdout),
            None => Ok(Vec::new()),
        };
        export.wait()?;
        let paths = paths.map_err(|error| format!("cannot list the image files: {error}"))?;
        fs::write(file, paths.join("\n") + "\n")?;
        Ok(())
    }

    /// Runs a command, reading its output while it runs, and kills it at the deadline
    /// (with `docker kill` when it runs a named container). `None` on timeout.
    fn run_captured(
        &self,
        command: &mut Command,
        deadline: Instant,
        container: Option<&str>,
    ) -> Result<Option<Captured>, RunnerError> {
        let mut child = command
            .stdout(Stdio::piped())
            .stderr(Stdio::piped())
            .spawn()
            .map_err(|error| format!("cannot start docker: {error}"))?;
        let stdout = child.stdout.take().map(spawn_reader);
        let stderr = child.stderr.take().map(spawn_reader);
        let code = loop {
            if let Some(status) = child.try_wait()? {
                break Some(status.code());
            }
            if Instant::now() >= deadline {
                if let Some(name) = container {
                    let _ = self.docker_command().args(["kill", name]).output();
                }
                let _ = child.kill();
                let _ = child.wait();
                break None;
            }
            thread::sleep(POLL_INTERVAL);
        };
        let read = |reader: Option<thread::JoinHandle<String>>| {
            reader
                .map(|r| r.join().unwrap_or_default())
                .unwrap_or_default()
        };
        let (stdout, stderr) = (read(stdout), read(stderr));
        Ok(code.map(|code| Captured {
            code,
            stdout,
            stderr,
        }))
    }
}

#[derive(Debug, PartialEq, Eq)]
enum Settled {
    Running,
    Stopped,
    Wait,
}

/// `status` and `health` as `docker inspect` reports them; `health` is empty when the
/// image has no health check.
fn settle(status: &str, health: &str, waited: Duration) -> Settled {
    if status != "running" {
        return Settled::Stopped;
    }
    let done = match health {
        "" => waited >= SETTLE,
        "starting" => waited >= HEALTH_WAIT,
        _ => true,
    };
    if done {
        Settled::Running
    } else {
        Settled::Wait
    }
}

/// The commands files of a tests folder, joined: `commands.txt` first, then every
/// `*.commands.txt` by name. Sidequest stages each bring their own file, because
/// stages cannot share a test file.
fn commands_text(tests: &Path) -> String {
    let mut extra: Vec<_> = fs::read_dir(tests)
        .into_iter()
        .flatten()
        .flatten()
        .map(|entry| entry.path())
        .filter(|path| {
            path.is_file()
                && path
                    .file_name()
                    .and_then(|name| name.to_str())
                    .is_some_and(|name| name.ends_with(".commands.txt"))
        })
        .collect();
    extra.sort();
    std::iter::once(tests.join("commands.txt"))
        .chain(extra)
        .filter_map(|path| fs::read_to_string(path).ok())
        .map(|text| text + "\n")
        .collect()
}

/// `name: args...` lines; blank lines and `#` comments are skipped, and so are lines
/// whose name is not made of lower-case letters, digits, `-` and `_`.
fn parse_commands(text: &str) -> Vec<(String, Vec<String>)> {
    text.lines()
        .map(str::trim)
        .filter(|line| !line.is_empty() && !line.starts_with('#'))
        .filter_map(|line| {
            let (name, args) = line.split_once(':')?;
            let name = name.trim();
            let valid = !name.is_empty()
                && name
                    .chars()
                    .all(|c| c.is_ascii_lowercase() || c.is_ascii_digit() || c == '-' || c == '_');
            valid.then(|| {
                (
                    name.to_string(),
                    args.split_whitespace().map(str::to_string).collect(),
                )
            })
        })
        .collect()
}

fn bind(source: &Path, target: &str) -> String {
    format!(
        "type=bind,source={},target={target},readonly",
        source.display()
    )
}

fn tail(text: &str, lines: usize) -> String {
    let all: Vec<&str> = text.lines().collect();
    all[all.len().saturating_sub(lines)..].join("\n")
}

fn elapsed_ms(started: Instant) -> u64 {
    started.elapsed().as_millis() as u64
}

fn timed_out(kata: &Kata, step: &str, started: Instant) -> CheckReport {
    CheckReport {
        outcome: CheckOutcome::TimedOut,
        tests: Vec::new(),
        details: format!(
            "Stopped after {} s while {step}. Look for a slow or hanging step.",
            kata.runner.timeout_secs
        ),
        duration_ms: elapsed_ms(started),
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn the_grader_keeps_every_sandbox_limit_but_the_network() {
        assert_eq!(&SANDBOX_FLAGS[..2], ["--network", "none"]);
        assert!(SANDBOX_FLAGS[2..].contains(&"--cap-drop"));
        assert_eq!(&LEARNER_FLAGS[..2], ["--network", "none"]);
        assert!(LEARNER_FLAGS.contains(&"--security-opt=no-new-privileges"));
    }

    #[test]
    fn a_stopped_container_is_settled_at_once() {
        assert_eq!(settle("exited", "", Duration::ZERO), Settled::Stopped);
        assert_eq!(settle("created", "", Duration::ZERO), Settled::Stopped);
    }

    #[test]
    fn a_container_without_health_check_settles_after_a_moment() {
        assert_eq!(
            settle("running", "", Duration::from_millis(500)),
            Settled::Wait
        );
        assert_eq!(settle("running", "", SETTLE), Settled::Running);
    }

    #[test]
    fn a_health_check_is_awaited_until_it_decides() {
        assert_eq!(settle("running", "starting", SETTLE * 3), Settled::Wait);
        assert_eq!(
            settle("running", "healthy", Duration::ZERO),
            Settled::Running
        );
        assert_eq!(
            settle("running", "unhealthy", Duration::ZERO),
            Settled::Running
        );
        assert_eq!(settle("running", "starting", HEALTH_WAIT), Settled::Running);
    }

    #[test]
    fn commands_file_lines_become_name_and_arguments() {
        let text = "# probes\n\nversion: --version\ndefault:\ngreet: Ada  Lovelace\nBad Name: x\nno colon\n";

        let commands = parse_commands(text);

        assert_eq!(
            commands,
            [
                ("version".to_string(), vec!["--version".to_string()]),
                ("default".to_string(), vec![]),
                (
                    "greet".to_string(),
                    vec!["Ada".to_string(), "Lovelace".to_string()]
                ),
            ]
        );
    }

    #[test]
    fn every_commands_file_of_the_tests_folder_is_read_in_order() {
        let tests = tempfile::tempdir().unwrap();
        fs::write(tests.path().join("stage2.commands.txt"), "two: b").unwrap();
        fs::write(tests.path().join("commands.txt"), "base: a").unwrap();
        fs::write(tests.path().join("stage1.commands.txt"), "one: c").unwrap();
        fs::write(tests.path().join("notes.txt"), "skipped: d").unwrap();

        let names: Vec<String> = parse_commands(&commands_text(tests.path()))
            .into_iter()
            .map(|(name, _)| name)
            .collect();

        assert_eq!(names, ["base", "one", "two"]);
        assert_eq!(commands_text(&tests.path().join("missing")), "");
    }

    #[test]
    fn the_build_log_keeps_its_last_lines() {
        assert_eq!(tail("a\nb\nc\nd", 2), "c\nd");
        assert_eq!(tail("a", 5), "a");
    }

    #[test]
    fn mounts_are_read_only() {
        assert_eq!(
            bind(Path::new("/tmp/x"), "/work"),
            "type=bind,source=/tmp/x,target=/work,readonly"
        );
    }
}
