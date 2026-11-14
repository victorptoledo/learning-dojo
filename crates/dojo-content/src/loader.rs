use crate::front_matter;
use crate::manifest::{
    BeltManifest, BridgeFrontMatter, NodeManifest, ObjectiveManifest, PhaseManifest,
    SkillsManifest, TrackManifest,
};
use crate::report::Report;
use crate::schema::{self, Schemas};
use anyhow::{Result, bail};
use dojo_core::{Belt, Bridge, Catalog, Node, Objective, Phase, Skill, SkillId, Track};
use regex::Regex;
use serde::de::DeserializeOwned;
use serde_json::Value;
use std::collections::BTreeMap;
use std::fs;
use std::path::{Path, PathBuf};
use std::sync::LazyLock;

static NODE_FOLDER: LazyLock<Regex> =
    LazyLock::new(|| Regex::new(r"^\d{2}-([a-z0-9]+(?:-[a-z0-9]+)*)$").expect("valid regex"));

#[derive(Clone, Copy)]
enum FileKind {
    Track,
    Node,
    Skills,
    Bridge,
}

pub fn load_catalog(root: &Path, report: &mut Report) -> Result<Catalog> {
    if !root.is_dir() {
        bail!("content folder not found: {}", root.display());
    }
    let schemas = Schemas::load(&root.join("schemas"))?;
    let mut reader = ContentReader {
        root,
        schemas,
        report,
    };
    let skills = reader.load_skills();
    let tracks = reader.load_tracks();
    Ok(Catalog { tracks, skills })
}

struct ContentReader<'a> {
    root: &'a Path,
    schemas: Schemas,
    report: &'a mut Report,
}

impl ContentReader<'_> {
    // ---- skills -------------------------------------------------------------

    fn load_skills(&mut self) -> BTreeMap<SkillId, Skill> {
        let mut skills = BTreeMap::new();
        for path in files_with_extension(&self.root.join("skills"), "yaml") {
            let Some(manifest) = self.read_manifest::<SkillsManifest>(&path, FileKind::Skills)
            else {
                continue;
            };
            self.collect_skills(&path, manifest, &mut skills);
        }
        skills
    }

    fn collect_skills(
        &mut self,
        path: &Path,
        manifest: SkillsManifest,
        skills: &mut BTreeMap<SkillId, Skill>,
    ) {
        let location = self.relative(path);
        let prefix = format!("{}.", manifest.namespace);
        for entry in manifest.skills {
            if !entry.id.starts_with(&prefix) {
                self.report.error(
                    &location,
                    format!(
                        "skill `{}` is outside namespace `{}`",
                        entry.id, manifest.namespace
                    ),
                );
            }
            if skills.contains_key(&entry.id) {
                self.report
                    .error(&location, format!("duplicate skill `{}`", entry.id));
                continue;
            }
            let skill = Skill {
                id: entry.id.clone(),
                title: entry.title,
                maps_to: entry.maps_to,
            };
            skills.insert(entry.id, skill);
        }
    }

    // ---- tracks -------------------------------------------------------------

    fn load_tracks(&mut self) -> Vec<Track> {
        subdirectories(&self.root.join("tracks"))
            .into_iter()
            .filter_map(|track_dir| self.load_track(&track_dir))
            .collect()
    }

    fn load_track(&mut self, track_dir: &Path) -> Option<Track> {
        let manifest_path = track_dir.join("track.yaml");
        if !manifest_path.is_file() {
            self.report
                .error(self.relative(track_dir), "missing track.yaml");
            return None;
        }
        let manifest = self.read_manifest::<TrackManifest>(&manifest_path, FileKind::Track)?;
        if folder_name(track_dir) != manifest.id {
            self.report.error(
                self.relative(&manifest_path),
                format!("track id `{}` must match its folder name", manifest.id),
            );
        }
        let phases = manifest
            .phases
            .iter()
            .map(|phase| self.load_phase(&manifest.id, phase, &track_dir.join(&phase.dir)))
            .collect();
        Some(Track {
            id: manifest.id,
            name: manifest.name,
            archetype: manifest.archetype,
            category: manifest.category,
            icon: manifest.icon,
            description: manifest.description,
            status: manifest.status,
            belts: manifest.belts.into_iter().map(to_belt).collect(),
            phases,
        })
    }

    fn load_phase(&mut self, track_id: &str, manifest: &PhaseManifest, phase_dir: &Path) -> Phase {
        let nodes = subdirectories(phase_dir)
            .into_iter()
            .filter_map(|node_dir| self.load_node(track_id, &manifest.id, &node_dir))
            .collect();
        Phase {
            id: manifest.id.clone(),
            order: phase_order(&manifest.id),
            title: manifest.title.clone(),
            goal: manifest.goal.clone(),
            nodes,
        }
    }

    // ---- nodes --------------------------------------------------------------

    fn load_node(&mut self, track_id: &str, phase_id: &str, node_dir: &Path) -> Option<Node> {
        let folder = folder_name(node_dir);
        let Some(slug) = NODE_FOLDER
            .captures(&folder)
            .map(|caps| caps[1].to_string())
        else {
            self.report.error(
                self.relative(node_dir),
                "node folder must be named `NN-slug` (e.g. `04-loops`)",
            );
            return None;
        };
        let manifest_path = node_dir.join("node.yaml");
        if !manifest_path.is_file() {
            self.report
                .error(self.relative(node_dir), "missing node.yaml");
            return None;
        }
        let manifest = self.read_manifest::<NodeManifest>(&manifest_path, FileKind::Node)?;
        let expected_id = format!("{track_id}.{phase_id}.{slug}");
        if manifest.id != expected_id {
            self.report.error(
                self.relative(&manifest_path),
                format!(
                    "node id `{}` must be `{expected_id}` (track.phase.folder-slug)",
                    manifest.id
                ),
            );
        }
        let lesson = manifest
            .lesson
            .as_ref()
            .and_then(|file| self.read_text(&node_dir.join(file)));
        let bridges = match &manifest.bridges {
            Some(dir) => self.load_bridges(&node_dir.join(dir)),
            None => BTreeMap::new(),
        };
        Some(to_node(manifest, lesson, bridges))
    }

    fn load_bridges(&mut self, bridges_dir: &Path) -> BTreeMap<String, Bridge> {
        if !bridges_dir.is_dir() {
            self.report.error(
                self.relative(bridges_dir),
                "bridges folder declared in node.yaml does not exist",
            );
            return BTreeMap::new();
        }
        files_with_extension(bridges_dir, "md")
            .into_iter()
            .filter_map(|path| self.load_bridge(&path))
            .map(|bridge| (bridge.from.clone(), bridge))
            .collect()
    }

    fn load_bridge(&mut self, path: &Path) -> Option<Bridge> {
        let text = self.read_text(path)?;
        let Some((yaml, body)) = front_matter::split(&text) else {
            self.report.error(
                self.relative(path),
                "bridge must start with a `---` YAML front matter block",
            );
            return None;
        };
        let front: BridgeFrontMatter = self.parse_manifest(path, yaml, FileKind::Bridge)?;
        let stem = path
            .file_stem()
            .and_then(|s| s.to_str())
            .unwrap_or_default();
        if front.from != stem {
            self.report.error(
                self.relative(path),
                format!(
                    "`from: {}` must match the file name `{stem}.md`",
                    front.from
                ),
            );
        }
        Some(Bridge {
            from: front.from,
            concept: front.concept,
            from_way: front.from_way,
            native_way: front.native_way,
            sensei_tip: front.sensei_tip,
            body: body.to_string(),
        })
    }

    // ---- file helpers -------------------------------------------------------

    fn read_manifest<T: DeserializeOwned>(&mut self, path: &Path, kind: FileKind) -> Option<T> {
        let text = self.read_text(path)?;
        self.parse_manifest(path, &text, kind)
    }

    /// YAML → JSON value → schema check → typed manifest. Every failure is reported.
    fn parse_manifest<T: DeserializeOwned>(
        &mut self,
        path: &Path,
        yaml: &str,
        kind: FileKind,
    ) -> Option<T> {
        let location = self.relative(path);
        let value: Value = match serde_norway::from_str(yaml) {
            Ok(value) => value,
            Err(error) => {
                self.report
                    .error(&location, format!("invalid YAML: {error}"));
                return None;
            }
        };
        let violations = schema::violations(self.validator(kind), &value);
        if !violations.is_empty() {
            for violation in violations {
                self.report.error(&location, format!("schema: {violation}"));
            }
            return None;
        }
        match serde_json::from_value(value) {
            Ok(manifest) => Some(manifest),
            Err(error) => {
                self.report
                    .error(&location, format!("unexpected structure: {error}"));
                None
            }
        }
    }

    fn validator(&self, kind: FileKind) -> &jsonschema::Validator {
        match kind {
            FileKind::Track => &self.schemas.track,
            FileKind::Node => &self.schemas.node,
            FileKind::Skills => &self.schemas.skills,
            FileKind::Bridge => &self.schemas.bridge,
        }
    }

    fn read_text(&mut self, path: &Path) -> Option<String> {
        match fs::read_to_string(path) {
            Ok(text) => Some(text),
            Err(error) => {
                self.report
                    .error(self.relative(path), format!("cannot read file: {error}"));
                None
            }
        }
    }

    fn relative(&self, path: &Path) -> String {
        path.strip_prefix(self.root)
            .unwrap_or(path)
            .to_string_lossy()
            .replace('\\', "/")
    }
}

// ---- conversions ------------------------------------------------------------

fn to_belt(belt: BeltManifest) -> Belt {
    Belt {
        name: belt.name,
        required_xp: belt.required_xp,
        title: belt.title,
    }
}

fn to_objective(objective: ObjectiveManifest) -> Objective {
    Objective {
        id: objective.id,
        text: objective.text,
        skills: objective.skills,
    }
}

fn to_node(
    manifest: NodeManifest,
    lesson: Option<String>,
    bridges: BTreeMap<String, Bridge>,
) -> Node {
    Node {
        id: manifest.id,
        legacy_id: manifest.legacy_id,
        kind: manifest.kind,
        status: manifest.status,
        title: manifest.title,
        summary: manifest.summary,
        objectives: manifest.objectives.into_iter().map(to_objective).collect(),
        prerequisites: manifest.prerequisites,
        difficulty: manifest.difficulty,
        estimated_minutes: manifest.estimated_minutes,
        xp: manifest.xp,
        lesson,
        bridges,
    }
}

/// `p3` → 3. The schema guarantees the `p[0-4]` shape.
fn phase_order(phase_id: &str) -> u8 {
    phase_id.trim_start_matches('p').parse().unwrap_or(u8::MAX)
}

// ---- directory helpers ------------------------------------------------------

fn sorted_entries(dir: &Path) -> Vec<PathBuf> {
    let Ok(entries) = fs::read_dir(dir) else {
        return Vec::new();
    };
    let mut paths: Vec<PathBuf> = entries
        .filter_map(|entry| entry.ok().map(|e| e.path()))
        .collect();
    paths.sort();
    paths
}

fn subdirectories(dir: &Path) -> Vec<PathBuf> {
    sorted_entries(dir)
        .into_iter()
        .filter(|path| path.is_dir())
        .collect()
}

fn files_with_extension(dir: &Path, extension: &str) -> Vec<PathBuf> {
    sorted_entries(dir)
        .into_iter()
        .filter(|path| path.is_file() && path.extension().is_some_and(|ext| ext == extension))
        .collect()
}

fn folder_name(path: &Path) -> String {
    path.file_name()
        .map(|name| name.to_string_lossy().into_owned())
        .unwrap_or_default()
}
