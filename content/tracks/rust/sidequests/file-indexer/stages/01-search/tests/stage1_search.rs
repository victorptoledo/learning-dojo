//! Stage 1 contract: `findex search <dir> <text>` prints `path:line: content`.

use std::fs;
use std::path::{Path, PathBuf};
use std::process::Command;

/// A throwaway folder under the system temp dir, removed when dropped.
struct Tree(PathBuf);

impl Tree {
    fn new(name: &str) -> Self {
        let dir = std::env::temp_dir().join(format!("findex1-{}-{name}", std::process::id()));
        let _ = fs::remove_dir_all(&dir);
        fs::create_dir_all(&dir).unwrap();
        Tree(dir)
    }

    fn file(&self, relative: &str, text: &str) -> &Self {
        let path = self.0.join(relative);
        fs::create_dir_all(path.parent().unwrap()).unwrap();
        fs::write(path, text).unwrap();
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

fn search(dir: &Path, text: &str) -> (String, i32) {
    let output = Command::new(env!("CARGO_BIN_EXE_findex"))
        .arg("search")
        .arg(dir)
        .arg(text)
        .output()
        .expect("the findex binary runs");
    (
        String::from_utf8_lossy(&output.stdout)
            .trim_end()
            .to_string(),
        output.status.code().unwrap_or(-1),
    )
}

#[test]
fn prints_matching_lines_with_relative_path_and_line_number() {
    let tree = Tree::new("basic");
    tree.file("a.txt", "one\ntwo todo\nthree\n")
        .file("sub/plan.md", "todo first\nnothing here\n");
    let (stdout, code) = search(tree.path(), "todo");
    assert_eq!(stdout, "a.txt:2: two todo\nsub/plan.md:1: todo first");
    assert_eq!(code, 0);
}

#[test]
fn every_matching_line_of_a_file_is_listed() {
    let tree = Tree::new("many");
    tree.file("log.txt", "x=1\ny\nx=2\nx=3\n");
    let (stdout, _) = search(tree.path(), "x=");
    assert_eq!(stdout, "log.txt:1: x=1\nlog.txt:3: x=2\nlog.txt:4: x=3");
}

#[test]
fn results_are_sorted_by_path_then_line() {
    let tree = Tree::new("sorted");
    tree.file("zeta.txt", "hit\n")
        .file("alpha.txt", "miss\nhit\n")
        .file("mid/deep/file.txt", "hit\nhit\n");
    let (stdout, _) = search(tree.path(), "hit");
    assert_eq!(
        stdout,
        "alpha.txt:2: hit\nmid/deep/file.txt:1: hit\nmid/deep/file.txt:2: hit\nzeta.txt:1: hit"
    );
}

#[test]
fn the_search_is_case_sensitive() {
    let tree = Tree::new("case");
    tree.file("a.txt", "Todo\nTODO\ntodo\n");
    let (stdout, _) = search(tree.path(), "todo");
    assert_eq!(stdout, "a.txt:3: todo");
}

#[test]
fn nothing_matched_exits_with_1_and_prints_nothing() {
    let tree = Tree::new("none");
    tree.file("a.txt", "hello\n");
    let (stdout, code) = search(tree.path(), "absent");
    assert_eq!((stdout.as_str(), code), ("", 1));
}

#[test]
fn content_is_kept_as_is_without_the_line_ending() {
    let tree = Tree::new("content");
    tree.file("a.txt", "  indented match  \r\nnext\r\n");
    let (stdout, _) = search(tree.path(), "match");
    assert_eq!(stdout, "a.txt:1:   indented match");
}
