//! What a learner's machine needs before starting a node: tools, editor
//! extensions, a running Docker engine, runner images.

#[derive(Debug, Clone, PartialEq, Eq)]
pub enum RequirementKind {
    /// A command-line tool, found by running it with `version_args`.
    Tool {
        command: String,
        version_args: Vec<String>,
        min_version: Option<String>,
        /// Shown when the tool is missing: tools are installed by the learner.
        install_hint: String,
        /// Command that updates the tool (e.g. `rustup update stable`).
        update: Option<Vec<String>>,
    },
    /// A VS Code extension, installed and updated through the `code` command.
    VsCodeExtension { id: String },
    /// The Docker engine must be running.
    DockerEngine { install_hint: String },
    /// A runner image built from `runner-images/<name>`.
    DockerImage { image: String },
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct Requirement {
    /// Human name, e.g. "rust-analyzer".
    pub name: String,
    /// Why this step needs it, in one sentence.
    pub why: String,
    /// Required items block the node until satisfied; optional ones only advise.
    pub required: bool,
    pub kind: RequirementKind,
}

impl Requirement {
    /// Stable identity used to merge track-wide and node requirements.
    pub fn key(&self) -> String {
        match &self.kind {
            RequirementKind::Tool { command, .. } => format!("tool:{command}"),
            RequirementKind::VsCodeExtension { id } => format!("vscode:{id}"),
            RequirementKind::DockerEngine { .. } => "docker-engine".to_string(),
            RequirementKind::DockerImage { image } => format!("image:{image}"),
        }
    }
}

/// Track-wide requirements followed by the node's own; a node entry with the same
/// key replaces the track's (e.g. to make an optional tool required for one kata).
pub fn merge<'a>(track: &'a [Requirement], node: &'a [Requirement]) -> Vec<&'a Requirement> {
    let node_keys: Vec<String> = node.iter().map(Requirement::key).collect();
    track
        .iter()
        .filter(|requirement| !node_keys.contains(&requirement.key()))
        .chain(node.iter())
        .collect()
}

/// Compares dotted versions numerically: `1.100.0` is newer than `1.85.0`.
pub fn version_at_least(found: &str, minimum: &str) -> bool {
    let parts = |text: &str| -> Vec<u64> {
        text.split('.')
            .map(|part| part.parse().unwrap_or(0))
            .collect()
    };
    let (found, minimum) = (parts(found), parts(minimum));
    let length = found.len().max(minimum.len());
    let at = |list: &[u64], i: usize| list.get(i).copied().unwrap_or(0);
    for i in 0..length {
        match at(&found, i).cmp(&at(&minimum, i)) {
            std::cmp::Ordering::Greater => return true,
            std::cmp::Ordering::Less => return false,
            std::cmp::Ordering::Equal => {}
        }
    }
    true
}

#[cfg(test)]
mod tests {
    use super::*;

    fn extension(id: &str, required: bool) -> Requirement {
        Requirement {
            name: id.into(),
            why: String::new(),
            required,
            kind: RequirementKind::VsCodeExtension { id: id.into() },
        }
    }

    #[test]
    fn node_requirements_add_to_and_override_the_track() {
        let track = [
            extension("rust-analyzer", false),
            extension("errorlens", false),
        ];
        let node = [
            extension("rust-analyzer", true),
            extension("codelldb", false),
        ];

        let merged: Vec<(String, bool)> = merge(&track, &node)
            .into_iter()
            .map(|r| (r.name.clone(), r.required))
            .collect();

        assert_eq!(
            merged,
            vec![
                ("errorlens".to_string(), false),
                ("rust-analyzer".to_string(), true),
                ("codelldb".to_string(), false),
            ]
        );
    }

    #[test]
    fn versions_compare_numerically() {
        assert!(version_at_least("1.100.0", "1.85.0"));
        assert!(version_at_least("1.85", "1.85.0"));
        assert!(!version_at_least("1.84.1", "1.85.0"));
        assert!(version_at_least("2.0", "1.99.99"));
    }
}
