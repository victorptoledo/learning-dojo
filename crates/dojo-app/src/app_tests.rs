//! End-to-end tests of the app with the real content, an in-memory store and a fake runner.

use crate::app::{App, Services};
use crate::ui::render;
use crate::workspace::Workspace;
use dojo_core::{
    CheckOutcome, CheckReport, Kata, KataRunner, Mode, ProgressStore, RunnerError, TestCase,
};
use dojo_store::{SqliteStore, import_v1_profile};
use ratatui::{Terminal, backend::TestBackend};
use std::path::Path;
use std::process::Command;
use std::sync::{Arc, Mutex};
use tempfile::TempDir;

/// Runner double: returns queued outcomes instead of starting Docker.
struct FakeRunner {
    outcomes: Mutex<Vec<CheckOutcome>>,
}

impl FakeRunner {
    fn returning(outcomes: &[CheckOutcome]) -> Arc<Self> {
        Arc::new(Self {
            outcomes: Mutex::new(outcomes.iter().rev().copied().collect()),
        })
    }
}

impl KataRunner for FakeRunner {
    fn check(&self, _kata: &Kata, _solution_dir: &Path) -> Result<CheckReport, RunnerError> {
        let outcome = self
            .outcomes
            .lock()
            .unwrap()
            .pop()
            .ok_or("no outcome queued")?;
        let passed = outcome == CheckOutcome::Passed;
        Ok(CheckReport {
            outcome,
            tests: vec![
                TestCase {
                    name: "a".into(),
                    passed: true,
                },
                TestCase {
                    name: "b".into(),
                    passed,
                },
            ],
            details: if passed {
                String::new()
            } else {
                "---- b stdout ----\nleft: 1 right: 2".into()
            },
            duration_ms: 5,
        })
    }
}

struct Harness {
    app: App,
    workspace: TempDir,
}

fn harness(store: SqliteStore, mode: Mode, runner: Arc<FakeRunner>) -> Harness {
    let workspace = TempDir::new().unwrap();
    let services = Services {
        store: Box::new(store),
        runner,
        workspace: Workspace::new(workspace.path()),
        editor: "true".into(),
    };
    Harness {
        app: App::new(real_catalog(), services, mode).unwrap(),
        workspace,
    }
}

fn real_catalog() -> dojo_core::Catalog {
    let content = Path::new(env!("CARGO_MANIFEST_DIR")).join("../../content");
    dojo_content::load(&content).unwrap().catalog
}

fn memory_store() -> SqliteStore {
    SqliteStore::open_in_memory().unwrap()
}

fn screen_text(app: &App) -> String {
    let mut terminal = Terminal::new(TestBackend::new(160, 40)).unwrap();
    terminal.draw(|frame| render(frame, app)).unwrap();
    terminal
        .backend()
        .buffer()
        .content()
        .iter()
        .map(|cell| cell.symbol())
        .collect()
}

fn selected_ids(app: &App) -> (String, String) {
    let track = app.active_track();
    let (_, node) = app.tree.selected(track).unwrap();
    (track.id.clone(), node.id.clone())
}

/// Opens the Rust track with the cursor on `node_id`.
fn select(app: &mut App, node_id: &str) {
    app.selector_cursor = app.catalog.track_index("rust").unwrap();
    app.confirm_selection();
    for _ in 0..200 {
        if selected_ids(app).1 == node_id {
            return;
        }
        app.move_down();
    }
    panic!("{node_id} not found");
}

fn run_check(app: &mut App) {
    app.check_selected();
    let finished = app.checks.wait().expect("a check was started");
    app.finish_check(finished);
}

fn git(dir: &Path, args: &[&str]) -> String {
    let output = Command::new("git")
        .arg("-C")
        .arg(dir)
        .args(args)
        .output()
        .unwrap();
    assert!(
        output.status.success(),
        "{}",
        String::from_utf8_lossy(&output.stderr)
    );
    String::from_utf8_lossy(&output.stdout).trim().to_string()
}

fn init_repo(dir: &Path) {
    git(dir, &["init", "--quiet", "-b", "main"]);
    git(dir, &["config", "user.name", "Test"]);
    git(dir, &["config", "user.email", "test@example.com"]);
}

#[test]
fn renders_real_content_after_importing_a_v1_profile() {
    let dir = TempDir::new().unwrap();
    let v1_path = dir.path().join("profile.json");
    std::fs::write(
        &v1_path,
        r#"{"name":"Dev Sensei","total_xp":100,"current_streak":1,"best_streak":1,
           "last_trained":"2026-11-14","active_track_id":"rust",
           "track_progress":{"rust":["rust-01-setup"]}}"#,
    )
    .unwrap();
    let mut store = memory_store();
    import_v1_profile(&v1_path, &real_catalog(), &mut store, App::today()).unwrap();

    let mut h = harness(store, Mode::Learner, FakeRunner::returning(&[]));

    assert!(h.app.profile.is_completed("rust", "rust.p0.setup"));
    let screen = screen_text(&h.app);
    assert!(screen.contains("Rust"), "{screen}");
    assert!(screen.contains("First Steps"), "{screen}");
    assert!(screen.contains("Rosetta Bridge"), "{screen}");

    h.app.open_selector();
    assert!(screen_text(&h.app).contains("29 available"));
}

#[test]
fn learner_completes_a_lesson_but_cannot_undo_it() {
    let mut h = harness(memory_store(), Mode::Learner, FakeRunner::returning(&[]));
    let (track_id, node_id) = selected_ids(&h.app);

    h.app.toggle_selected();
    assert!(h.app.profile.is_completed(&track_id, &node_id));
    h.app.toggle_selected();

    assert!(h.app.profile.is_completed(&track_id, &node_id));
    assert!(h.app.status_message.as_deref().unwrap().contains("--dev"));
}

#[test]
fn dev_mode_can_undo_and_shows_its_badge() {
    let mut h = harness(memory_store(), Mode::Dev, FakeRunner::returning(&[]));
    let (track_id, node_id) = selected_ids(&h.app);

    h.app.toggle_selected();
    h.app.toggle_selected();

    assert!(!h.app.profile.is_completed(&track_id, &node_id));
    assert!(screen_text(&h.app).contains("DEV MODE"));
}

#[test]
fn first_run_saves_settings_so_the_store_is_no_longer_empty() {
    let dir = TempDir::new().unwrap();
    let db = dir.path().join("dojo.db");
    harness(
        SqliteStore::open(&db).unwrap(),
        Mode::Learner,
        FakeRunner::returning(&[]),
    );

    assert!(
        SqliteStore::open(&db)
            .unwrap()
            .load_settings()
            .unwrap()
            .is_some()
    );
}

#[test]
fn a_kata_must_be_started_before_checking() {
    let mut h = harness(memory_store(), Mode::Learner, FakeRunner::returning(&[]));
    select(&mut h.app, "rust.p0.hello-dojo");

    h.app.check_selected();

    assert!(
        h.app
            .status_message
            .as_deref()
            .unwrap()
            .contains("start this kata first")
    );
    assert!(screen_text(&h.app).contains("Not started yet"));
}

#[test]
fn learner_cannot_tick_a_kata() {
    let mut h = harness(memory_store(), Mode::Learner, FakeRunner::returning(&[]));
    select(&mut h.app, "rust.p0.setup");
    h.app.toggle_selected();
    select(&mut h.app, "rust.p0.hello-dojo");

    h.app.toggle_selected();

    assert!(!h.app.profile.is_completed("rust", "rust.p0.hello-dojo"));
    assert!(
        h.app
            .status_message
            .as_deref()
            .unwrap()
            .contains("passing its checks")
    );
}

#[test]
fn failing_then_passing_check_completes_the_kata_and_commits_it() {
    let mut h = harness(
        memory_store(),
        Mode::Learner,
        FakeRunner::returning(&[CheckOutcome::TestsFailed, CheckOutcome::Passed]),
    );
    init_repo(h.workspace.path());
    select(&mut h.app, "rust.p0.setup");
    h.app.toggle_selected();
    select(&mut h.app, "rust.p0.hello-dojo");

    h.app.activate_selected();
    let kata_dir = h.workspace.path().join("rust/p0/hello-dojo");
    assert!(kata_dir.join("src/lib.rs").is_file(), "starter copied");

    run_check(&mut h.app);
    assert!(!h.app.profile.is_completed("rust", "rust.p0.hello-dojo"));
    let screen = screen_text(&h.app);
    assert!(
        screen.contains("left: 1 right: 2"),
        "failure details shown: {screen}"
    );
    assert!(screen.contains("1 attempt"), "{screen}");

    run_check(&mut h.app);
    assert!(h.app.profile.is_completed("rust", "rust.p0.hello-dojo"));
    assert!(h.app.status_message.as_deref().unwrap().contains("+100 XP"));
    assert_eq!(
        git(h.workspace.path(), &["log", "-1", "--format=%s"]),
        "rust/p0/hello-dojo: passed (2/2 tests passed)"
    );
    let key = ("rust".to_string(), "rust.p0.hello-dojo".to_string());
    assert_eq!(h.app.check_history[&key].1, 2);
}

#[test]
fn saving_a_file_in_a_kata_folder_starts_its_check() {
    let mut h = harness(
        memory_store(),
        Mode::Learner,
        FakeRunner::returning(&[CheckOutcome::TestsFailed]),
    );
    let changed = h
        .workspace
        .path()
        .join("rust/p1/control-flow-workout/src/lib.rs");

    h.app.on_workspace_change(&changed);

    assert!(h.app.is_checking("rust", "rust.p1.control-flow-workout"));
    let finished = h.app.checks.wait().unwrap();
    assert_eq!(finished.target.node_id, "rust.p1.control-flow-workout");
}
