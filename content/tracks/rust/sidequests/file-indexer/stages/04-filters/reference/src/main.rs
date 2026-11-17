use std::collections::{BTreeMap, HashMap};
use std::fmt;
use std::fs;
use std::path::{Path, PathBuf};
use std::process::ExitCode;

const USAGE: &str = "usage: findex search <dir> <text> [--ignore-case] [--ext a,b]\n       findex index <dir> <index-file>\n       findex query <index-file> <word>...";

#[derive(Debug)]
enum FindexError {
    Usage,
    CannotRead { path: PathBuf, reason: String },
    CannotWrite { path: PathBuf, reason: String },
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
            Self::CannotWrite { path, reason } => {
                write!(f, "error: cannot write {}: {reason}", path.display())
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
            let hidden = path
                .file_name()
                .is_some_and(|name| name.to_string_lossy().starts_with('.'));
            if hidden {
                continue;
            }
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

fn parse_options(args: &[String]) -> Result<SearchOptions, FindexError> {
    let mut options = SearchOptions {
        ignore_case: false,
        extensions: None,
    };
    let mut args = args.iter();
    while let Some(arg) = args.next() {
        match arg.as_str() {
            "--ignore-case" => options.ignore_case = true,
            "--ext" => {
                let list = args.next().ok_or(FindexError::Usage)?;
                options.extensions = Some(list.split(',').map(str::to_string).collect());
            }
            _ => return Err(FindexError::Usage),
        }
    }
    Ok(options)
}

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

fn words(text: &str) -> impl Iterator<Item = String> + '_ {
    text.split(|c: char| !c.is_alphanumeric())
        .filter(|word| !word.is_empty())
        .map(str::to_lowercase)
}

/// The index file: one `path<TAB>word<TAB>count` line per word of every file.
fn index(dir: &Path, index_file: &Path) -> Result<(), FindexError> {
    let files = text_files(dir)?;
    let mut out = String::new();
    for file in &files {
        let mut counts: BTreeMap<String, usize> = BTreeMap::new();
        for word in words(&file.text) {
            *counts.entry(word).or_default() += 1;
        }
        for (word, count) in counts {
            out.push_str(&format!("{}\t{word}\t{count}\n", file.relative));
        }
    }
    fs::write(index_file, out).map_err(|error| FindexError::CannotWrite {
        path: index_file.to_path_buf(),
        reason: error.to_string(),
    })?;
    println!("indexed {} files", files.len());
    Ok(())
}

fn query(index_file: &Path, query_words: &[String]) -> Result<bool, FindexError> {
    let text = fs::read_to_string(index_file).map_err(|error| FindexError::CannotRead {
        path: index_file.to_path_buf(),
        reason: error.to_string(),
    })?;
    let wanted: Vec<String> = query_words.iter().map(|word| word.to_lowercase()).collect();
    // For each file, the count of each wanted word it holds.
    let mut hits: HashMap<&str, HashMap<&str, usize>> = HashMap::new();
    for line in text.lines() {
        let mut fields = line.split('\t');
        let (Some(path), Some(word), Some(count)) = (fields.next(), fields.next(), fields.next())
        else {
            continue;
        };
        if wanted.iter().any(|w| w == word) {
            let count = count.parse().unwrap_or(0);
            hits.entry(path).or_default().insert(word, count);
        }
    }
    let mut results: Vec<(&str, usize)> = hits
        .into_iter()
        .filter(|(_, found)| wanted.iter().all(|word| found.contains_key(word.as_str())))
        .map(|(path, found)| (path, found.values().sum()))
        .collect();
    results.sort_by(|a, b| b.1.cmp(&a.1).then(a.0.cmp(b.0)));
    for (path, count) in &results {
        println!("{path} ({count})");
    }
    Ok(!results.is_empty())
}

fn run(args: &[String]) -> Result<bool, FindexError> {
    match args {
        [command, dir, text, options @ ..] if command == "search" => {
            search(Path::new(dir), text, &parse_options(options)?)
        }
        [command, dir, index_file] if command == "index" => {
            index(Path::new(dir), Path::new(index_file)).map(|()| true)
        }
        [command, index_file, words @ ..] if command == "query" && !words.is_empty() => {
            query(Path::new(index_file), words)
        }
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
