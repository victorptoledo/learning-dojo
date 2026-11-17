//! Stage 2 contract: exit codes 0/1/2, usage errors, and non-UTF-8 files skipped.

use std::fs;
use std::path::{Path, PathBuf};
use std::process::Command;

struct Tree(PathBuf);

impl Tree {
    fn new(name: &str) -> Self {
        let dir = std::env::temp_dir().join(format!("findex2-{}-{name}", std::process::id()));
        let _ = fs::remove_dir_all(&dir);
        fs::create_dir_all(&dir).unwrap();
        Tree(dir)
    }

    fn file(&self, relative: &str, bytes: &[u8]) -> &Self {
        let path = self.0.join(relative);
        fs::create_dir_all(path.parent().unwrap()).unwrap();
        fs::write(path, bytes).unwrap();
        self
    }

    fn path(&self) -> &Path {
        &self.0
    }
}

impl Drop for Tree {
    fn drop(&mut self) {
        let _ = fs::remove_dir_all(&self.0);
    }
}

struct Run {
    stdout: String,
    stderr: String,
    code: i32,
}

fn findex(args: &[&std::ffi::OsStr]) -> Run {
    let output = Command::new(env!("CARGO_BIN_EXE_findex"))
        .args(args)
        .output()
        .expect("the findex binary runs");
    Run {
        stdout: String::from_utf8_lossy(&output.stdout)
            .trim_end()
            .to_string(),
        stderr: String::from_utf8_lossy(&output.stderr)
            .trim_end()
            .to_string(),
        code: output.status.code().unwrap_or(-1),
    }
}

fn os(text: &str) -> &std::ffi::OsStr {
    text.as_ref()
}

#[test]
fn missing_arguments_are_a_usage_error() {
    for args in [vec![], vec![os("search")], vec![os("search"), os(".")]] {
        let run = findex(&args);
        assert_eq!(run.code, 2, "findex {args:?} should exit 2");
        assert!(
            run.stderr.starts_with("usage: findex"),
            "findex {args:?}: stderr was {:?}",
            run.stderr
        );
    }
}

#[test]
fn an_unknown_command_is_a_usage_error() {
    let run = findex(&[os("frobnicate"), os("."), os("x")]);
    assert_eq!(run.code, 2);
    assert!(run.stderr.starts_with("usage: findex"), "{:?}", run.stderr);
}

#[test]
fn a_missing_folder_is_an_error_with_exit_code_2() {
    let missing = std::env::temp_dir().join("findex2-does-not-exist-anywhere");
    let run = findex(&[os("search"), missing.as_os_str(), os("x")]);
    assert_eq!(run.code, 2);
    assert!(
        run.stderr.starts_with("error: cannot read ")
            && run.stderr.contains("findex2-does-not-exist-anywhere"),
        "{:?}",
        run.stderr
    );
    assert!(run.stdout.is_empty());
}

#[test]
fn a_file_instead_of_a_folder_is_an_error() {
    let tree = Tree::new("notdir");
    tree.file("plain.txt", b"x\n");
    let file = tree.path().join("plain.txt");
    let run = findex(&[os("search"), file.as_os_str(), os("x")]);
    assert_eq!(run.code, 2);
    assert!(
        run.stderr.starts_with("error: cannot read "),
        "{:?}",
        run.stderr
    );
}

#[test]
fn a_binary_file_is_skipped_with_a_warning() {
    let tree = Tree::new("binary");
    tree.file("bin.dat", &[0x66, 0x6f, 0xff, 0xfe, 0x0a])
        .file("notes.txt", b"find me\n");
    let run = findex(&[os("search"), tree.path().as_os_str(), os("find")]);
    assert_eq!(run.stdout, "notes.txt:1: find me");
    assert_eq!(run.code, 0, "a warning does not change the exit code");
    assert!(
        run.stderr.contains("warning: skipping bin.dat: not UTF-8"),
        "{:?}",
        run.stderr
    );
}

#[test]
fn only_skipped_files_and_no_match_still_exit_with_1() {
    let tree = Tree::new("onlybinary");
    tree.file("sub/x.bin", &[0xc3, 0x28]);
    let run = findex(&[os("search"), tree.path().as_os_str(), os("x")]);
    assert_eq!(run.code, 1);
    assert!(
        run.stderr
            .contains("warning: skipping sub/x.bin: not UTF-8"),
        "{:?}",
        run.stderr
    );
}
