use anyhow::{Context, Result};
use serde::Deserialize;
use std::fs;
use std::path::{Path, PathBuf};

/// `~/.dojo/config.toml`. Every key is optional; command-line flags win over it.
#[derive(Debug, Default, Deserialize)]
#[serde(deny_unknown_fields)]
struct ConfigFile {
    /// Folder where kata solutions live (a git repository of its own).
    workspace: Option<PathBuf>,
    /// Command that opens a folder, e.g. `idea64` or `code`.
    editor: Option<String>,
}

#[derive(Debug, Clone)]
pub struct Config {
    pub workspace: PathBuf,
    pub editor: String,
}

impl Config {
    pub fn load(path: &Path) -> Result<Self> {
        let file = if path.is_file() {
            let text = fs::read_to_string(path)
                .with_context(|| format!("cannot read {}", path.display()))?;
            toml::from_str(&text)
                .with_context(|| format!("invalid config in {}", path.display()))?
        } else {
            ConfigFile::default()
        };
        Ok(Self {
            workspace: file.workspace.unwrap_or_else(default_workspace),
            editor: file.editor.unwrap_or_else(default_editor),
        })
    }
}

pub fn dojo_home() -> PathBuf {
    std::env::home_dir().unwrap_or_default().join(".dojo")
}

fn default_workspace() -> PathBuf {
    if cfg!(windows) {
        PathBuf::from(r"C:\dev\dojo-workspace")
    } else {
        std::env::home_dir()
            .unwrap_or_default()
            .join("dojo-workspace")
    }
}

fn default_editor() -> String {
    if cfg!(windows) {
        "explorer"
    } else {
        "xdg-open"
    }
    .to_string()
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn missing_file_gives_defaults_and_keys_override_them() {
        let dir = tempfile::tempdir().unwrap();
        let path = dir.path().join("config.toml");
        assert_eq!(Config::load(&path).unwrap().editor, default_editor());

        fs::write(&path, "workspace = 'D:/katas'\neditor = 'idea64'\n").unwrap();
        let config = Config::load(&path).unwrap();

        assert_eq!(config.workspace, PathBuf::from("D:/katas"));
        assert_eq!(config.editor, "idea64");
    }

    #[test]
    fn unknown_keys_are_rejected() {
        let dir = tempfile::tempdir().unwrap();
        let path = dir.path().join("config.toml");
        fs::write(&path, "workspce = 'typo'\n").unwrap();

        assert!(Config::load(&path).is_err());
    }
}
