use dojo_content::{LoadedContent, Severity, load};
use std::fs;
use std::path::{Path, PathBuf};
use tempfile::TempDir;

/// Builds a throwaway `content/` tree that uses the real schemas of the repository.
struct Fixture {
    dir: TempDir,
}

impl Fixture {
    fn new() -> Self {
        let fixture = Self {
            dir: TempDir::new().unwrap(),
        };
        let schemas = Path::new(env!("CARGO_MANIFEST_DIR")).join("../../content/schemas");
        for entry in fs::read_dir(schemas).unwrap() {
            let path = entry.unwrap().path();
            fixture.write(
                &format!("schemas/{}", path.file_name().unwrap().to_string_lossy()),
                &fs::read_to_string(&path).unwrap(),
            );
        }
        fixture.write(
            "skills/demo.yaml",
            "namespace: demo\nskills:\n  - { id: demo.basics, title: Basics }\n  - { id: demo.more, title: More }\n",
        );
        fixture.write("tracks/demo/track.yaml", &track_yaml("draft"));
        fixture
    }

    fn root(&self) -> PathBuf {
        self.dir.path().to_path_buf()
    }

    fn write(&self, relative: &str, text: &str) {
        let path = self.dir.path().join(relative);
        fs::create_dir_all(path.parent().unwrap()).unwrap();
        fs::write(path, text).unwrap();
    }

    fn node(&self, folder: &str, yaml: &str) {
        self.write(&format!("tracks/demo/{folder}/node.yaml"), yaml);
        self.write(&format!("tracks/demo/{folder}/lesson.md"), "# Lesson\n");
    }

    fn load(&self) -> LoadedContent {
        load(&self.root()).unwrap()
    }
}

fn track_yaml(status: &str) -> String {
    format!(
        r#"schema_version: 2
id: demo
name: Demo
archetype: language
category: Programming Languages
icon: "🧪"
description: A demo track.
status: {status}
belts:
  - {{ name: White Belt, required_xp: 0, title: Novice }}
  - {{ name: Yellow Belt, required_xp: 300, title: Apprentice }}
phases:
  - {{ id: p0, dir: p0-first-steps, title: First Steps, goal: Start }}
  - {{ id: p1, dir: p1-fundamentals, title: Fundamentals, goal: Basics }}
  - {{ id: p2, dir: p2-intermediate, title: Intermediate, goal: More }}
  - {{ id: p3, dir: p3-advanced, title: Advanced, goal: Hard }}
  - {{ id: p4, dir: p4-mastery, title: Mastery, goal: Production }}
"#
    )
}

fn node_yaml(id: &str, prerequisites: &[&str], skill: &str) -> String {
    format!(
        r#"id: {id}
kind: lesson
status: draft
title: Title of {id}
summary: Summary.
objectives:
  - {{ id: o1, text: Learn it, skills: [{skill}] }}
prerequisites: [{}]
difficulty: 1
estimated_minutes: 10
xp: 100
lesson: lesson.md
"#,
        prerequisites.join(", ")
    )
}

fn errors(content: &LoadedContent) -> Vec<String> {
    content
        .report
        .issues()
        .iter()
        .filter(|issue| issue.severity == Severity::Error)
        .map(ToString::to_string)
        .collect()
}

#[test]
fn valid_draft_track_loads_with_warnings_only() {
    let fixture = Fixture::new();
    fixture.node(
        "p0-first-steps/01-setup",
        &node_yaml("demo.p0.setup", &[], "demo.basics"),
    );
    fixture.node(
        "p1-fundamentals/01-loops",
        &node_yaml("demo.p1.loops", &["demo.p0.setup"], "demo.more"),
    );
    fixture.write(
        "tracks/demo/p1-fundamentals/01-loops/bridges/java.md",
        "---\nfrom: java\nstatus: draft\nconcept: Loops\nfrom_way: for (int i...)\nnative_way: for x in v\nsensei_tip: Prefer iterators.\n---\n\nMore text.\n",
    );
    fixture.write(
        "tracks/demo/p1-fundamentals/01-loops/node.yaml",
        &format!(
            "{}bridges: bridges\n",
            node_yaml("demo.p1.loops", &["demo.p0.setup"], "demo.more")
        ),
    );

    let content = fixture.load();

    assert!(errors(&content).is_empty(), "{:?}", errors(&content));
    assert!(
        content.report.count(Severity::Warning) > 0,
        "a 2-node draft must warn about completeness"
    );
    let track = content.catalog.track("demo").unwrap();
    assert_eq!(track.node_count(), 2);
    let loops = track.node("demo.p1.loops").unwrap();
    assert_eq!(loops.bridge_for("java").unwrap().body, "More text.");
    assert_eq!(loops.lesson.as_deref(), Some("# Lesson\n"));
}

#[test]
fn schema_violations_are_reported_and_the_node_skipped() {
    let fixture = Fixture::new();
    let invalid =
        node_yaml("demo.p0.setup", &[], "demo.basics").replace("difficulty: 1", "difficulty: 9");
    fixture.node("p0-first-steps/01-setup", &invalid);

    let content = fixture.load();

    assert!(
        errors(&content)
            .iter()
            .any(|e| e.contains("schema") && e.contains("difficulty")),
        "{:?}",
        errors(&content)
    );
    assert_eq!(content.catalog.track("demo").unwrap().node_count(), 0);
}

#[test]
fn node_id_must_match_track_phase_and_folder() {
    let fixture = Fixture::new();
    fixture.node(
        "p0-first-steps/01-setup",
        &node_yaml("demo.p0.install", &[], "demo.basics"),
    );

    assert!(
        errors(&fixture.load())
            .iter()
            .any(|e| e.contains("must be `demo.p0.setup`"))
    );
}

#[test]
fn badly_named_node_folder_is_rejected() {
    let fixture = Fixture::new();
    fixture.node(
        "p0-first-steps/setup",
        &node_yaml("demo.p0.setup", &[], "demo.basics"),
    );

    assert!(
        errors(&fixture.load())
            .iter()
            .any(|e| e.contains("NN-slug"))
    );
}

#[test]
fn missing_prerequisite_and_unknown_skill_are_errors() {
    let fixture = Fixture::new();
    fixture.node(
        "p0-first-steps/01-setup",
        &node_yaml("demo.p0.setup", &["demo.p0.ghost"], "demo.unknown"),
    );

    let found = errors(&fixture.load());

    assert!(
        found
            .iter()
            .any(|e| e.contains("`demo.p0.ghost` does not exist")),
        "{found:?}"
    );
    assert!(
        found
            .iter()
            .any(|e| e.contains("unknown skill `demo.unknown`")),
        "{found:?}"
    );
}

#[test]
fn prerequisite_in_a_later_phase_is_an_error() {
    let fixture = Fixture::new();
    fixture.node(
        "p0-first-steps/01-setup",
        &node_yaml("demo.p0.setup", &["demo.p1.loops"], "demo.basics"),
    );
    fixture.node(
        "p1-fundamentals/01-loops",
        &node_yaml("demo.p1.loops", &[], "demo.basics"),
    );

    assert!(
        errors(&fixture.load())
            .iter()
            .any(|e| e.contains("later phase"))
    );
}

#[test]
fn prerequisite_cycle_is_an_error() {
    let fixture = Fixture::new();
    fixture.node(
        "p1-fundamentals/01-a",
        &node_yaml("demo.p1.a", &["demo.p1.b"], "demo.basics"),
    );
    fixture.node(
        "p1-fundamentals/02-b",
        &node_yaml("demo.p1.b", &["demo.p1.a"], "demo.basics"),
    );

    assert!(errors(&fixture.load()).iter().any(|e| e.contains("cycle")));
}

#[test]
fn reviewed_track_turns_completeness_gaps_into_errors() {
    let fixture = Fixture::new();
    fixture.write("tracks/demo/track.yaml", &track_yaml("reviewed"));
    fixture.node(
        "p0-first-steps/01-setup",
        &node_yaml("demo.p0.setup", &[], "demo.basics"),
    );

    let found = errors(&fixture.load());

    assert!(
        found
            .iter()
            .any(|e| e.contains("must end with a checkpoint")),
        "{found:?}"
    );
    assert!(
        found.iter().any(|e| e.contains("expected 15–25")),
        "{found:?}"
    );
}

#[test]
fn bridge_file_name_must_match_its_language() {
    let fixture = Fixture::new();
    fixture.node(
        "p0-first-steps/01-setup",
        &format!(
            "{}bridges: bridges\n",
            node_yaml("demo.p0.setup", &[], "demo.basics")
        ),
    );
    fixture.write(
        "tracks/demo/p0-first-steps/01-setup/bridges/java.md",
        "---\nfrom: python\nstatus: draft\nconcept: C\nfrom_way: F\nnative_way: N\nsensei_tip: T\n---\n",
    );

    assert!(
        errors(&fixture.load())
            .iter()
            .any(|e| e.contains("must match the file name"))
    );
}

#[test]
fn repository_content_has_no_errors() {
    let content = Path::new(env!("CARGO_MANIFEST_DIR")).join("../../content");
    let loaded = load(&content).unwrap();

    assert!(errors(&loaded).is_empty(), "{:#?}", errors(&loaded));
    assert_eq!(loaded.catalog.tracks.len(), 29);
}
