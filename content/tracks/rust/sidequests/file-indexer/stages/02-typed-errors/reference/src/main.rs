use std::fmt;
use std::fs;
use std::path::{Path, PathBuf};
use std::process::ExitCode;

const USAGE: &str = "usage: findex search <dir> <text>";

#[derive(Debug)]
enum FindexError {
    Usage,
    CannotRead { path: PathBuf, reason: String },
}

impl FindexError {
    fn exit_code(&self) -> u8 {
        2
    }
}

impl fmt::Display for FindexError {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        match self {
            Self::Usage => write!(f, "{USAGE}"),
            Self::CannotRead { path, reason } => {
                write!(f, "error: cannot read {}: {reason}", path.display())
            }
        }
    }
}

/// A text file under the searched folder, with its path relative to it.
struct TextFile {
    relative: String,
    text: String,
}

/// Every visible UTF-8 file under `root`, sorted by relative path. Files that are not
/// UTF-8 are reported on stderr and skipped.
fn text_files(root: &Path) -> Result<Vec<TextFile>, FindexError> {
    let cannot_read = |path: &Path, error: std::io::Error| FindexError::CannotRead {
        path: path.to_path_buf(),
        reason: error.to_string(),
    };
    if !root.is_dir() {
        return Err(FindexError::CannotRead {
            path: root.to_path_buf(),
            reason: "not a folder".to_string(),
        });
    }
    let mut paths = Vec::new();
    let mut pending = vec![root.to_path_buf()];
    while let Some(dir) = pending.pop() {
        for entry in fs::read_dir(&dir).map_err(|error| cannot_read(&dir, error))? {
            let path = entry.map_err(|error| cannot_read(&dir, error))?.path();
            if path.is_dir() {
                pending.push(path);
            } else {
                paths.push(path);
            }
        }
    }
    let mut files: Vec<TextFile> = paths
        .into_iter()
        .filter_map(|path| {
            let relative = relative_path(root, &path);
            match fs::read(&path).map(String::from_utf8) {
                Ok(Ok(text)) => Some(TextFile { relative, text }),
                Ok(Err(_)) => {
                    eprintln!("warning: skipping {relative}: not UTF-8");
                    None
                }
                Err(error) => {
                    eprintln!("warning: skipping {relative}: {error}");
                    None
                }
            }
        })
        .collect();
    files.sort_by(|a, b| a.relative.cmp(&b.relative));
    Ok(files)
}

fn relative_path(root: &Path, path: &Path) -> String {
    path.strip_prefix(root)
        .unwrap_or(path)
        .components()
        .map(|part| part.as_os_str().to_string_lossy().into_owned())
        .collect::<Vec<_>>()
        .join("/")
}

struct SearchOptions {
    ignore_case: bool,
    extensions: Option<Vec<String>>,
}

const NO_OPTIONS: SearchOptions = SearchOptions {
    ignore_case: false,
    extensions: None,
};

fn search(dir: &Path, text: &str, options: &SearchOptions) -> Result<bool, FindexError> {
    let wanted = |file: &TextFile| {
        options.extensions.as_ref().is_none_or(|extensions| {
            Path::new(&file.relative)
                .extension()
                .is_some_and(|ext| extensions.iter().any(|wanted| ext == wanted.as_str()))
        })
    };
    let needle = if options.ignore_case {
        text.to_lowercase()
    } else {
        text.to_string()
    };
    let mut found = false;
    for file in text_files(dir)?.iter().filter(|file| wanted(file)) {
        for (number, line) in file.text.lines().enumerate() {
            let haystack = if options.ignore_case {
                line.to_lowercase()
            } else {
                line.to_string()
            };
            if haystack.contains(&needle) {
                println!("{}:{}: {line}", file.relative, number + 1);
                found = true;
            }
        }
    }
    Ok(found)
}

fn run(args: &[String]) -> Result<bool, FindexError> {
    match args {
        [command, dir, text] if command == "search" => search(Path::new(dir), text, &NO_OPTIONS),
        _ => Err(FindexError::Usage),
    }
}

fn main() -> ExitCode {
    let args: Vec<String> = std::env::args().skip(1).collect();
    match run(&args) {
        Ok(true) => ExitCode::SUCCESS,
        Ok(false) => ExitCode::from(1),
        Err(error) => {
            eprintln!("{error}");
            ExitCode::from(error.exit_code())
        }
    }
}
