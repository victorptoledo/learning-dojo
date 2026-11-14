//! Builds the throwaway project a check runs on.

use dojo_core::Kata;
use std::fs;
use std::io;
use std::path::Path;
use std::time::SystemTime;
use tempfile::TempDir;

/// Folders never copied: build output, dependencies and version control.
const SKIPPED: [&str; 4] = ["target", ".git", "node_modules", ".idea"];

/// Copies `from` into `to` recursively, skipping build and VCS folders.
pub fn copy_dir(from: &Path, to: &Path) -> io::Result<()> {
    fs::create_dir_all(to)?;
    for entry in fs::read_dir(from)? {
        let entry = entry?;
        let name = entry.file_name();
        if SKIPPED.iter().any(|skipped| name == *skipped) {
            continue;
        }
        let target = to.join(&name);
        if entry.file_type()?.is_dir() {
            copy_dir(&entry.path(), &target)?;
        } else {
            fs::copy(entry.path(), target)?;
        }
    }
    Ok(())
}

/// Solution plus the grading tests, laid out as one Cargo project.
///
/// Every staged file gets a fresh modification time. Copies keep the original
/// times, and the build cache is shared between checks of the same kata, so an
/// older file could otherwise look up to date and reuse a stale binary.
pub(crate) fn stage_check(kata: &Kata, solution_dir: &Path) -> io::Result<TempDir> {
    let staging = TempDir::with_prefix("dojo-check-")?;
    copy_dir(solution_dir, staging.path())?;
    copy_dir(&kata.tests_dir, &staging.path().join("tests"))?;
    touch_all(staging.path(), SystemTime::now())?;
    Ok(staging)
}

fn touch_all(dir: &Path, now: SystemTime) -> io::Result<()> {
    for entry in fs::read_dir(dir)? {
        let entry = entry?;
        if entry.file_type()?.is_dir() {
            touch_all(&entry.path(), now)?;
        } else {
            fs::File::options()
                .write(true)
                .open(entry.path())?
                .set_modified(now)?;
        }
    }
    Ok(())
}

/// Starter overlaid with the reference solution: what a correct learner would submit.
pub fn stage_reference(kata: &Kata) -> io::Result<TempDir> {
    let staging = TempDir::with_prefix("dojo-reference-")?;
    copy_dir(&kata.starter_dir, staging.path())?;
    copy_dir(&kata.reference_dir, staging.path())?;
    Ok(staging)
}

#[cfg(test)]
mod tests {
    use super::*;
    use dojo_core::RunnerSpec;

    fn write(path: &Path, text: &str) {
        fs::create_dir_all(path.parent().unwrap()).unwrap();
        fs::write(path, text).unwrap();
    }

    #[test]
    fn check_staging_overlays_grading_tests_and_skips_build_output() {
        let root = TempDir::new().unwrap();
        let solution = root.path().join("solution");
        write(&solution.join("src/lib.rs"), "mine");
        write(&solution.join("tests/visible.rs"), "visible");
        write(&solution.join("target/debug/junk"), "junk");
        write(&root.path().join("grading/grading.rs"), "hidden");
        let kata = Kata {
            statement: String::new(),
            starter_dir: root.path().join("starter"),
            tests_dir: root.path().join("grading"),
            reference_dir: root.path().join("reference"),
            runner: RunnerSpec {
                image: "dojo-rust".into(),
                timeout_secs: 60,
            },
        };

        let staged = stage_check(&kata, &solution).unwrap();

        assert_eq!(
            fs::read_to_string(staged.path().join("src/lib.rs")).unwrap(),
            "mine"
        );
        assert_eq!(
            fs::read_to_string(staged.path().join("tests/visible.rs")).unwrap(),
            "visible"
        );
        assert_eq!(
            fs::read_to_string(staged.path().join("tests/grading.rs")).unwrap(),
            "hidden"
        );
        assert!(!staged.path().join("target").exists());
    }

    #[test]
    fn check_staging_refreshes_modification_times() {
        let root = TempDir::new().unwrap();
        let solution = root.path().join("solution");
        write(&solution.join("src/lib.rs"), "old file");
        let long_ago = SystemTime::UNIX_EPOCH + std::time::Duration::from_secs(1_000_000);
        fs::File::options()
            .write(true)
            .open(solution.join("src/lib.rs"))
            .unwrap()
            .set_modified(long_ago)
            .unwrap();
        write(&root.path().join("grading/grading.rs"), "hidden");
        let kata = Kata {
            statement: String::new(),
            starter_dir: root.path().join("starter"),
            tests_dir: root.path().join("grading"),
            reference_dir: root.path().join("reference"),
            runner: RunnerSpec {
                image: "dojo-rust".into(),
                timeout_secs: 60,
            },
        };

        let staged = stage_check(&kata, &solution).unwrap();

        let modified = fs::metadata(staged.path().join("src/lib.rs"))
            .unwrap()
            .modified()
            .unwrap();
        assert!(modified > long_ago + std::time::Duration::from_secs(60));
    }

    #[test]
    fn reference_staging_replaces_starter_files() {
        let root = TempDir::new().unwrap();
        write(&root.path().join("starter/src/lib.rs"), "todo");
        write(&root.path().join("starter/Cargo.toml"), "manifest");
        write(&root.path().join("reference/src/lib.rs"), "solved");
        let kata = Kata {
            statement: String::new(),
            starter_dir: root.path().join("starter"),
            tests_dir: root.path().join("grading"),
            reference_dir: root.path().join("reference"),
            runner: RunnerSpec {
                image: "dojo-rust".into(),
                timeout_secs: 60,
            },
        };

        let staged = stage_reference(&kata).unwrap();

        assert_eq!(
            fs::read_to_string(staged.path().join("src/lib.rs")).unwrap(),
            "solved"
        );
        assert_eq!(
            fs::read_to_string(staged.path().join("Cargo.toml")).unwrap(),
            "manifest"
        );
    }
}
