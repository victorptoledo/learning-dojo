//! End-to-end tests of the app with the real content, an in-memory store and a fake runner.

use crate::app::{App, Screen, Services};
use crate::editor::Editor;
use crate::environment::Environment;
use crate::ui::render;
use crate::workspace::Workspace;
use dojo_core::{
    CheckOutcome, CheckReport, Kata, KataRunner, Mode, ProgressStore, QuestionKind, RunnerError,
    TestCase,
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
                    message: None,
                },
                TestCase {
                    name: "b".into(),
                    passed,
                    message: (!passed).then(|| "left: 1 right: 2".to_string()),
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
    harness_with(store, mode, runner, None)
}

fn harness_with(
    store: SqliteStore,
    mode: Mode,
    runner: Arc<FakeRunner>,
    environment: Option<Environment>,
) -> Harness {
    let workspace = TempDir::new().unwrap();
    let services = Services {
        store: Box::new(store),
        runner,
        workspace: Workspace::new(workspace.path()),
        editor: Editor::new("no-editor-in-tests"),
        code_workspace: None,
        environment,
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

/// Answers every question of the open quiz, `wrong` of them incorrectly, and closes it.
fn take_quiz(app: &mut App, wrong: usize) {
    for index in 0.. {
        let session = app.quiz.as_mut().expect("a quiz is open");
        if session.score().is_some() {
            break;
        }
        let answer_wrong = index < wrong;
        match session.question().kind.clone() {
            QuestionKind::SingleChoice { options, answer } => {
                let pick = if answer_wrong {
                    (answer + 1) % options.len()
                } else {
                    answer
                };
                session.choose(pick + 1);
            }
            QuestionKind::Text { answers } => {
                let typed = if answer_wrong {
                    "definitely wrong".to_string()
                } else {
                    answers[0].clone()
                };
                typed.chars().for_each(|c| session.type_char(c));
                session.confirm();
            }
        }
        session.confirm();
    }
    app.close_finished_quiz();
}

/// Completes `node_id` (a Rust lesson) by passing its quiz.
fn pass_lesson(app: &mut App, node_id: &str) {
    select(app, node_id);
    app.start_lesson_quiz();
    take_quiz(app, 0);
    assert!(
        app.profile.is_completed("rust", node_id),
        "{node_id} completed"
    );
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
    assert!(screen.contains("Quiz:"), "{screen}");

    h.app.open_selector();
    assert!(screen_text(&h.app).contains("29 available"));
}

#[test]
fn learner_completes_a_lesson_by_its_quiz_and_cannot_undo_it() {
    let mut h = harness(memory_store(), Mode::Learner, FakeRunner::returning(&[]));
    select(&mut h.app, "rust.p0.setup");

    h.app.toggle_selected();
    assert!(
        !h.app.profile.is_completed("rust", "rust.p0.setup"),
        "a lesson cannot be ticked"
    );
    assert!(h.app.status_message.as_deref().unwrap().contains("quiz"));

    pass_lesson(&mut h.app, "rust.p0.setup");
    assert!(h.app.status_message.as_deref().unwrap().contains("+100 XP"));

    h.app.toggle_selected();
    assert!(h.app.profile.is_completed("rust", "rust.p0.setup"));
    assert!(h.app.status_message.as_deref().unwrap().contains("--dev"));
}

#[test]
fn a_failed_quiz_is_recorded_but_does_not_complete_the_lesson() {
    let mut h = harness(memory_store(), Mode::Learner, FakeRunner::returning(&[]));
    select(&mut h.app, "rust.p0.setup");
    h.app.start_lesson_quiz();
    assert!(screen_text(&h.app).contains("Question 1 of 5"));

    take_quiz(&mut h.app, 2);

    assert!(!h.app.profile.is_completed("rust", "rust.p0.setup"));
    let key = ("rust".to_string(), "rust.p0.setup".to_string());
    assert_eq!(h.app.quiz_history[&key].score.correct, 3);
    assert!(screen_text(&h.app).contains("Last try: 3/5"));
}

#[test]
fn leaving_a_quiz_with_esc_records_nothing() {
    let mut h = harness(memory_store(), Mode::Learner, FakeRunner::returning(&[]));
    select(&mut h.app, "rust.p0.setup");
    h.app.start_lesson_quiz();

    h.app.abandon_quiz();

    assert!(h.app.quiz.is_none());
    assert!(h.app.quiz_history.is_empty());
}

#[test]
fn a_lesson_without_document_and_quiz_is_under_construction() {
    let mut h = harness(memory_store(), Mode::Learner, FakeRunner::returning(&[]));
    h.app.selector_cursor = h.app.catalog.track_index("go").unwrap();
    h.app.confirm_selection();

    h.app.activate_selected();

    assert!(h.app.quiz.is_none());
    assert!(
        h.app
            .status_message
            .as_deref()
            .unwrap()
            .contains("Under construction")
    );
    assert!(screen_text(&h.app).contains("Under construction"));
}

#[test]
fn the_next_phase_stays_locked_until_the_current_one_is_finished() {
    let mut h = harness(memory_store(), Mode::Learner, FakeRunner::returning(&[]));
    pass_lesson(&mut h.app, "rust.p0.setup");
    select(&mut h.app, "rust.p1.variables-mutability");

    h.app.start_lesson_quiz();

    assert!(h.app.quiz.is_none(), "hello-dojo (P0) is not done yet");
    assert!(
        h.app
            .status_message
            .as_deref()
            .unwrap()
            .contains("prerequisites")
    );
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
    assert!(screen_text(&h.app).contains("How to start"));
}

#[test]
fn learner_cannot_tick_a_kata() {
    let mut h = harness(memory_store(), Mode::Learner, FakeRunner::returning(&[]));
    pass_lesson(&mut h.app, "rust.p0.setup");
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
    pass_lesson(&mut h.app, "rust.p0.setup");
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
fn creating_a_kata_folder_does_not_trigger_a_check() {
    let mut h = harness(memory_store(), Mode::Learner, FakeRunner::returning(&[]));
    select(&mut h.app, "rust.p0.hello-dojo");
    h.app.activate_selected();

    h.app
        .on_workspace_change(&h.workspace.path().join("rust/p0/hello-dojo/src/lib.rs"));

    assert!(!h.app.is_checking("rust", "rust.p0.hello-dojo"));
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

#[test]
fn enter_on_a_lesson_opens_its_document_and_on_a_kata_its_folder_with_the_statement() {
    let mut h = harness(memory_store(), Mode::Learner, FakeRunner::returning(&[]));
    select(&mut h.app, "rust.p0.setup");

    h.app.activate_selected();
    assert!(h.app.quiz.is_none(), "Enter no longer starts the quiz");
    assert!(
        h.app
            .status_message
            .as_deref()
            .unwrap()
            .contains("no-editor-in-tests")
    );

    select(&mut h.app, "rust.p0.hello-dojo");
    h.app.activate_selected();
    let statement =
        std::fs::read_to_string(h.workspace.path().join("rust/p0/hello-dojo/README.md")).unwrap();
    assert!(statement.contains("greet"));
    assert!(
        h.app
            .status_message
            .as_deref()
            .unwrap()
            .contains("Kata folder created")
    );
}

#[test]
fn q_on_a_kata_explains_that_only_lessons_have_quizzes() {
    let mut h = harness(memory_store(), Mode::Learner, FakeRunner::returning(&[]));
    pass_lesson(&mut h.app, "rust.p0.setup");
    select(&mut h.app, "rust.p0.hello-dojo");

    h.app.start_lesson_quiz();

    assert!(h.app.quiz.is_none());
    assert!(
        h.app
            .status_message
            .as_deref()
            .unwrap()
            .contains("Only lessons")
    );
}

fn tool_requirement(command: &str, required: bool) -> dojo_core::Requirement {
    dojo_core::Requirement {
        name: command.into(),
        why: "needed for this step".into(),
        required,
        kind: dojo_core::RequirementKind::Tool {
            command: command.into(),
            version_args: vec![],
            min_version: None,
            install_hint: format!("install {command}"),
            update: None,
        },
    }
}

/// The real catalog with the Rust track's requirements replaced by `requirements`.
fn harness_requiring(requirements: Vec<dojo_core::Requirement>) -> Harness {
    let environment = Environment::new(Editor::new("explorer"), Path::new("runner-images"));
    let mut h = harness_with(
        memory_store(),
        Mode::Learner,
        FakeRunner::returning(&[]),
        Some(environment),
    );
    let rust = h.app.catalog.track_index("rust").unwrap();
    h.app.catalog.tracks[rust].requirements = requirements;
    for phase in &mut h.app.catalog.tracks[rust].phases {
        for node in &mut phase.nodes {
            node.requirements.clear();
        }
    }
    h
}

fn wait_for_setup(app: &mut App) {
    for _ in 0..200 {
        app.poll_setup();
        if app
            .setup
            .as_ref()
            .is_none_or(|session| session.busy.is_none())
        {
            return;
        }
        std::thread::sleep(std::time::Duration::from_millis(20));
    }
    panic!("setup did not finish");
}

#[test]
fn a_missing_required_tool_stops_the_step_until_it_is_installed() {
    let mut h = harness_requiring(vec![tool_requirement("no-such-tool-xyz", true)]);
    select(&mut h.app, "rust.p0.setup");

    h.app.activate_selected();
    wait_for_setup(&mut h.app);

    assert_eq!(h.app.screen, Screen::Setup);
    let screen = screen_text(&h.app);
    assert!(screen.contains("Prepare"), "{screen}");
    assert!(screen.contains("install no-such-tool-xyz"), "{screen}");
    h.app.setup_continue();
    assert_eq!(
        h.app.screen,
        Screen::Setup,
        "Enter does nothing while something required is missing"
    );
}

#[test]
fn when_everything_is_installed_the_step_opens_by_itself_and_is_not_checked_again() {
    let mut h = harness_requiring(vec![tool_requirement("cargo", true)]);
    select(&mut h.app, "rust.p0.setup");

    h.app.activate_selected();
    wait_for_setup(&mut h.app);

    assert_eq!(h.app.screen, Screen::TrackView);
    assert!(h.app.status_message.as_deref().unwrap().contains("lesson"));
    h.app.activate_selected();
    assert!(h.app.setup.is_none(), "cleared for the rest of the session");
}

#[test]
fn a_missing_optional_tool_shows_the_setup_but_lets_the_learner_continue() {
    let mut h = harness_requiring(vec![
        tool_requirement("cargo", true),
        tool_requirement("no-such-tool-xyz", false),
    ]);
    select(&mut h.app, "rust.p0.setup");

    h.app.activate_selected();
    wait_for_setup(&mut h.app);
    assert_eq!(h.app.screen, Screen::Setup);

    h.app.setup_continue();

    assert_eq!(h.app.screen, Screen::TrackView);
}
