//! Stage 4 contract: hidden files skipped, --ignore-case, --ext and multi-word queries.

use std::ffi::OsStr;
use std::fs;
use std::path::PathBuf;
use std::process::Command;

struct Tree(PathBuf);

impl Tree {
    fn new(name: &str) -> Self {
        let dir = std::env::temp_dir().join(format!("findex4-{}-{name}", std::process::id()));
        let _ = fs::remove_dir_all(&dir);
        fs::create_dir_all(dir.join("src")).unwrap();
        Tree(dir)
    }

    fn file(&self, relative: &str, text: &str) -> &Self {
        let path = self.source().join(relative);
        fs::create_dir_all(path.parent().unwrap()).unwrap();
        fs::write(path, text).unwrap();
        self
    }

    fn source(&self) -> PathBuf {
        self.0.join("src")
    }

    fn index_file(&self) -> PathBuf {
        self.0.join("words.idx")
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

fn findex(args: &[&OsStr]) -> Run {
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

fn search(tree: &Tree, text: &str, options: &[&str]) -> Run {
    let source = tree.source();
    let mut args: Vec<&OsStr> = vec!["search".as_ref(), source.as_os_str(), text.as_ref()];
    args.extend(options.iter().map(OsStr::new));
    findex(&args)
}

#[test]
fn hidden_files_and_folders_are_skipped() {
    let tree = Tree::new("hidden");
    tree.file(".git/config", "todo hidden\n")
        .file(".notes.txt", "todo hidden\n")
        .file("visible.txt", "todo\n");
    assert_eq!(search(&tree, "todo", &[]).stdout, "visible.txt:1: todo");

    let index = tree.index_file();
    let run = findex(&[
        "index".as_ref(),
        tree.source().as_os_str(),
        index.as_os_str(),
    ]);
    assert_eq!(run.stdout, "indexed 1 files");
}

#[test]
fn ignore_case_matches_any_case() {
    let tree = Tree::new("icase");
    tree.file("a.txt", "Todo\nTODO\nnothing\ntodo\n");
    let run = search(&tree, "todo", &["--ignore-case"]);
    assert_eq!(run.stdout, "a.txt:1: Todo\na.txt:2: TODO\na.txt:4: todo");
}

#[test]
fn ext_keeps_only_the_listed_extensions() {
    let tree = Tree::new("ext");
    tree.file("main.rs", "fn todo()\n")
        .file("README.md", "todo\n")
        .file("notes.txt", "todo\n")
        .file("Makefile", "todo\n");
    let run = search(&tree, "todo", &["--ext", "rs,md"]);
    assert_eq!(run.stdout, "README.md:1: todo\nmain.rs:1: fn todo()");
}

#[test]
fn options_combine_in_any_order() {
    let tree = Tree::new("both");
    tree.file("a.rs", "TODO\n").file("b.txt", "TODO\n");
    let expected = "a.rs:1: TODO";
    assert_eq!(
        search(&tree, "todo", &["--ext", "rs", "--ignore-case"]).stdout,
        expected
    );
    assert_eq!(
        search(&tree, "todo", &["--ignore-case", "--ext", "rs"]).stdout,
        expected
    );
}

#[test]
fn an_unknown_option_is_a_usage_error() {
    let tree = Tree::new("unknown");
    tree.file("a.txt", "x\n");
    let run = search(&tree, "x", &["--colour"]);
    assert_eq!(run.code, 2);
    assert!(run.stderr.starts_with("usage: findex"), "{:?}", run.stderr);
}

#[test]
fn a_query_with_several_words_needs_all_of_them() {
    let tree = Tree::new("multi");
    tree.file("both.txt", "rust and go, rust again\n")
        .file("rust.txt", "rust only\n")
        .file("go.txt", "go only go go\n")
        .file("also.txt", "Go RUST\n");
    let index = tree.index_file();
    findex(&[
        "index".as_ref(),
        tree.source().as_os_str(),
        index.as_os_str(),
    ]);
    let run = findex(&[
        "query".as_ref(),
        index.as_os_str(),
        "rust".as_ref(),
        "go".as_ref(),
    ]);
    assert_eq!(run.stdout, "both.txt (3)\nalso.txt (2)", "{}", run.stderr);
    assert_eq!(run.code, 0);
}
