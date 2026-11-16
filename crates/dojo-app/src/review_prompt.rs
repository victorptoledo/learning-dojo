//! Review prompts for an AI of the learner's choice. The dojo does not call any AI: it writes
//! a prompt the learner pastes into a chat, with the written answer (lessons) or a list of
//! files to attach (katas). The review is feedback only and never changes progress.

use dojo_core::{Exercise, KataLanguage, Node, Phase, Track};
use std::path::{Path, PathBuf};

/// The marker in ANSWER.md; everything after it is the learner's answer.
pub const ANSWER_MARKER: &str = "<!-- Write your answer below this line. -->";

/// The ANSWER.md a lesson exercise starts with.
pub fn answer_template(node: &Node, exercise: &Exercise) -> String {
    format!(
        "# {}\n\n{}\n\nA good answer covers:\n{}\n\n{ANSWER_MARKER}\n\n",
        node.title,
        exercise.task.trim(),
        bullet_list(&exercise.criteria)
    )
}

/// The learner's answer in an ANSWER.md, or `None` when nothing was written yet.
pub fn answer_from(file_text: &str) -> Option<String> {
    let answer = match file_text.split_once(ANSWER_MARKER) {
        Some((_, after)) => after,
        None => file_text,
    }
    .trim();
    (!answer.is_empty()).then(|| answer.to_string())
}

pub fn lesson_prompt(
    track: &Track,
    phase: &Phase,
    node: &Node,
    exercise: &Exercise,
    answer: &str,
) -> String {
    format!(
        r#"You are a senior {domain} mentor reviewing a learner's written answer in the Learning Dojo, a self-study course.
Grade the answer only against the criteria below. Be specific and kind. Do not write a model answer before giving your feedback.

## Context
- Track: {track} · Phase {phase_id} ({phase_title}: {phase_goal})
- Lesson: {lesson}
- Objectives:
{objectives}

## Exercise
{task}

## Criteria
{criteria}

## The learner's answer
{answer}

## How to reply
1. For each criterion: ✅ met, 🟡 partly or ❌ missing, with one sentence that quotes or points to the answer.
2. Mistakes or misconceptions, if any, and why they are wrong.
3. The single improvement that matters most.
4. A score from 0 to 10, and whether the answer is ready (7 or more).
Reply in the language the answer is written in, in under 300 words.
"#,
        domain = track.name,
        track = track.name,
        phase_id = phase.id.to_uppercase(),
        phase_title = phase.title,
        phase_goal = phase.goal,
        lesson = node.title,
        objectives = objectives(node),
        task = exercise.task.trim(),
        criteria = numbered_list(&exercise.criteria),
        answer = answer.trim(),
    )
}

/// What the learner writes in a Docker kata, in the order a reviewer reads it.
const DOCKER_FILES: [&str; 5] = [
    "Dockerfile",
    ".dockerignore",
    "compose.yaml",
    "compose.yml",
    "docker-compose.yml",
];

/// The learner's source files in a kata folder, relative to it: the files to attach.
pub fn kata_files(kata_dir: &Path, language: KataLanguage) -> Vec<PathBuf> {
    let (root, extension) = match language {
        KataLanguage::Rust => (kata_dir.join("src"), "rs"),
        KataLanguage::Java => (kata_dir.join("src").join("main"), "java"),
        KataLanguage::Bash => (kata_dir.to_path_buf(), "sh"),
        KataLanguage::Docker => {
            return DOCKER_FILES
                .iter()
                .map(PathBuf::from)
                .filter(|file| kata_dir.join(file).is_file())
                .collect();
        }
        KataLanguage::Sql => (kata_dir.to_path_buf(), "sql"),
        KataLanguage::Python => (kata_dir.to_path_buf(), "py"),
        KataLanguage::Kubernetes => (kata_dir.to_path_buf(), "yaml"),
        KataLanguage::Go => (kata_dir.to_path_buf(), "go"),
        KataLanguage::TypeScript => (kata_dir.to_path_buf(), "ts"),
        KataLanguage::Web => {
            let mut found = Vec::new();
            for extension in ["html", "css", "js"] {
                collect(kata_dir, extension, &mut found);
            }
            return learner_files(kata_dir, found);
        }
    };
    let mut found = Vec::new();
    collect(&root, extension, &mut found);
    learner_files(kata_dir, found)
}

/// `found` relative to the kata folder, without tests, fixtures and tool folders, sorted.
fn learner_files(kata_dir: &Path, found: Vec<PathBuf>) -> Vec<PathBuf> {
    let mut files: Vec<PathBuf> = found
        .into_iter()
        .filter_map(|path| path.strip_prefix(kata_dir).ok().map(Path::to_path_buf))
        .filter(|path| !path.starts_with("tests") && !path.starts_with("fixture"))
        .filter(|path| !in_tool_folder(path))
        .filter(|path| !path.to_string_lossy().ends_with("_test.go"))
        .collect();
    files.sort();
    files
}

/// Virtual environments, caches and packages (`.venv`, `__pycache__`, `node_modules`) are
/// not the learner's code.
fn in_tool_folder(path: &Path) -> bool {
    path.components().any(|part| {
        let part = part.as_os_str().to_string_lossy();
        part.starts_with('.') || part == "__pycache__" || part == "node_modules"
    })
}

fn collect(dir: &Path, extension: &str, found: &mut Vec<PathBuf>) {
    let Ok(entries) = std::fs::read_dir(dir) else {
        return;
    };
    for path in entries.flatten().map(|entry| entry.path()) {
        if path.is_dir() {
            collect(&path, extension, found);
        } else if path.extension().is_some_and(|ext| ext == extension) {
            found.push(path);
        }
    }
}

pub struct KataContext<'a> {
    pub track: &'a Track,
    pub phase: &'a Phase,
    pub node: &'a Node,
    pub statement: &'a str,
    pub language: KataLanguage,
    pub kata_dir: &'a Path,
    pub files: &'a [PathBuf],
    /// "9/9 tests passed", or what the last check said.
    pub last_check: Option<String>,
}

pub fn kata_prompt(kata: &KataContext) -> String {
    let language = match kata.language {
        KataLanguage::Rust => "Rust",
        KataLanguage::Java => "Java",
        KataLanguage::Bash => "Bash and Linux",
        KataLanguage::Docker => "Dockerfile and Docker Compose",
        KataLanguage::Sql => "SQL (PostgreSQL 17)",
        KataLanguage::Python => "Python",
        KataLanguage::Kubernetes => "Kubernetes manifests, Kustomize and Helm",
        KataLanguage::Go => "Go",
        KataLanguage::TypeScript => "TypeScript",
        KataLanguage::Web => "HTML, CSS and JavaScript in the browser",
    };
    let files = kata
        .files
        .iter()
        .map(|file| format!("- `{}`", file.display().to_string().replace('\\', "/")))
        .collect::<Vec<_>>()
        .join("\n");
    format!(
        r#"You are a senior {language} engineer reviewing a learner's solution to a kata in the Learning Dojo, a self-study course.
Automated tests already check the behaviour. Review the quality of the code, as in a careful code review.

## Context
- Track: {track} · Phase {phase_id} ({phase_title}: {phase_goal})
- Kata: {kata}
- Objectives:
{objectives}
- Last automated check: {last_check}

## The challenge
{statement}

## The code
I am attaching these files from the kata folder (`{dir}`):
{files}
If the files are not attached, ask me for them before reviewing.

## Review criteria
1. Correctness beyond the tests: edge cases, error handling, input validation, concurrency where it applies.
2. Readability: names, function size, clear control flow, comments only where they add something.
3. Idiomatic {language} for this phase. Do not demand features or patterns from later phases.
4. Design: responsibilities, duplication, simplicity; nothing speculative.
5. The kata's objectives above: is each one met?

## How to reply
- Start with what is done well (two or three points).
- Then the issues, each with `file:line`, the quoted code, why it matters, and its weight: must fix, should fix or nice to have.
- Never paste a full solution; a short snippet to illustrate one point is fine.
- End with the three changes that matter most and a score from 0 to 10.
Reply in the language I write to you in, in under 500 words.
"#,
        track = kata.track.name,
        phase_id = kata.phase.id.to_uppercase(),
        phase_title = kata.phase.title,
        phase_goal = kata.phase.goal,
        kata = kata.node.title,
        objectives = objectives(kata.node),
        last_check = kata.last_check.as_deref().unwrap_or("not run yet"),
        statement = kata.statement.trim(),
        dir = kata.kata_dir.display(),
    )
}

fn objectives(node: &Node) -> String {
    node.objectives
        .iter()
        .map(|objective| format!("  - {}", objective.text))
        .collect::<Vec<_>>()
        .join("\n")
}

fn numbered_list(items: &[String]) -> String {
    items
        .iter()
        .enumerate()
        .map(|(index, item)| format!("{}. {item}", index + 1))
        .collect::<Vec<_>>()
        .join("\n")
}

fn bullet_list(items: &[String]) -> String {
    items
        .iter()
        .map(|item| format!("- {item}"))
        .collect::<Vec<_>>()
        .join("\n")
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn the_answer_is_what_follows_the_marker() {
        assert_eq!(
            answer_from("# Title\n\ntask\n\n<!-- Write your answer below this line. -->\n\n"),
            None
        );
        assert_eq!(
            answer_from(
                "# T\n<!-- Write your answer below this line. -->\n\nRecords are values.\n"
            )
            .as_deref(),
            Some("Records are values.")
        );
        assert_eq!(
            answer_from("No marker at all").as_deref(),
            Some("No marker at all")
        );
    }

    #[test]
    fn kata_files_lists_the_learner_sources_only() {
        let dir = tempfile::TempDir::new().unwrap();
        let main = dir.path().join("src/main/java/dojo");
        let test = dir.path().join("src/test/java/dojo");
        std::fs::create_dir_all(&main).unwrap();
        std::fs::create_dir_all(&test).unwrap();
        std::fs::write(main.join("Greeter.java"), "class Greeter {}").unwrap();
        std::fs::write(main.join("notes.txt"), "x").unwrap();
        std::fs::write(test.join("GreeterTest.java"), "class GreeterTest {}").unwrap();
        let files = kata_files(dir.path(), KataLanguage::Java);
        assert_eq!(
            files,
            vec![PathBuf::from("src/main/java/dojo/Greeter.java")]
        );
    }

    #[test]
    fn kata_files_lists_bash_scripts_but_not_their_tests() {
        let dir = tempfile::TempDir::new().unwrap();
        std::fs::create_dir_all(dir.path().join("tests")).unwrap();
        std::fs::write(dir.path().join("worker.sh"), "#!/usr/bin/env bash").unwrap();
        std::fs::write(dir.path().join("sample.log"), "x").unwrap();
        std::fs::write(dir.path().join("tests/helper.sh"), "x").unwrap();
        std::fs::write(dir.path().join("tests/visible.bats"), "x").unwrap();
        let files = kata_files(dir.path(), KataLanguage::Bash);
        assert_eq!(files, vec![PathBuf::from("worker.sh")]);
    }

    #[test]
    fn kata_files_lists_the_dockerfile_and_compose_file() {
        let dir = tempfile::TempDir::new().unwrap();
        std::fs::create_dir_all(dir.path().join("tests")).unwrap();
        std::fs::write(dir.path().join("Dockerfile"), "FROM alpine").unwrap();
        std::fs::write(dir.path().join(".dockerignore"), ".git").unwrap();
        std::fs::write(dir.path().join("compose.yaml"), "services: {}").unwrap();
        std::fs::write(dir.path().join("main.go"), "package main").unwrap();
        std::fs::write(dir.path().join("tests/visible.bats"), "x").unwrap();
        let files = kata_files(dir.path(), KataLanguage::Docker);
        assert_eq!(
            files,
            ["Dockerfile", ".dockerignore", "compose.yaml"].map(PathBuf::from)
        );
    }

    #[test]
    fn kata_files_lists_sql_queries_but_not_the_fixture_or_tests() {
        let dir = tempfile::TempDir::new().unwrap();
        std::fs::create_dir_all(dir.path().join("tests")).unwrap();
        std::fs::create_dir_all(dir.path().join("fixture")).unwrap();
        std::fs::write(dir.path().join("top_customers.sql"), "SELECT 1").unwrap();
        std::fs::write(dir.path().join("fixture/01_shop.sql"), "CREATE TABLE t ()").unwrap();
        std::fs::write(dir.path().join("tests/visible.sql"), "SELECT 1").unwrap();
        let files = kata_files(dir.path(), KataLanguage::Sql);
        assert_eq!(files, vec![PathBuf::from("top_customers.sql")]);
    }

    #[test]
    fn kata_files_lists_kubernetes_manifests_and_overlays_but_not_tests() {
        let dir = tempfile::TempDir::new().unwrap();
        for folder in ["tests", "base", "overlays/prod"] {
            std::fs::create_dir_all(dir.path().join(folder)).unwrap();
        }
        std::fs::write(dir.path().join("base/deployment.yaml"), "kind: Deployment").unwrap();
        std::fs::write(dir.path().join("overlays/prod/kustomization.yaml"), "x").unwrap();
        std::fs::write(dir.path().join("tests/render.txt"), "x").unwrap();
        std::fs::write(dir.path().join("tests/fixture.yaml"), "x").unwrap();
        let files = kata_files(dir.path(), KataLanguage::Kubernetes);
        assert_eq!(
            files,
            ["base/deployment.yaml", "overlays/prod/kustomization.yaml"].map(PathBuf::from)
        );
    }

    #[test]
    fn kata_files_lists_python_modules_but_not_tests_or_the_virtual_environment() {
        let dir = tempfile::TempDir::new().unwrap();
        for folder in ["tests", ".venv/lib", "__pycache__"] {
            std::fs::create_dir_all(dir.path().join(folder)).unwrap();
        }
        std::fs::write(dir.path().join("cart.py"), "x = 1").unwrap();
        std::fs::write(dir.path().join("pyproject.toml"), "x").unwrap();
        std::fs::write(dir.path().join("tests/test_visible.py"), "x").unwrap();
        std::fs::write(dir.path().join(".venv/lib/site.py"), "x").unwrap();
        std::fs::write(dir.path().join("__pycache__/cart.py"), "x").unwrap();
        let files = kata_files(dir.path(), KataLanguage::Python);
        assert_eq!(files, vec![PathBuf::from("cart.py")]);
    }

    #[test]
    fn kata_files_lists_go_sources_but_not_test_files() {
        let dir = tempfile::TempDir::new().unwrap();
        std::fs::write(dir.path().join("ledger.go"), "package ledger").unwrap();
        std::fs::write(dir.path().join("ledger_test.go"), "package ledger_test").unwrap();
        std::fs::write(dir.path().join("go.mod"), "module dojo/ledger").unwrap();
        let files = kata_files(dir.path(), KataLanguage::Go);
        assert_eq!(files, vec![PathBuf::from("ledger.go")]);
    }

    #[test]
    fn kata_files_lists_typescript_modules_but_not_tests_or_node_modules() {
        let dir = tempfile::TempDir::new().unwrap();
        for folder in ["tests", "node_modules/zod"] {
            std::fs::create_dir_all(dir.path().join(folder)).unwrap();
        }
        std::fs::write(dir.path().join("shapes.ts"), "export {}").unwrap();
        std::fs::write(dir.path().join("tsconfig.json"), "{}").unwrap();
        std::fs::write(dir.path().join("tests/visible.test.ts"), "x").unwrap();
        std::fs::write(dir.path().join("node_modules/zod/index.d.ts"), "x").unwrap();
        let files = kata_files(dir.path(), KataLanguage::TypeScript);
        assert_eq!(files, vec![PathBuf::from("shapes.ts")]);
    }

    #[test]
    fn kata_files_lists_pages_styles_and_scripts_but_not_tests() {
        let dir = tempfile::TempDir::new().unwrap();
        for folder in ["tests", "node_modules/x", "js"] {
            std::fs::create_dir_all(dir.path().join(folder)).unwrap();
        }
        std::fs::write(dir.path().join("index.html"), "<!doctype html>").unwrap();
        std::fs::write(dir.path().join("style.css"), "body {}").unwrap();
        std::fs::write(dir.path().join("js/app.js"), "export {}").unwrap();
        std::fs::write(dir.path().join("package.json"), "{}").unwrap();
        std::fs::write(dir.path().join("tests/visible.test.js"), "x").unwrap();
        std::fs::write(dir.path().join("node_modules/x/index.js"), "x").unwrap();
        let files = kata_files(dir.path(), KataLanguage::Web);
        assert_eq!(
            files,
            vec![
                PathBuf::from("index.html"),
                PathBuf::from("js").join("app.js"),
                PathBuf::from("style.css"),
            ]
        );
    }
}
