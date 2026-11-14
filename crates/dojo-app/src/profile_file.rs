use anyhow::{Context, Result};
use dojo_core::Profile;
use std::fs;
use std::path::{Path, PathBuf};

const DEFAULT_LEARNER_NAME: &str = "Dev Sensei";

/// JSON file holding the learner profile. Replaced by the SQLite store in M1.
pub struct ProfileFile {
    path: PathBuf,
}

impl ProfileFile {
    pub fn new(path: &Path) -> Self {
        Self {
            path: path.to_path_buf(),
        }
    }

    /// Reads the saved profile, or starts a fresh one when there is none yet.
    pub fn load_or_new(&self, default_track_id: &str) -> Result<Profile> {
        if !self.path.exists() {
            return Ok(Profile::new(DEFAULT_LEARNER_NAME, default_track_id));
        }
        let text = fs::read_to_string(&self.path)
            .with_context(|| format!("cannot read {}", self.path.display()))?;
        serde_json::from_str(&text)
            .with_context(|| format!("invalid profile in {}", self.path.display()))
    }

    pub fn save(&self, profile: &Profile) -> Result<()> {
        if let Some(parent) = self.path.parent() {
            fs::create_dir_all(parent)?;
        }
        fs::write(&self.path, serde_json::to_string_pretty(profile)?)
            .with_context(|| format!("cannot write {}", self.path.display()))
    }
}
