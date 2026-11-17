//! Stage 1: `findex search <dir> <text>`, every file assumed to be UTF-8 text.

use std::fs;
use std::path::{Path, PathBuf};
use std::process::ExitCode;

/// Every file under `dir`, recursively.
fn files_under(dir: &Path) -> Vec<PathBuf> {
    let mut files = Vec::new();
    for entry in fs::read_dir(dir).expect("readable folder") {
        let path = entry.expect("readable entry").path();
        if path.is_dir() {
            files.extend(files_under(&path));
        } else {
            files.push(path);
        }
    }
    files
}

fn relative(root: &Path, path: &Path) -> String {
    path.strip_prefix(root)
        .unwrap_or(path)
        .components()
        .map(|part| part.as_os_str().to_string_lossy().into_owned())
        .collect::<Vec<_>>()
        .join("/")
}

fn main() -> ExitCode {
    let args: Vec<String> = std::env::args().collect();
    let (root, text) = (Path::new(&args[2]), &args[3]);
    let mut files: Vec<(String, PathBuf)> = files_under(root)
        .into_iter()
        .map(|path| (relative(root, &path), path))
        .collect();
    files.sort();
    let mut found = false;
    for (name, path) in files {
        let content = fs::read_to_string(&path).expect("UTF-8 text");
        for (number, line) in content.lines().enumerate() {
            if line.contains(text.as_str()) {
                println!("{name}:{}: {line}", number + 1);
                found = true;
            }
        }
    }
    if found {
        ExitCode::SUCCESS
    } else {
        ExitCode::from(1)
    }
}
