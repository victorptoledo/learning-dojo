//! Generates `dojo.code-workspace`: one VS Code window with the course
//! (read-only lessons) and the learner's solutions as two separate roots.

use crate::editor::Editor;
use anyhow::{Context, Result};
use serde_json::{Value, json};
use std::fs;
use std::path::{Path, PathBuf};
use std::process::ExitCode;

pub const FILE_NAME: &str = "dojo.code-workspace";

/// Course files a learner should not see while studying: answers, grading tests and manifests.
const HIDDEN_COURSE_FILES: [&str; 6] = [
    "**/kata/reference",
    "**/kata/tests",
    "**/kata/starter",
    "**/quiz.yaml",
    "**/node.yaml",
    "**/track.yaml",
];

/// Extensions VS Code offers to install when the workspace opens.
const RECOMMENDED_EXTENSIONS: [&str; 12] = [
    "rust-lang.rust-analyzer",
    "vscjava.vscode-java-pack",
    "vadimcn.vscode-lldb",
    "tamasfe.even-better-toml",
    "usernamehw.errorlens",
    "ms-azuretools.vscode-docker",
    "redhat.vscode-yaml",
    "timonwong.shellcheck",
    "ms-python.python",
    "charliermarsh.ruff",
    "golang.go",
    "typescriptteam.native-preview",
];

#[derive(Debug, Clone)]
pub struct CodeWorkspace {
    /// The tracks folder of the dojo (`content/tracks`).
    pub lessons_dir: PathBuf,
    /// The learner's workspace (a git repository).
    pub solutions_dir: PathBuf,
    /// Command that starts the dojo, run by the "Dojo" task.
    pub launcher: PathBuf,
}

impl CodeWorkspace {
    pub fn path(&self) -> PathBuf {
        self.solutions_dir.join(FILE_NAME)
    }

    /// Writes the file, listing every kata found so rust-analyzer understands each one.
    pub fn write(&self) -> Result<PathBuf> {
        let path = self.path();
        let text = serde_json::to_string_pretty(&self.to_json())?;
        fs::write(&path, text + "\n")
            .with_context(|| format!("cannot write {}", path.display()))?;
        Ok(path)
    }

    fn to_json(&self) -> Value {
        let hidden: serde_json::Map<String, Value> = HIDDEN_COURSE_FILES
            .iter()
            .chain(["**/target"].iter())
            .map(|pattern| (pattern.to_string(), Value::Bool(true)))
            .collect();
        let cargo_projects: Vec<String> = kata_manifests(&self.solutions_dir)
            .iter()
            .map(|manifest| display(manifest))
            .collect();
        json!({
            "folders": [
                { "name": "🥋 Solutions", "path": display(&self.solutions_dir) },
                { "name": "📖 Lessons", "path": display(&self.lessons_dir) },
            ],
            "settings": {
                "files.exclude": hidden,
                "workbench.editorAssociations": { "*.md": "vscode.markdown.preview.editor" },
                "rust-analyzer.linkedProjects": cargo_projects,
                "files.associations": { "*.bats": "shellscript" },
                "[shellscript]": { "files.eol": "
        " },
            },
            "extensions": { "recommendations": RECOMMENDED_EXTENSIONS },
            "tasks": {
                "version": "2.0.0",
                "tasks": [{
                    "label": "Dojo",
                    "type": "shell",
                    "command": display(&self.launcher),
                    "presentation": { "reveal": "always", "panel": "dedicated", "focus": true },
                    "problemMatcher": [],
                    "runOptions": { "runOn": "folderOpen" },
                }],
            },
        })
    }
}

/// The workspace file for these folders; `launcher` is `dojo.cmd` next to the content.
pub fn for_dojo(content_dir: &Path, solutions_dir: &Path) -> Result<CodeWorkspace> {
    Ok(CodeWorkspace {
        lessons_dir: std::path::absolute(content_dir.join("tracks"))?,
        solutions_dir: std::path::absolute(solutions_dir)?,
        launcher: std::env::current_dir()?.join("dojo.cmd"),
    })
}

/// `dojo vscode`: writes the workspace file and opens it in VS Code.
pub fn open_in_vscode(
    content_dir: &Path,
    solutions_dir: &Path,
    editor_command: &str,
) -> Result<ExitCode> {
    let editor = Editor::new(editor_command);
    if !editor.is_vscode_like() {
        anyhow::bail!(
            "the configured editor `{editor_command}` is not VS Code; set `editor` in ~/.dojo/config.toml"
        );
    }
    fs::create_dir_all(solutions_dir)?;
    let path = for_dojo(content_dir, solutions_dir)?.write()?;
    editor
        .open_file(&path)
        .map_err(|error| anyhow::anyhow!(error))?;
    println!(
        "Opened {} in VS Code. The Dojo task starts in its terminal.",
        path.display()
    );
    Ok(ExitCode::SUCCESS)
}

/// `Cargo.toml` of every kata: `<solutions>/<track>/<phase>/<kata>/Cargo.toml`.
fn kata_manifests(solutions_dir: &Path) -> Vec<PathBuf> {
    let mut manifests: Vec<PathBuf> = subdirs(solutions_dir)
        .flat_map(|track| subdirs(&track).collect::<Vec<_>>())
        .flat_map(|phase| subdirs(&phase).collect::<Vec<_>>())
        .map(|kata| kata.join("Cargo.toml"))
        .filter(|manifest| manifest.is_file())
        .collect();
    manifests.sort();
    manifests
}

fn subdirs(dir: &Path) -> impl Iterator<Item = PathBuf> {
    fs::read_dir(dir)
        .into_iter()
        .flatten()
        .filter_map(|entry| entry.ok().map(|e| e.path()))
        .filter(|path| path.is_dir() && !is_hidden(path))
}

fn is_hidden(path: &Path) -> bool {
    path.file_name()
        .is_some_and(|name| name.to_string_lossy().starts_with('.'))
}

/// Forward slashes: valid on every platform inside VS Code settings.
fn display(path: &Path) -> String {
    path.to_string_lossy().replace('\\', "/")
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn lists_both_roots_hides_answers_and_links_every_kata() {
        let dir = tempfile::tempdir().unwrap();
        let solutions = dir.path().join("ws");
        let kata = solutions.join("rust/p0/hello-dojo");
        fs::create_dir_all(kata.join("src")).unwrap();
        fs::write(kata.join("Cargo.toml"), "[package]").unwrap();
        fs::create_dir_all(solutions.join(".git/objects/aa")).unwrap();
        let lessons = dir.path().join("content/tracks");
        let launcher = dir.path().join("learning-dojo/dojo.cmd");
        let workspace = CodeWorkspace {
            lessons_dir: lessons.clone(),
            solutions_dir: solutions.clone(),
            launcher: launcher.clone(),
        };

        let written = workspace.write().unwrap();
        let json: Value = serde_json::from_str(&fs::read_to_string(&written).unwrap()).unwrap();

        assert_eq!(written, solutions.join(FILE_NAME));
        assert_eq!(
            json["folders"][0]["path"],
            display(&solutions),
            "solutions first: VS Code runs tasks in the first root"
        );
        assert_eq!(json["folders"][1]["path"], display(&lessons));
        assert_eq!(json["settings"]["files.exclude"]["**/kata/reference"], true);
        assert_eq!(json["settings"]["files.exclude"]["**/quiz.yaml"], true);
        assert_eq!(
            json["settings"]["rust-analyzer.linkedProjects"],
            json!([display(&kata.join("Cargo.toml"))])
        );
        assert_eq!(json["tasks"]["tasks"][0]["label"], "Dojo");
        assert!(
            json["extensions"]["recommendations"]
                .as_array()
                .unwrap()
                .contains(&json!("vadimcn.vscode-lldb"))
        );
        assert_eq!(json["tasks"]["tasks"][0]["command"], display(&launcher));
    }
}
