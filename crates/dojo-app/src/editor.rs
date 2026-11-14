//! Opens kata folders and lesson documents in the learner's editor.
//!
//! VS Code-like editors (VS Code, Antigravity, Cursor) and IntelliJ open a folder
//! as a project and can show files in it; anything else (Explorer, xdg-open) only
//! gets the path.

use std::ffi::OsString;
use std::path::{Path, PathBuf};
use std::process::{Command, Stdio};

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
enum Family {
    /// `editor <folder> <file>...` opens the folder and the files in one window.
    VsCodeLike,
    /// `idea64 <folder>` opens a project; files are opened one call at a time.
    IntelliJ,
    /// A file manager or generic opener: one path.
    Other,
}

/// Where the dojo itself runs. Inside an editor's terminal, files open in that
/// editor's window instead of a new project window.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum Host {
    Standalone,
    /// IntelliJ's terminal sets `TERMINAL_EMULATOR=JetBrains-JediTerm`.
    JetBrainsTerminal,
    /// VS Code and its forks set `TERM_PROGRAM=vscode`.
    VsCodeTerminal,
}

impl Host {
    pub fn detect() -> Self {
        let var = |name| std::env::var(name).unwrap_or_default();
        if var("TERMINAL_EMULATOR").starts_with("JetBrains") {
            Self::JetBrainsTerminal
        } else if var("TERM_PROGRAM") == "vscode" {
            Self::VsCodeTerminal
        } else {
            Self::Standalone
        }
    }
}

#[derive(Debug, Clone)]
pub struct Editor {
    command: String,
    family: Family,
    host: Host,
    /// A VS Code workspace file to open instead of a bare folder.
    project: Option<PathBuf>,
}

impl Editor {
    pub fn new(command: &str) -> Self {
        Self::with_host(command, Host::detect())
    }

    pub fn with_host(command: &str, host: Host) -> Self {
        Self {
            command: command.to_string(),
            family: family_of(command),
            host,
            project: None,
        }
    }

    /// Opens files inside this VS Code workspace file when no editor window is open yet.
    pub fn with_project(mut self, project: PathBuf) -> Self {
        self.project = Some(project);
        self
    }

    pub fn is_vscode_like(&self) -> bool {
        self.family == Family::VsCodeLike
    }

    pub fn command(&self) -> &str {
        &self.command
    }

    /// Opens a kata folder, with the files to look at first when the editor supports it.
    pub fn open_folder(&self, folder: &Path, files: &[PathBuf]) -> Result<(), String> {
        let target = self.project.as_deref().unwrap_or(folder);
        invocations(self.family, self.host, target, files)
            .into_iter()
            .try_for_each(|args| self.spawn(args))
    }

    pub fn open_file(&self, file: &Path) -> Result<(), String> {
        let calls = match &self.project {
            Some(project) => invocations(self.family, self.host, project, &[file.to_path_buf()]),
            None => invocations(self.family, self.host, file, &[]),
        };
        calls.into_iter().try_for_each(|args| self.spawn(args))
    }

    fn spawn(&self, args: Vec<OsString>) -> Result<(), String> {
        // Detached from our terminal: the editor must not draw over the TUI or keep
        // the caller's output pipe open after the dojo exits.
        Command::new(&self.command)
            .args(args)
            .stdin(Stdio::null())
            .stdout(Stdio::null())
            .stderr(Stdio::null())
            .spawn()
            .map(|_| ())
            .map_err(|error| format!("could not run `{}`: {error}", self.command))
    }
}

/// The editor calls needed to show `target` (a folder or a file) and `files`.
///
/// From inside an editor's own terminal the project is already open, so only the
/// files are sent, to the same window. Otherwise the folder opens as a project first.
fn invocations(family: Family, host: Host, target: &Path, files: &[PathBuf]) -> Vec<Vec<OsString>> {
    let path = |p: &Path| p.as_os_str().to_os_string();
    let to_show: Vec<OsString> = if files.is_empty() {
        vec![path(target)]
    } else {
        files.iter().map(|file| path(file)).collect()
    };
    match (family, host) {
        (Family::VsCodeLike, Host::VsCodeTerminal) => {
            vec![
                std::iter::once(OsString::from("--reuse-window"))
                    .chain(to_show)
                    .collect(),
            ]
        }
        (Family::VsCodeLike, _) => {
            let mut args = vec![path(target)];
            args.extend(files.iter().map(|file| path(file)));
            vec![args]
        }
        (Family::IntelliJ, Host::JetBrainsTerminal) => {
            to_show.into_iter().map(|arg| vec![arg]).collect()
        }
        (Family::IntelliJ, _) => std::iter::once(vec![path(target)])
            .chain(files.iter().map(|file| vec![path(file)]))
            .collect(),
        (Family::Other, _) => vec![vec![path(target)]],
    }
}

fn family_of(command: &str) -> Family {
    let name = Path::new(command)
        .file_stem()
        .map(|stem| stem.to_string_lossy().to_lowercase())
        .unwrap_or_default();
    if [
        "code",
        "code-insiders",
        "codium",
        "antigravity",
        "cursor",
        "windsurf",
    ]
    .contains(&name.as_str())
    {
        Family::VsCodeLike
    } else if name.starts_with("idea") {
        Family::IntelliJ
    } else {
        Family::Other
    }
}

/// First editor found on this machine: VS Code, then IntelliJ, else the system file browser.
pub fn detect_default() -> String {
    find_on_path(if cfg!(windows) { "code.cmd" } else { "code" })
        .or_else(|| known_install("Microsoft VS Code/bin/code.cmd"))
        .or_else(|| find_on_path(if cfg!(windows) { "idea64.exe" } else { "idea" }))
        .or_else(|| known_install("IntelliJ IDEA/bin/idea64.exe"))
        .map(|path| path.to_string_lossy().into_owned())
        .unwrap_or_else(|| {
            if cfg!(windows) {
                "explorer"
            } else {
                "xdg-open"
            }
            .to_string()
        })
}

fn find_on_path(executable: &str) -> Option<PathBuf> {
    let paths = std::env::var_os("PATH")?;
    std::env::split_paths(&paths)
        .map(|dir| dir.join(executable))
        .find(|candidate| candidate.is_file())
}

/// Per-user installs on Windows live under `%LOCALAPPDATA%\Programs`.
fn known_install(relative: &str) -> Option<PathBuf> {
    let base = std::env::var_os("LOCALAPPDATA")?;
    let candidate = PathBuf::from(base).join("Programs").join(relative);
    candidate.is_file().then_some(candidate)
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn editors_are_recognised_by_their_command_name() {
        assert_eq!(family_of("code.cmd"), Family::VsCodeLike);
        assert_eq!(
            family_of(r"C:\Users\me\AppData\Local\Programs\antigravity\Antigravity.exe"),
            Family::VsCodeLike
        );
        assert_eq!(family_of("/usr/bin/cursor"), Family::VsCodeLike);
        assert_eq!(
            family_of(r"C:\Program Files\IntelliJ IDEA\bin\idea64.exe"),
            Family::IntelliJ
        );
        assert_eq!(family_of("explorer"), Family::Other);
    }

    fn os(args: &[&str]) -> Vec<OsString> {
        args.iter().map(OsString::from).collect()
    }

    #[test]
    fn intellij_terminal_opens_only_the_files_in_the_current_window() {
        let files = [
            PathBuf::from("kata/README.md"),
            PathBuf::from("kata/src/lib.rs"),
        ];

        let inside = invocations(
            Family::IntelliJ,
            Host::JetBrainsTerminal,
            Path::new("kata"),
            &files,
        );
        let outside = invocations(
            Family::IntelliJ,
            Host::Standalone,
            Path::new("kata"),
            &files,
        );

        assert_eq!(
            inside,
            vec![os(&["kata/README.md"]), os(&["kata/src/lib.rs"])]
        );
        assert_eq!(
            outside,
            vec![
                os(&["kata"]),
                os(&["kata/README.md"]),
                os(&["kata/src/lib.rs"])
            ]
        );
    }

    #[test]
    fn vscode_terminal_reuses_the_window() {
        let files = [PathBuf::from("kata/src/lib.rs")];

        assert_eq!(
            invocations(
                Family::VsCodeLike,
                Host::VsCodeTerminal,
                Path::new("kata"),
                &files
            ),
            vec![os(&["--reuse-window", "kata/src/lib.rs"])]
        );
        assert_eq!(
            invocations(
                Family::VsCodeLike,
                Host::Standalone,
                Path::new("kata"),
                &files
            ),
            vec![os(&["kata", "kata/src/lib.rs"])]
        );
    }

    #[test]
    fn outside_vscode_files_open_inside_the_workspace_file() {
        let editor = Editor::with_host("code.cmd", Host::Standalone)
            .with_project(PathBuf::from("dojo.code-workspace"));
        let target = editor.project.as_deref().unwrap();

        assert_eq!(
            invocations(
                editor.family,
                editor.host,
                target,
                &[PathBuf::from("lessons/setup.md")]
            ),
            vec![os(&["dojo.code-workspace", "lessons/setup.md"])]
        );
    }

    #[test]
    fn a_single_file_is_opened_as_is() {
        assert_eq!(
            invocations(
                Family::IntelliJ,
                Host::JetBrainsTerminal,
                Path::new("lesson.md"),
                &[]
            ),
            vec![os(&["lesson.md"])]
        );
        assert_eq!(
            invocations(Family::Other, Host::Standalone, Path::new("lesson.md"), &[]),
            vec![os(&["lesson.md"])]
        );
    }

    #[test]
    fn a_missing_editor_is_reported_not_panicking() {
        let editor = Editor::new("definitely-not-an-editor-xyz");
        let error = editor.open_file(Path::new("lesson.md")).unwrap_err();
        assert!(error.contains("definitely-not-an-editor-xyz"));
    }
}
