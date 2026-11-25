use crate::front_matter;
use crate::manifest::{
    BeltManifest, BridgeFrontMatter, CareerManifest, CareerRequirementManifest, InterviewManifest,
    KataManifest, NodeManifest, ObjectiveManifest, PhaseManifest, PlacementManifest,
    QuestionManifest, QuizManifest, RemedialManifest, RequirementManifest, RunnerManifest,
    SidequestManifest, SkillsManifest, TrackManifest,
};
use crate::report::Report;
use crate::schema::{self, Schemas};
use anyhow::{Result, bail};
use dojo_core::{
    Belt, Bridge, Career, CareerRequirement, Catalog, DEFAULT_PASS_PERCENT, EarlyStop, EntryOption,
    Exercise, Kata, Node, Objective, Phase, PhaseRule, PlacementConfig, PlacementItem, Question,
    QuestionKind, Quiz, Remedial, Requirement, RequirementKind, RunnerSpec, Sidequest,
    SidequestRequirement, SidequestStage, Skill, SkillId, Stage, Track,
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
static REMEDIAL_SLUG: LazyLock<Regex> =
    LazyLock::new(|| Regex::new(r"^[a-z0-9]+(?:-[a-z0-9]+)*$").expect("valid regex"));

/// The folder of a track that holds its sidequests, one subfolder each.
pub const SIDEQUESTS_FOLDER: &str = "sidequests";
/// The statement of a sidequest stage, inside its folder.
const STAGE_STATEMENT: &str = "README.md";

#[derive(Clone, Copy)]
enum FileKind {
    Track,
    Node,
    Skills,
    Bridge,
    Quiz,
    Placement,
    Interview,
    Career,
    Sidequest,
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
    let careers = reader.load_careers();
    let sidequests = reader.load_sidequests();
    Ok(Catalog {
        tracks,
        skills,
        careers,
        sidequests,
    })
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
        let placement_path = track_dir.join("placement.yaml");
        let placement = if placement_path.is_file() {
            self.load_placement(&placement_path)
        } else {
            None
        };
        let interview_path = track_dir.join("interview.yaml");
        let interview = if interview_path.is_file() {
            self.load_interview(&interview_path)
        } else {
            None
        };
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
            placement,
            interview,
        })
    }

    fn load_interview(&mut self, path: &Path) -> Option<dojo_core::InterviewBank> {
        let manifest = self.read_manifest::<InterviewManifest>(path, FileKind::Interview)?;
        let questions = manifest
            .questions
            .into_iter()
            .filter_map(|item| {
                Some(dojo_core::InterviewQuestion {
                    level: dojo_core::InterviewLevel::parse(&item.level)?,
                    id: item.id,
                    skills: item.skills,
                    question: item.question,
                    key_points: item.key_points,
                    follow_up: item.follow_up,
                })
            })
            .collect();
        Some(dojo_core::InterviewBank {
            sources: manifest.sources,
            questions,
        })
    }

    fn load_placement(&mut self, path: &Path) -> Option<PlacementConfig> {
        let manifest = self.read_manifest::<PlacementManifest>(path, FileKind::Placement)?;
        let location = self.relative(path);
        let mut items = Vec::new();
        for item in manifest.items {
            match to_question(item.question) {
                Ok(question) => items.push(PlacementItem {
                    phase: item.phase,
                    difficulty: item.difficulty,
                    question,
                }),
                Err(message) => self.report.error(&location, message),
            }
        }
        Some(PlacementConfig {
            entry_prompt: manifest.entry_question.prompt,
            entry_options: manifest
                .entry_question
                .options
                .into_iter()
                .map(|option| EntryOption {
                    label: option.label,
                    start_phase: option.start_phase,
                    test: option.test,
                })
                .collect(),
            phases: manifest
                .phases
                .into_iter()
                .map(|(phase, rule)| {
                    let rule = PhaseRule {
                        items: rule.items_per_phase,
                        pass: rule.pass,
                        partial: rule.partial,
                    };
                    (phase, rule)
                })
                .collect(),
            early_stop: EarlyStop {
                wrong: manifest.early_stop.wrong_in_first,
                of: manifest.early_stop.of,
            },
            skill_threshold: manifest.skill_placed_out_threshold,
            weights_by_difficulty: manifest.weights_by_difficulty,
            max_items: manifest.max_items,
            items,
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
        let remedial = self.load_remedial(node_dir, &manifest_path, &manifest);
        Some(to_node(
            manifest,
            (lesson, lesson_file),
            bridges,
            (kata, remedial),
            quiz,
        ))
    }

    /// The node's practice katas. Each one needs a kata of its own, a unique slug, and a
    /// parent that is itself a kata or a checkpoint (practice is for hands-on work).
    fn load_remedial(
        &mut self,
        node_dir: &Path,
        manifest_path: &Path,
        manifest: &NodeManifest,
    ) -> Vec<Remedial> {
        if manifest.remedial.is_empty() {
            return Vec::new();
        }
        let location = self.relative(manifest_path);
        if manifest.kata.is_none() {
            self.report.error(
                &location,
                "`remedial` needs a node with a kata (a kata or a checkpoint)",
            );
        }
        let mut seen = std::collections::BTreeSet::new();
        let mut remedial = Vec::new();
        for item in &manifest.remedial {
            if !REMEDIAL_SLUG.is_match(&item.id) {
                self.report.error(
                    &location,
                    format!("remedial id `{}` must be a lower-case slug", item.id),
                );
                continue;
            }
            if !seen.insert(item.id.clone()) {
                self.report
                    .error(&location, format!("remedial id `{}` is repeated", item.id));
                continue;
            }
            if let Some(kata) = self.load_kata(node_dir, &item.kata) {
                remedial.push(to_remedial(&manifest.id, item, kata));
            }
        }
        remedial
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
            hints: manifest.hints.clone(),
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

    // ---- careers ------------------------------------------------------------

    fn load_careers(&mut self) -> Vec<Career> {
        files_with_extension(&self.root.join("careers"), "yaml")
            .into_iter()
            .filter_map(|path| self.read_manifest::<CareerManifest>(&path, FileKind::Career))
            .map(to_career)
            .collect()
    }

    // ---- sidequests ---------------------------------------------------------

    /// Every `tracks/<track>/sidequests/<slug>/sidequest.yaml`.
    fn load_sidequests(&mut self) -> Vec<Sidequest> {
        subdirectories(&self.root.join("tracks"))
            .into_iter()
            .flat_map(|track_dir| subdirectories(&track_dir.join(SIDEQUESTS_FOLDER)))
            .filter_map(|dir| self.load_sidequest(&dir))
            .collect()
    }

    fn load_sidequest(&mut self, dir: &Path) -> Option<Sidequest> {
        let manifest_path = dir.join("sidequest.yaml");
        if !manifest_path.is_file() {
            self.report
                .error(self.relative(dir), "missing sidequest.yaml");
            return None;
        }
        let manifest: SidequestManifest =
            self.read_manifest(&manifest_path, FileKind::Sidequest)?;
        let location = self.relative(&manifest_path);
        let slug = folder_name(dir);
        let track_id = dir
            .parent()
            .and_then(Path::parent)
            .map(folder_name)
            .unwrap_or_default();
        let expected_id = format!("{track_id}.sidequest.{slug}");
        if manifest.id != expected_id {
            self.report.error(
                &location,
                format!(
                    "sidequest id `{}` must be `{expected_id}` (track.sidequest.folder)",
                    manifest.id
                ),
            );
        }
        let requires = manifest
            .requires
            .iter()
            .filter_map(|text| {
                let parsed = SidequestRequirement::parse(text);
                if parsed.is_none() {
                    self.report.error(
                        &location,
                        format!("requirement `{text}` must be `track:P<n>` with n >= 1"),
                    );
                }
                parsed
            })
            .collect();
        let statement = self.read_text(&dir.join(&manifest.statement))?;
        let stages = self.load_stages(dir, &manifest)?;
        Some(Sidequest {
            id: manifest.id,
            slug,
            track_id,
            level: manifest.level,
            title: manifest.title,
            summary: manifest.summary,
            statement,
            skills: manifest.skills,
            requires,
            stages,
        })
    }

    /// Stage N is `stages/NN-<id>/`; the first one must have a starter, the project
    /// the learner begins from. `None` when any stage is broken.
    fn load_stages(
        &mut self,
        dir: &Path,
        manifest: &SidequestManifest,
    ) -> Option<Vec<SidequestStage>> {
        let mut stages = Vec::new();
        let mut broken = false;
        let mut seen = std::collections::BTreeSet::new();
        for (index, item) in manifest.stages.iter().enumerate() {
            if !seen.insert(item.id.as_str()) {
                self.report.error(
                    self.relative(&dir.join("sidequest.yaml")),
                    format!("stage id `{}` is repeated", item.id),
                );
                broken = true;
                continue;
            }
            let stage_dir = dir
                .join("stages")
                .join(format!("{:02}-{}", index + 1, item.id));
            let runner = item.runner.as_ref().unwrap_or(&manifest.runner);
            match self.load_stage(&stage_dir, index == 0, runner) {
                Some(kata) => stages.push(SidequestStage {
                    id: format!("{}.{}", manifest.id, item.id),
                    slug: item.id.clone(),
                    title: item.title.clone(),
                    kata,
                }),
                None => broken = true,
            }
        }
        (!broken).then_some(stages)
    }

    fn load_stage(
        &mut self,
        stage_dir: &Path,
        first: bool,
        runner: &RunnerManifest,
    ) -> Option<Kata> {
        if !stage_dir.is_dir() {
            self.report.error(
                self.relative(stage_dir),
                "stage folder is missing (stage N lives in `stages/NN-<stage id>`)",
            );
            return None;
        }
        let statement = self.read_text(&stage_dir.join(STAGE_STATEMENT));
        let tests_dir = self.existing_dir(stage_dir, "tests", "tests");
        let reference_dir = self.existing_dir(stage_dir, "reference", "reference");
        let starter_dir = if first {
            self.existing_dir(stage_dir, "starter", "starter")
        } else {
            Some(stage_dir.join("starter"))
        };
        Some(Kata {
            statement: statement?,
            starter_dir: starter_dir?,
            tests_dir: tests_dir?,
            reference_dir: reference_dir?,
            runner: RunnerSpec {
                image: runner.image.clone(),
                timeout_secs: runner.timeout_secs,
            },
            hints: Vec::new(),
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
            FileKind::Placement => &self.schemas.placement,
            FileKind::Career => &self.schemas.career,
            FileKind::Interview => &self.schemas.interview,
            FileKind::Sidequest => &self.schemas.sidequest,
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

fn to_career(manifest: CareerManifest) -> Career {
    Career {
        id: manifest.id,
        title: manifest.title,
        summary: manifest.summary,
        roles: manifest.roles,
        stages: manifest
            .stages
            .into_iter()
            .map(|stage| Stage {
                id: stage.id,
                title: stage.title,
                requires: stage
                    .requires
                    .into_iter()
                    .map(to_career_requirement)
                    .collect(),
            })
            .collect(),
        next_paths: manifest.next_paths,
    }
}

fn to_career_requirement(manifest: CareerRequirementManifest) -> CareerRequirement {
    match manifest {
        CareerRequirementManifest::TrackPhase { track, phase } => {
            CareerRequirement::TrackPhase { track, phase }
        }
        CareerRequirementManifest::Skill { skill, min } => CareerRequirement::Skill { skill, min },
        CareerRequirementManifest::AnyOf { any_of } => {
            CareerRequirement::AnyOf(any_of.into_iter().map(to_career_requirement).collect())
        }
    }
}

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
        skills: manifest.skills,
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
    (kata, remedial): (Option<Kata>, Vec<Remedial>),
    quiz: Option<Quiz>,
) -> Node {
    let requirements = manifest
        .requirements
        .into_iter()
        .map(to_requirement)
        .collect();
    let exercise = manifest.exercise.map(|exercise| Exercise {
        task: exercise.task,
        criteria: exercise.criteria,
    });
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
        exercise,
        requirements,
        remedial,
    }
}

fn to_remedial(node_id: &str, item: &RemedialManifest, kata: Kata) -> Remedial {
    Remedial {
        id: format!("{node_id}.practice.{}", item.id),
        slug: item.id.clone(),
        title: item.title.clone(),
        summary: item.summary.clone(),
        skills: item.skills.clone(),
        kata,
    }
}

/// `p3` → 3. The schema guarantees the `p<n>` shape.
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
