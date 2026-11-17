//! Stage 3 contract: `findex index` writes an index that `findex query` answers alone.

use std::fs;
use std::path::{Path, PathBuf};
use std::process::Command;

struct Tree(PathBuf);

impl Tree {
    fn new(name: &str) -> Self {
        let dir = std::env::temp_dir().join(format!("findex3-{}-{name}", std::process::id()));
        let _ = fs::remove_dir_all(&dir);
        fs::create_dir_all(dir.join("src")).unwrap();
        Tree(dir)
    }

    /// A file of the folder that gets indexed (`src/`).
    fn file(&self, relative: &str, bytes: &[u8]) -> &Self {
        let path = self.source().join(relative);
        fs::create_dir_all(path.parent().unwrap()).unwrap();
        fs::write(path, bytes).unwrap();
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

fn index(tree: &Tree) -> Run {
    findex(&[
        "index".as_ref(),
        tree.source().as_os_str(),
        tree.index_file().as_os_str(),
    ])
}

fn query(index: &Path, words: &[&str]) -> Run {
    let mut args: Vec<&std::ffi::OsStr> = vec!["query".as_ref(), index.as_os_str()];
    args.extend(words.iter().map(|word| std::ffi::OsStr::new(word)));
    findex(&args)
}

#[test]
fn index_reports_how_many_files_it_read() {
    let tree = Tree::new("count");
    tree.file("a.txt", b"alpha\n")
        .file("b/c.txt", b"beta\n")
        .file("d.bin", &[0xff, 0xfe]);
    let run = index(&tree);
    assert_eq!(run.stdout, "indexed 2 files", "{}", run.stderr);
    assert_eq!(run.code, 0);
    assert!(tree.index_file().is_file(), "the index file is written");
}

#[test]
fn a_query_needs_only_the_index() {
    let tree = Tree::new("alone");
    tree.file("notes/rust.md", b"Rust is fun. rust!\n");
    index(&tree);
    fs::remove_dir_all(tree.source()).unwrap();
    let run = query(&tree.index_file(), &["rust"]);
    assert_eq!(run.stdout, "notes/rust.md (2)", "{}", run.stderr);
    assert_eq!(run.code, 0);
}

#[test]
fn files_come_by_count_then_path() {
    let tree = Tree::new("order");
    tree.file("b.txt", b"go rust\n")
        .file("a.txt", b"rust go\n")
        .file("c.txt", b"rust rust rust\n")
        .file("d.txt", b"python\n");
    index(&tree);
    let run = query(&tree.index_file(), &["rust"]);
    assert_eq!(run.stdout, "c.txt (3)\na.txt (1)\nb.txt (1)");
}

#[test]
fn words_ignore_case_and_punctuation() {
    let tree = Tree::new("words");
    tree.file("a.txt", b"RUST, Rust; rust! (rust)\n");
    index(&tree);
    assert_eq!(query(&tree.index_file(), &["Rust"]).stdout, "a.txt (4)");
}

#[test]
fn a_word_is_whole_not_part_of_another() {
    let tree = Tree::new("whole");
    tree.file("a.txt", b"trust rusty rust_lang\n")
        .file("b.txt", b"v2 v2\n");
    index(&tree);
    let rust = query(&tree.index_file(), &["rust"]);
    assert_eq!(
        rust.stdout, "a.txt (1)",
        "rust_lang holds the word rust, trust does not"
    );
    assert_eq!(query(&tree.index_file(), &["v2"]).stdout, "b.txt (2)");
}

#[test]
fn no_matching_file_exits_with_1() {
    let tree = Tree::new("nomatch");
    tree.file("a.txt", b"alpha\n");
    index(&tree);
    let run = query(&tree.index_file(), &["omega"]);
    assert_eq!((run.stdout.as_str(), run.code), ("", 1));
}

#[test]
fn a_missing_index_is_an_error() {
    let missing = std::env::temp_dir().join("findex3-no-such-index.idx");
    let run = query(&missing, &["x"]);
    assert_eq!(run.code, 2);
    assert!(
        run.stderr.starts_with("error: cannot read "),
        "{:?}",
        run.stderr
    );
}

#[test]
fn search_still_works() {
    let tree = Tree::new("search");
    tree.file("a.txt", b"keep searching\n");
    let run = findex(&[
        "search".as_ref(),
        tree.source().as_os_str(),
        "search".as_ref(),
    ]);
    assert_eq!(run.stdout, "a.txt:1: keep searching");
}
