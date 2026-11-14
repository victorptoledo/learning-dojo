use crate::front_matter;
use crate::manifest::{
    BeltManifest, BridgeFrontMatter, KataManifest, NodeManifest, ObjectiveManifest, PhaseManifest,
    QuestionManifest, QuizManifest, RequirementManifest, SkillsManifest, TrackManifest,
};
use crate::report::Report;
use crate::schema::{self, Schemas};
use anyhow::{Result, bail};
use dojo_core::{
    Belt, Bridge, Catalog, DEFAULT_PASS_PERCENT, Kata, Node, Objective, Phase, Question,
    QuestionKind, Quiz, Requirement, RequirementKind, RunnerSpec, Skill, SkillId, Track,
};
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
    Quiz,
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
            requirements: manifest
                .requirements
                .into_iter()
                .map(to_requirement)
                .collect(),
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
        let kata = manifest
            .kata
            .as_ref()
            .and_then(|kata| self.load_kata(node_dir, kata));
        let quiz = manifest
            .quiz
            .as_ref()
            .and_then(|file| self.load_quiz(&node_dir.join(file)));
        let lesson_file = manifest.lesson.as_ref().map(|file| node_dir.join(file));
        Some(to_node(
            manifest,
            (lesson, lesson_file),
            bridges,
            kata,
            quiz,
        ))
    }

    fn load_quiz(&mut self, path: &Path) -> Option<Quiz> {
        let manifest: QuizManifest = self.read_manifest(path, FileKind::Quiz)?;
        let location = self.relative(path);
        let questions = manifest
            .questions
            .into_iter()
            .filter_map(|question| match to_question(question) {
                Ok(question) => Some(question),
                Err(problem) => {
                    self.report.error(&location, problem);
                    None
                }
            })
            .collect();
        Some(Quiz {
            pass_percent: manifest.pass_percent.unwrap_or(DEFAULT_PASS_PERCENT),
            questions,
        })
    }

    /// Resolves the kata folders and reads its statement; every missing piece is reported.
    fn load_kata(&mut self, node_dir: &Path, manifest: &KataManifest) -> Option<Kata> {
        let statement = self.read_text(&node_dir.join(&manifest.statement));
        let starter_dir = self.existing_dir(node_dir, &manifest.starter, "starter");
        let tests_dir = self.existing_dir(node_dir, &manifest.tests, "tests");
        let reference_dir = self.existing_dir(node_dir, &manifest.reference, "reference");
        Some(Kata {
            statement: statement?,
            starter_dir: starter_dir?,
            tests_dir: tests_dir?,
            reference_dir: reference_dir?,
            runner: RunnerSpec {
                image: manifest.runner.image.clone(),
                timeout_secs: manifest.runner.timeout_secs,
            },
        })
    }

    fn existing_dir(&mut self, node_dir: &Path, relative: &str, role: &str) -> Option<PathBuf> {
        let dir = node_dir.join(relative);
        let has_files = fs::read_dir(&dir).is_ok_and(|mut entries| entries.next().is_some());
        if has_files {
            Some(dir)
        } else {
            self.report.error(
                self.relative(&dir),
                format!("kata {role} folder is missing or empty"),
            );
            None
        }
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
            FileKind::Quiz => &self.schemas.quiz,
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

/// The schema guarantees one of the two shapes; only the answer index needs checking here.
fn to_question(manifest: QuestionManifest) -> Result<Question, String> {
    let kind = match (manifest.options, manifest.answer, manifest.answers) {
        (Some(options), Some(answer), None) if answer < options.len() => {
            QuestionKind::SingleChoice { options, answer }
        }
        (Some(options), Some(answer), None) => {
            return Err(format!(
                "question `{}`: answer {answer} is out of range ({} options, counted from 0)",
                manifest.id,
                options.len()
            ));
        }
        (None, None, Some(answers)) => QuestionKind::Text { answers },
        _ => {
            return Err(format!(
                "question `{}`: needs options + answer, or answers",
                manifest.id
            ));
        }
    };
    Ok(Question {
        id: manifest.id,
        prompt: manifest.prompt,
        code: manifest.code,
        kind,
        explanation: manifest.explanation,
    })
}

/// The schema guarantees the fields each kind needs.
fn to_requirement(manifest: RequirementManifest) -> Requirement {
    let kind = match manifest.kind.as_str() {
        "tool" => RequirementKind::Tool {
            command: manifest.command.unwrap_or_default(),
            version_args: manifest.version_args,
            min_version: manifest.min_version,
            install_hint: manifest.install_hint.unwrap_or_default(),
            update: manifest.update,
        },
        "vscode-extension" => RequirementKind::VsCodeExtension {
            id: manifest.id.unwrap_or_default(),
        },
        "docker-engine" => RequirementKind::DockerEngine {
            install_hint: manifest.install_hint.unwrap_or_default(),
        },
        _ => RequirementKind::DockerImage {
            image: manifest.image.unwrap_or_default(),
        },
    };
    Requirement {
        name: manifest.name,
        why: manifest.why,
        required: manifest.required,
        kind,
    }
}

fn to_node(
    manifest: NodeManifest,
    (lesson, lesson_file): (Option<String>, Option<PathBuf>),
    bridges: BTreeMap<String, Bridge>,
    kata: Option<Kata>,
    quiz: Option<Quiz>,
) -> Node {
    let requirements = manifest
        .requirements
        .into_iter()
        .map(to_requirement)
        .collect();
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
        lesson_file,
        bridges,
        kata,
        quiz,
        requirements,
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
