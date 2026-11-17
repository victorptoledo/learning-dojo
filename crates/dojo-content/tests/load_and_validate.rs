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

const QUIZ: &str = "pass_percent: 75
questions:
  - id: q1
    prompt: Pick b.
    options: [a, b]
    answer: 1
    explanation: It is b.
  - id: q2
    prompt: What does it print?
    code: |
      println!(\"{}\", 1 + 1);
    answers: [\"2\"]
    explanation: One plus one.
  - id: q3
    prompt: Type yes.
    answers: [yes, y]
    explanation: Yes.
";

fn lesson_with_quiz(fixture: &Fixture, quiz: &str) {
    fixture.node(
        "p0-first-steps/01-setup",
        &format!(
            "{}quiz: quiz.yaml\n",
            node_yaml("demo.p0.setup", &[], "demo.basics")
        ),
    );
    fixture.write("tracks/demo/p0-first-steps/01-setup/quiz.yaml", quiz);
}

#[test]
fn a_lesson_with_document_and_quiz_is_ready_for_learners() {
    let fixture = Fixture::new();
    lesson_with_quiz(&fixture, QUIZ);

    let content = fixture.load();

    assert!(errors(&content).is_empty(), "{:?}", errors(&content));
    let node = content
        .catalog
        .track("demo")
        .unwrap()
        .node("demo.p0.setup")
        .unwrap();
    let quiz = node.quiz.as_ref().unwrap();
    assert_eq!(quiz.pass_percent, 75);
    assert_eq!(quiz.questions.len(), 3);
    assert_eq!(
        quiz.questions[1].code.as_deref(),
        Some("println!(\"{}\", 1 + 1);\n")
    );
    assert!(!node.under_construction());
}

#[test]
fn a_lesson_without_quiz_is_reported_as_under_construction() {
    let fixture = Fixture::new();
    fixture.node(
        "p0-first-steps/01-setup",
        &node_yaml("demo.p0.setup", &[], "demo.basics"),
    );

    let content = fixture.load();

    assert!(
        content
            .report
            .issues()
            .iter()
            .any(|issue| issue.message.contains("under construction"))
    );
}

#[test]
fn quiz_answer_out_of_range_is_an_error() {
    let fixture = Fixture::new();
    lesson_with_quiz(
        &fixture,
        "questions:\n  - { id: q1, prompt: P, options: [a, b], answer: 2, explanation: E }\n",
    );

    let found = errors(&fixture.load());

    assert!(
        found.iter().any(|e| e.contains("out of range")),
        "{found:?}"
    );
}

#[test]
fn question_with_both_options_and_typed_answers_breaks_the_schema() {
    let fixture = Fixture::new();
    lesson_with_quiz(
        &fixture,
        "questions:\n  - { id: q1, prompt: P, options: [a, b], answer: 0, answers: [a], explanation: E }\n",
    );

    let found = errors(&fixture.load());

    assert!(found.iter().any(|e| e.contains("schema")), "{found:?}");
}

#[test]
fn repository_content_has_no_errors() {
    let content = Path::new(env!("CARGO_MANIFEST_DIR")).join("../../content");
    let loaded = load(&content).unwrap();

    assert!(errors(&loaded).is_empty(), "{:#?}", errors(&loaded));
    assert_eq!(loaded.catalog.tracks.len(), 30);
}

#[test]
fn every_rust_kata_has_a_practice_kata_on_its_own_skills() {
    let content = Path::new(env!("CARGO_MANIFEST_DIR")).join("../../content");
    let catalog = load(&content).unwrap().catalog;
    let rust = catalog.track("rust").unwrap();

    for node in rust.nodes().filter(|node| node.kata.is_some()) {
        assert!(
            !node.remedial.is_empty(),
            "{} has no practice kata",
            node.id
        );
        for practice in &node.remedial {
            assert_eq!(
                practice.id,
                format!("{}.practice.{}", node.id, practice.slug)
            );
            assert!(practice.kata.starter_dir.is_dir());
            let own: Vec<&str> = node.skills().collect();
            assert!(
                practice
                    .skills
                    .iter()
                    .all(|skill| own.contains(&skill.as_str()))
            );
        }
    }
}

#[test]
fn every_rust_node_is_ready_for_learners() {
    let content = Path::new(env!("CARGO_MANIFEST_DIR")).join("../../content");
    let catalog = load(&content).unwrap().catalog;
    let rust = catalog.track("rust").unwrap();

    let unfinished: Vec<&str> = rust
        .nodes()
        .filter(|node| node.under_construction())
        .map(|node| node.id.as_str())
        .collect();

    assert!(unfinished.is_empty(), "{unfinished:?}");
}

// ---- sidequests ---------------------------------------------------------------

fn sidequest_yaml(id: &str, level: u8, requires: &str, stages: &[&str]) -> String {
    let stages: String = stages
        .iter()
        .map(|stage| format!("  - {{ id: {stage}, title: Stage {stage} }}\n"))
        .collect();
    format!(
        "id: {id}\nlevel: {level}\ntitle: A project\nsummary: Build it.\nstatement: README.md\nrequires: [{requires}]\nskills: [demo.basics]\nrunner: {{ image: dojo-rust, timeout_secs: 60 }}\nstages:\n{stages}"
    )
}

/// A sidequest with stages `one`, `two`, `three`; `test_names` gives each stage's test file.
fn write_sidequest(fixture: &Fixture, slug: &str, yaml: &str, test_names: [&str; 3]) {
    let dir = format!("tracks/demo/sidequests/{slug}");
    fixture.write(&format!("{dir}/sidequest.yaml"), yaml);
    fixture.write(&format!("{dir}/README.md"), "# Project\n");
    for (index, (stage, test)) in ["one", "two", "three"].iter().zip(test_names).enumerate() {
        let stage_dir = format!("{dir}/stages/{:02}-{stage}", index + 1);
        fixture.write(&format!("{stage_dir}/README.md"), &format!("## {stage}\n"));
        fixture.write(&format!("{stage_dir}/tests/{test}"), "#[test] fn t() {}\n");
        fixture.write(
            &format!("{stage_dir}/reference/src/main.rs"),
            "fn main() {}\n",
        );
    }
    fixture.write(
        &format!("{dir}/stages/01-one/starter/Cargo.toml"),
        "[package]\n",
    );
}

/// A node in phase p1, so `demo:P1` has evidence (a phase without a checkpoint).
fn track_with_p1(fixture: &Fixture) {
    fixture.node(
        "p1-fundamentals/01-loops",
        &node_yaml("demo.p1.loops", &[], "demo.basics"),
    );
}

#[test]
fn a_sidequest_loads_with_its_stages_in_order() {
    let fixture = Fixture::new();
    track_with_p1(&fixture);
    write_sidequest(
        &fixture,
        "calculator",
        &sidequest_yaml(
            "demo.sidequest.calculator",
            1,
            "demo:P1",
            &["one", "two", "three"],
        ),
        ["one.rs", "two.rs", "three.rs"],
    );

    let content = fixture.load();
    let errors = errors(&content);
    assert!(errors.is_empty(), "{errors:?}");
    let quest = content
        .catalog
        .sidequest("demo.sidequest.calculator")
        .unwrap();
    assert_eq!(quest.track_id, "demo");
    assert_eq!(quest.level, 1);
    assert_eq!(quest.statement, "# Project\n");
    let ids: Vec<&str> = quest.stages.iter().map(|stage| stage.id.as_str()).collect();
    assert_eq!(
        ids,
        [
            "demo.sidequest.calculator.one",
            "demo.sidequest.calculator.two",
            "demo.sidequest.calculator.three"
        ]
    );
    assert!(quest.stages[0].kata.starter_dir.is_dir());
    assert!(
        !quest.stages[1].kata.starter_dir.exists(),
        "later starters are optional"
    );
    assert_eq!(quest.stages[2].kata.statement, "## three\n");
    assert_eq!(quest.requires[0].to_string(), "demo:P1");
}

#[test]
fn sidequest_rules_catch_ids_levels_requirements_and_clashing_tests() {
    let fixture = Fixture::new();
    track_with_p1(&fixture);
    write_sidequest(
        &fixture,
        "first",
        &sidequest_yaml(
            "demo.sidequest.wrong",
            2,
            "demo:P1, nope:P1, demo:P2",
            &["one", "two", "three"],
        ),
        ["same.rs", "same.rs", "three.rs"],
    );

    let errors = errors(&fixture.load()).join("\n");
    for expected in [
        "must be `demo.sidequest.first`",
        "the first requirement must be `demo:P2`",
        "`nope:P1`: unknown track",
        "track `demo` is required twice",
        "`demo:P2`: the track has no node for it",
        "stages `one` and `two` both have tests/same.rs",
    ] {
        assert!(
            errors.contains(expected),
            "missing {expected:?} in:\n{errors}"
        );
    }
}

#[test]
fn a_sidequest_needs_every_stage_folder_and_a_first_starter() {
    let fixture = Fixture::new();
    track_with_p1(&fixture);
    let dir = "tracks/demo/sidequests/broken";
    fixture.write(
        &format!("{dir}/sidequest.yaml"),
        &sidequest_yaml(
            "demo.sidequest.broken",
            1,
            "demo:P1",
            &["one", "two", "three"],
        ),
    );
    fixture.write(&format!("{dir}/README.md"), "# Project\n");
    fixture.write(&format!("{dir}/stages/01-one/README.md"), "one\n");
    fixture.write(&format!("{dir}/stages/01-one/tests/a.rs"), "x\n");
    fixture.write(&format!("{dir}/stages/01-one/reference/a.rs"), "x\n");

    let content = fixture.load();
    let errors = errors(&content).join("\n");
    assert!(
        errors.contains("kata starter folder is missing"),
        "{errors}"
    );
    assert!(
        errors.contains("stages/02-two: stage folder is missing"),
        "{errors}"
    );
    assert!(
        content.catalog.sidequests.is_empty(),
        "a broken sidequest is skipped"
    );
}

#[test]
fn sidequest_tracks_have_one_sidequest_per_level_with_contract_stages() {
    let content = Path::new(env!("CARGO_MANIFEST_DIR")).join("../../content");
    let catalog = load(&content).unwrap().catalog;
    for track in ["rust", "java", "python", "go"] {
        let quests = catalog.sidequests_of(track);
        let levels: Vec<u8> = quests.iter().map(|quest| quest.level).collect();
        assert_eq!(levels, [1, 2, 3, 4], "{track}");
        let sizes: Vec<usize> = quests.iter().map(|quest| quest.stages.len()).collect();
        assert_eq!(sizes, [3, 4, 4, 5], "{track}");
        for quest in quests {
            assert_eq!(quest.requires[0].track, track);
            for stage in &quest.stages {
                assert!(stage.kata.tests_dir.is_dir(), "{}", stage.id);
                assert!(stage.kata.reference_dir.is_dir(), "{}", stage.id);
            }
        }
    }
}
