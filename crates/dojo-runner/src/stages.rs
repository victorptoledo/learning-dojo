//! Sidequest stages: checks made of several runs, and the folders `verify-katas` checks.

use crate::staging::copy_dir;
use dojo_core::{CheckPart, CheckReport, KataRunner, RunnerError, SidequestStage};
use std::fs;
use std::io;
use std::path::{Path, PathBuf};
use tempfile::TempDir;

/// Runs every part of a stage check on `solution_dir` and combines the reports. A part
/// with several test folders runs them together from one merged copy.
pub fn check_parts(
    runner: &dyn KataRunner,
    parts: &[CheckPart],
    solution_dir: &Path,
) -> Result<CheckReport, RunnerError> {
    let mut reports = Vec::new();
    for part in parts {
        let merged = merge_tests(&part.tests_dirs)?;
        let mut kata = part.kata.clone();
        kata.tests_dir = merged.path().to_path_buf();
        let report = runner.check(&kata, solution_dir)?;
        reports.push((part.kata.runner.image.clone(), report));
    }
    Ok(CheckReport::combine(reports))
}

fn merge_tests(dirs: &[PathBuf]) -> io::Result<TempDir> {
    let merged = TempDir::with_prefix("dojo-stage-tests-")?;
    for dir in dirs {
        copy_dir(dir, merged.path())?;
    }
    Ok(merged)
}

/// Copies the files of `from` that `to` does not have yet: a stage opening adds its
/// starter files and never overwrites the learner's work.
pub fn copy_missing(from: &Path, to: &Path) -> io::Result<()> {
    if !from.is_dir() {
        return Ok(());
    }
    fs::create_dir_all(to)?;
    for entry in fs::read_dir(from)? {
        let entry = entry?;
        let target = to.join(entry.file_name());
        if entry.file_type()?.is_dir() {
            copy_missing(&entry.path(), &target)?;
        } else if !target.exists() {
            fs::copy(entry.path(), target)?;
        }
    }
    Ok(())
}

/// The learner's folder right after stage `index` opens, as a correct learner would
/// have it: the reference of every earlier stage, plus each stage's starter files.
/// With `solved`, stage `index`'s own reference is laid over it too.
pub fn stage_sidequest(
    stages: &[SidequestStage],
    index: usize,
    solved: bool,
) -> io::Result<TempDir> {
    let staging = TempDir::with_prefix("dojo-sidequest-")?;
    for (position, stage) in stages.iter().enumerate().take(index + 1) {
        copy_missing(&stage.kata.starter_dir, staging.path())?;
        if position < index || solved {
            copy_dir(&stage.kata.reference_dir, staging.path())?;
        }
    }
    Ok(staging)
}

#[cfg(test)]
mod tests {
    use super::*;
    use dojo_core::{CheckOutcome, Kata, RunnerSpec, TestCase};
    use std::sync::Mutex;

    fn write(path: &Path, text: &str) {
        fs::create_dir_all(path.parent().unwrap()).unwrap();
        fs::write(path, text).unwrap();
    }

    fn kata(root: &Path, slug: &str, image: &str) -> Kata {
        Kata {
            statement: String::new(),
            starter_dir: root.join(slug).join("starter"),
            tests_dir: root.join(slug).join("tests"),
            reference_dir: root.join(slug).join("reference"),
            runner: RunnerSpec {
                image: image.into(),
                timeout_secs: 60,
            },
            hints: vec![],
        }
    }

    fn stage(root: &Path, slug: &str) -> SidequestStage {
        SidequestStage {
            id: format!("q.{slug}"),
            slug: slug.into(),
            title: slug.into(),
            kata: kata(root, slug, "dojo-rust"),
        }
    }

    /// Records the test files each run saw and passes every run but `dojo-devops`.
    struct Recorder(Mutex<Vec<(String, Vec<String>)>>);

    impl KataRunner for Recorder {
        fn check(&self, kata: &Kata, _: &Path) -> Result<CheckReport, RunnerError> {
            let mut files: Vec<String> = fs::read_dir(&kata.tests_dir)?
                .map(|entry| entry.unwrap().file_name().to_string_lossy().into_owned())
                .collect();
            files.sort();
            self.0
                .lock()
                .unwrap()
                .push((kata.runner.image.clone(), files.clone()));
            let passed = kata.runner.image != "dojo-devops";
            Ok(CheckReport {
                outcome: if passed {
                    CheckOutcome::Passed
                } else {
                    CheckOutcome::TestsFailed
                },
                tests: files
                    .into_iter()
                    .map(|name| TestCase {
                        name,
                        passed,
                        message: None,
                    })
                    .collect(),
                details: String::new(),
                duration_ms: 1,
            })
        }
    }

    #[test]
    fn each_part_runs_its_merged_tests_and_the_reports_are_combined() {
        let root = tempfile::tempdir().unwrap();
        write(&root.path().join("one/tests/one.rs"), "1");
        write(&root.path().join("two/tests/two.rs"), "2");
        write(&root.path().join("three/tests/alerts.bats"), "3");
        let parts = vec![
            CheckPart {
                kata: kata(root.path(), "one", "dojo-rust"),
                tests_dirs: vec![root.path().join("one/tests"), root.path().join("two/tests")],
            },
            CheckPart {
                kata: kata(root.path(), "three", "dojo-devops"),
                tests_dirs: vec![root.path().join("three/tests")],
            },
        ];
        let runner = Recorder(Mutex::new(Vec::new()));

        let report = check_parts(&runner, &parts, Path::new("solution")).unwrap();

        let runs = runner.0.lock().unwrap().clone();
        assert_eq!(
            runs,
            [
                (
                    "dojo-rust".to_string(),
                    vec!["one.rs".to_string(), "two.rs".to_string()]
                ),
                ("dojo-devops".to_string(), vec!["alerts.bats".to_string()]),
            ]
        );
        assert_eq!(report.outcome, CheckOutcome::TestsFailed);
        assert_eq!(report.summary(), "2/3 tests passed");
    }

    #[test]
    fn stage_folders_build_on_earlier_references_and_keep_existing_files() {
        let root = tempfile::tempdir().unwrap();
        let base = root.path();
        write(&base.join("one/starter/Cargo.toml"), "manifest");
        write(&base.join("one/starter/src/main.rs"), "fn main() {}");
        write(&base.join("one/reference/src/main.rs"), "solved one");
        write(
            &base.join("two/starter/src/main.rs"),
            "starter two must not win",
        );
        write(&base.join("two/starter/data.txt"), "new in two");
        write(&base.join("two/reference/src/main.rs"), "solved two");
        let stages = [stage(base, "one"), stage(base, "two")];
        let read = |dir: &TempDir, file: &str| fs::read_to_string(dir.path().join(file)).unwrap();

        let opening = stage_sidequest(&stages, 0, false).unwrap();
        assert_eq!(read(&opening, "src/main.rs"), "fn main() {}");

        let second = stage_sidequest(&stages, 1, false).unwrap();
        assert_eq!(read(&second, "src/main.rs"), "solved one");
        assert_eq!(read(&second, "data.txt"), "new in two");
        assert_eq!(read(&second, "Cargo.toml"), "manifest");

        let solved = stage_sidequest(&stages, 1, true).unwrap();
        assert_eq!(read(&solved, "src/main.rs"), "solved two");
    }

    #[test]
    fn copy_missing_ignores_a_missing_source() {
        let root = tempfile::tempdir().unwrap();
        copy_missing(&root.path().join("nothing"), &root.path().join("out")).unwrap();
        assert!(!root.path().join("out").exists());
    }
}
