//! Checks, installs and updates what a node needs on this machine.

use crate::editor::Editor;
use dojo_core::{Requirement, RequirementKind, version_at_least};
use regex::Regex;
use std::path::{Path, PathBuf};
use std::process::{Command, Stdio};
use std::sync::LazyLock;

static VERSION: LazyLock<Regex> =
    LazyLock::new(|| Regex::new(r"\d+\.\d+(\.\d+)?").expect("valid regex"));

const DOCKER_DESKTOP: &str = r"C:\Program Files\Docker\Docker\Docker Desktop.exe";

#[derive(Debug, Clone, PartialEq, Eq)]
pub enum Status {
    Ready {
        detail: String,
    },
    Missing {
        hint: String,
    },
    Outdated {
        found: String,
        needed: String,
    },
    /// The requirement does not apply here (e.g. a VS Code extension with another editor).
    NotApplicable {
        reason: String,
    },
}

impl Status {
    /// Good enough to start the node.
    pub fn is_satisfied(&self) -> bool {
        matches!(self, Status::Ready { .. } | Status::NotApplicable { .. })
    }
}

/// The machine as the dojo sees it: which editor and where the runner images live.
#[derive(Debug, Clone)]
pub struct Environment {
    editor: Editor,
    images_dir: PathBuf,
}

impl Environment {
    pub fn new(editor: Editor, images_dir: &Path) -> Self {
        Self {
            editor,
            images_dir: images_dir.to_path_buf(),
        }
    }

    pub fn check(&self, requirement: &Requirement) -> Status {
        match &requirement.kind {
            RequirementKind::Tool {
                command,
                version_args,
                min_version,
                install_hint,
                ..
            } => check_tool(command, version_args, min_version.as_deref(), install_hint),
            RequirementKind::VsCodeExtension { id } => self.check_extension(id),
            RequirementKind::DockerEngine { install_hint } => check_docker_engine(install_hint),
            RequirementKind::DockerImage { image } => check_image(image),
        }
    }

    /// Whether `install` can do the job itself, or only point the learner to it.
    pub fn can_install(&self, requirement: &Requirement) -> bool {
        match &requirement.kind {
            RequirementKind::VsCodeExtension { .. } => self.editor.is_vscode_like(),
            RequirementKind::DockerImage { .. } => true,
            RequirementKind::DockerEngine { .. } => Path::new(DOCKER_DESKTOP).is_file(),
            RequirementKind::Tool { .. } => false,
        }
    }

    pub fn install(&self, requirement: &Requirement) -> Result<String, String> {
        match &requirement.kind {
            RequirementKind::VsCodeExtension { id } => {
                run(self.editor.command(), &["--install-extension", id])
                    .map(|_| format!("installed {id}"))
            }
            RequirementKind::DockerImage { image } => self.build_image(image, false),
            RequirementKind::DockerEngine { install_hint } => start_docker_desktop(install_hint),
            RequirementKind::Tool { install_hint, .. } => Err(install_hint.clone()),
        }
    }

    pub fn update(&self, requirement: &Requirement) -> Result<String, String> {
        match &requirement.kind {
            RequirementKind::Tool {
                update: Some(command),
                ..
            } => {
                let (program, args) = command.split_first().ok_or("empty update command")?;
                let args: Vec<&str> = args.iter().map(String::as_str).collect();
                run(program, &args).map(|_| format!("updated with `{}`", command.join(" ")))
            }
            RequirementKind::VsCodeExtension { id } if self.editor.is_vscode_like() => run(
                self.editor.command(),
                &["--install-extension", id, "--force"],
            )
            .map(|_| format!("updated {id}")),
            RequirementKind::DockerImage { image } => self.build_image(image, true),
            _ => Ok(format!("{}: nothing to update", requirement.name)),
        }
    }

    fn check_extension(&self, id: &str) -> Status {
        if !self.editor.is_vscode_like() {
            return Status::NotApplicable {
                reason: "your editor is not VS Code".to_string(),
            };
        }
        match run(
            self.editor.command(),
            &["--list-extensions", "--show-versions"],
        ) {
            Ok(listing) => extension_status(&listing, id),
            Err(error) => Status::Missing { hint: error },
        }
    }

    /// `dojo-rust` is built from `runner-images/rust`; `pull` refreshes the base image.
    fn build_image(&self, image: &str, pull: bool) -> Result<String, String> {
        let folder = self.images_dir.join(image.trim_start_matches("dojo-"));
        let folder = folder.to_string_lossy().into_owned();
        let mut args = vec!["build", "--tag", image];
        if pull {
            args.push("--pull");
        }
        args.push(&folder);
        run("docker", &args).map(|_| format!("built {image}"))
    }
}

fn check_tool(command: &str, version_args: &[String], minimum: Option<&str>, hint: &str) -> Status {
    let args: Vec<&str> = if version_args.is_empty() {
        vec!["--version"]
    } else {
        version_args.iter().map(String::as_str).collect()
    };
    let Ok(output) = run(command, &args) else {
        return Status::Missing {
            hint: hint.to_string(),
        };
    };
    let found = VERSION
        .find(&output)
        .map(|m| m.as_str().to_string())
        .unwrap_or_default();
    match minimum {
        Some(needed) if !found.is_empty() && !version_at_least(&found, needed) => {
            Status::Outdated {
                found,
                needed: needed.to_string(),
            }
        }
        _ => Status::Ready {
            detail: if found.is_empty() {
                "found".to_string()
            } else {
                found
            },
        },
    }
}

fn extension_status(listing: &str, id: &str) -> Status {
    let wanted = id.to_lowercase();
    listing
        .lines()
        .find_map(|line| {
            let (name, version) = line.trim().split_once('@').unwrap_or((line.trim(), ""));
            (name.to_lowercase() == wanted).then(|| Status::Ready {
                detail: version.to_string(),
            })
        })
        .unwrap_or(Status::Missing {
            hint: format!("press [I] to install {id}"),
        })
}

fn check_docker_engine(hint: &str) -> Status {
    match run("docker", &["info", "--format", "{{.ServerVersion}}"]) {
        Ok(version) if !version.trim().is_empty() => Status::Ready {
            detail: version.trim().to_string(),
        },
        _ => Status::Missing {
            hint: hint.to_string(),
        },
    }
}

fn check_image(image: &str) -> Status {
    match run(
        "docker",
        &["image", "inspect", "--format", "{{.Created}}", image],
    ) {
        Ok(created) => Status::Ready {
            detail: format!("built {}", created.trim().get(..10).unwrap_or("")),
        },
        Err(_) => Status::Missing {
            hint: "press [I] to build it (Docker must be running)".to_string(),
        },
    }
}

fn start_docker_desktop(hint: &str) -> Result<String, String> {
    Command::new(DOCKER_DESKTOP)
        .stdin(Stdio::null())
        .stdout(Stdio::null())
        .stderr(Stdio::null())
        .spawn()
        .map(|_| "starting Docker Desktop; press [R] in a minute to check again".to_string())
        .map_err(|_| hint.to_string())
}

/// Runs a command to completion, returning its standard output.
fn run(program: &str, args: &[&str]) -> Result<String, String> {
    let output = Command::new(program)
        .args(args)
        .stdin(Stdio::null())
        .output()
        .map_err(|_| format!("`{program}` was not found"))?;
    if output.status.success() {
        Ok(String::from_utf8_lossy(&output.stdout).into_owned())
    } else {
        let error = String::from_utf8_lossy(&output.stderr);
        Err(error.lines().last().unwrap_or("failed").trim().to_string())
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    fn tool(command: &str, minimum: Option<&str>) -> Requirement {
        Requirement {
            name: command.into(),
            why: String::new(),
            required: true,
            kind: RequirementKind::Tool {
                command: command.into(),
                version_args: vec![],
                min_version: minimum.map(str::to_string),
                install_hint: "install it".into(),
                update: None,
            },
        }
    }

    fn environment(editor: &str) -> Environment {
        Environment::new(Editor::new(editor), Path::new("runner-images"))
    }

    #[test]
    fn cargo_is_found_with_its_version_and_a_missing_tool_shows_the_hint() {
        let env = environment("explorer");

        assert!(matches!(
            env.check(&tool("cargo", Some("1.0"))),
            Status::Ready { .. }
        ));
        assert_eq!(
            env.check(&tool("definitely-not-a-tool-xyz", None)),
            Status::Missing {
                hint: "install it".into()
            }
        );
        assert!(matches!(
            env.check(&tool("cargo", Some("999.0"))),
            Status::Outdated { .. }
        ));
    }

    #[test]
    fn extensions_are_matched_by_id_ignoring_case() {
        let listing = "rust-lang.rust-analyzer@0.3.3113\nVadimcn.Vscode-Lldb@1.12.3\n";

        assert_eq!(
            extension_status(listing, "vadimcn.vscode-lldb"),
            Status::Ready {
                detail: "1.12.3".into()
            }
        );
        assert!(matches!(
            extension_status(listing, "redhat.vscode-yaml"),
            Status::Missing { .. }
        ));
    }

    #[test]
    fn extensions_do_not_apply_to_other_editors_and_tools_cannot_self_install() {
        let env = environment("explorer");
        let extension = Requirement {
            name: "rust-analyzer".into(),
            why: String::new(),
            required: false,
            kind: RequirementKind::VsCodeExtension {
                id: "rust-lang.rust-analyzer".into(),
            },
        };

        assert!(matches!(
            env.check(&extension),
            Status::NotApplicable { .. }
        ));
        assert!(!env.can_install(&tool("cargo", None)));
        assert_eq!(
            env.install(&tool("cargo", None)),
            Err("install it".to_string())
        );
    }
}
