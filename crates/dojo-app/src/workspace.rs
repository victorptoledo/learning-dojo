//! The learner's solutions folder: one directory per kata, committed to git when it passes.

use anyhow::{Context, Result};
use dojo_core::{CheckReport, Kata, Node, Phase, Track};
use dojo_runner::copy_dir;
use std::path::{Path, PathBuf};
use std::process::Command;

/// The kata statement, copied into the kata folder so it opens next to the code.
pub const STATEMENT_FILE: &str = "README.md";
/// The folder of a track (next to its phases) that holds its practice katas.
pub const PRACTICE_FOLDER: &str = "practice";

#[derive(Debug, PartialEq, Eq)]
pub enum Prepared {
    Created,
    AlreadyThere,
}

#[derive(Debug, PartialEq, Eq)]
pub enum CommitOutcome {
    Committed,
    NothingToCommit,
    /// The workspace is not a git repository, or git is not installed.
    GitUnavailable(String),
}

#[derive(Debug, Clone)]
pub struct Workspace {
    root: PathBuf,
}

impl Workspace {
    pub fn new(root: &Path) -> Self {
        Self {
            root: root.to_path_buf(),
        }
    }

    pub fn root(&self) -> &Path {
        &self.root
    }

    /// `<root>/<track>/<phase>/<slug>`, e.g. `rust/p1/control-flow-workout`.
    pub fn kata_dir(&self, track: &Track, phase: &Phase, node: &Node) -> PathBuf {
        self.root.join(relative_kata_path(track, phase, node))
    }

    /// `<root>/<track>/practice/<phase>-<node slug>-<practice slug>`: practice katas live
    /// apart from the track's own katas, so their checks never touch the node's folder.
    pub fn practice_dir(&self, track: &Track, phase: &Phase, node: &Node, slug: &str) -> PathBuf {
        let node_slug = node.id.rsplit('.').next().unwrap_or(&node.id);
        self.root
            .join(&track.id)
            .join(PRACTICE_FOLDER)
            .join(format!("{}-{node_slug}-{slug}", phase.id))
    }

    /// Copies the starter the first time; never overwrites an existing solution.
    pub fn prepare(&self, kata: &Kata, kata_dir: &Path) -> Result<Prepared> {
        if kata_dir.exists() {
            let statement = kata_dir.join(STATEMENT_FILE);
            if !statement.exists() {
                std::fs::write(&statement, &kata.statement)
                    .with_context(|| format!("cannot write {}", statement.display()))?;
            }
            return Ok(Prepared::AlreadyThere);
        }
        copy_dir(&kata.starter_dir, kata_dir)
            .with_context(|| format!("cannot create {}", kata_dir.display()))?;
        std::fs::write(kata_dir.join(STATEMENT_FILE), &kata.statement)
            .with_context(|| format!("cannot write the statement in {}", kata_dir.display()))?;
        Ok(Prepared::Created)
    }

    /// Commits only `kata_dir` with a message such as `rust/p1/loops: passed (6/6 tests)`.
    pub fn commit_pass(&self, kata_dir: &Path, report: &CheckReport) -> CommitOutcome {
        let Ok(relative) = kata_dir.strip_prefix(&self.root) else {
            return CommitOutcome::GitUnavailable("the kata is outside the workspace".to_string());
        };
        let pathspec = relative.to_string_lossy().replace('\\', "/");
        if let Err(reason) = self.git(&["rev-parse", "--is-inside-work-tree"]) {
            return CommitOutcome::GitUnavailable(reason);
        }
        if let Err(reason) = self.git(&["add", "--", &pathspec]) {
            return CommitOutcome::GitUnavailable(reason);
        }
        if self
            .git(&["diff", "--cached", "--quiet", "--", &pathspec])
            .is_ok()
        {
            return CommitOutcome::NothingToCommit;
        }
        let message = format!("{pathspec}: passed ({})", report.summary());
        match self.git(&["commit", "--quiet", "--message", &message, "--", &pathspec]) {
            Ok(()) => CommitOutcome::Committed,
            Err(reason) => CommitOutcome::GitUnavailable(reason),
        }
    }

    fn git(&self, args: &[&str]) -> Result<(), String> {
        let output = Command::new("git")
            .arg("-C")
            .arg(&self.root)
            .args(args)
            .output()
            .map_err(|_| "git is not installed".to_string())?;
        if output.status.success() {
            Ok(())
        } else {
            Err(String::from_utf8_lossy(&output.stderr).trim().to_string())
        }
    }
}

fn relative_kata_path(track: &Track, phase: &Phase, node: &Node) -> PathBuf {
    let slug = node.id.rsplit('.').next().unwrap_or(&node.id);
    PathBuf::from(&track.id).join(&phase.id).join(slug)
}

#[cfg(test)]
mod tests {
    use super::*;
    use dojo_core::{CheckOutcome, RunnerSpec, TestCase};
    use std::fs;

    fn kata_with_starter(root: &Path) -> Kata {
        let starter = root.join("starter");
        fs::create_dir_all(starter.join("src")).unwrap();
        fs::write(starter.join("src/lib.rs"), "todo").unwrap();
        Kata {
            statement: String::new(),
            starter_dir: starter,
            tests_dir: root.join("tests"),
            reference_dir: root.join("reference"),
            runner: RunnerSpec {
                image: "dojo-rust".into(),
                timeout_secs: 60,
            },
            hints: vec![],
        }
    }

    fn passing_report() -> CheckReport {
        CheckReport {
            outcome: CheckOutcome::Passed,
            tests: vec![TestCase {
                name: "a".into(),
                passed: true,
                message: None,
            }],
            details: String::new(),
            duration_ms: 1,
        }
    }

    fn git(dir: &Path, args: &[&str]) -> String {
        let output = Command::new("git")
            .arg("-C")
            .arg(dir)
            .args(args)
            .output()
            .unwrap();
        assert!(
            output.status.success(),
            "{}",
            String::from_utf8_lossy(&output.stderr)
        );
        String::from_utf8_lossy(&output.stdout).trim().to_string()
    }

    #[test]
    fn prepare_copies_the_starter_once_and_keeps_learner_changes() {
        let dir = tempfile::tempdir().unwrap();
        let kata = kata_with_starter(dir.path());
        let workspace = Workspace::new(&dir.path().join("ws"));
        let kata_dir = workspace.root().join("rust/p0/hello");

        assert_eq!(
            workspace.prepare(&kata, &kata_dir).unwrap(),
            Prepared::Created
        );
        fs::write(kata_dir.join("src/lib.rs"), "my solution").unwrap();
        assert_eq!(
            workspace.prepare(&kata, &kata_dir).unwrap(),
            Prepared::AlreadyThere
        );

        assert_eq!(
            fs::read_to_string(kata_dir.join("src/lib.rs")).unwrap(),
            "my solution"
        );
    }

    #[test]
    fn commit_pass_commits_only_the_kata_folder_and_only_when_it_changed() {
        let dir = tempfile::tempdir().unwrap();
        let root = dir.path().join("ws");
        fs::create_dir_all(&root).unwrap();
        git(&root, &["init", "--quiet", "-b", "main"]);
        git(&root, &["config", "user.name", "Test"]);
        git(&root, &["config", "user.email", "test@example.com"]);
        fs::write(root.join("notes.txt"), "not mine to commit").unwrap();
        let workspace = Workspace::new(&root);
        let kata_dir = root.join("rust/p0/hello");
        workspace
            .prepare(&kata_with_starter(dir.path()), &kata_dir)
            .unwrap();

        assert_eq!(
            workspace.commit_pass(&kata_dir, &passing_report()),
            CommitOutcome::Committed
        );
        assert_eq!(
            git(&root, &["log", "-1", "--format=%s"]),
            "rust/p0/hello: passed (1/1 tests passed)"
        );
        assert_eq!(git(&root, &["status", "--porcelain"]), "?? notes.txt");
        assert_eq!(
            workspace.commit_pass(&kata_dir, &passing_report()),
            CommitOutcome::NothingToCommit
        );
    }

    #[test]
    fn commit_pass_outside_a_repository_reports_git_unavailable() {
        let dir = tempfile::tempdir().unwrap();
        let workspace = Workspace::new(dir.path());
        let kata_dir = dir.path().join("rust/p0/hello");
        fs::create_dir_all(&kata_dir).unwrap();

        assert!(matches!(
            workspace.commit_pass(&kata_dir, &passing_report()),
            CommitOutcome::GitUnavailable(_)
        ));
    }
}
