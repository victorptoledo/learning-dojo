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
        clipboard: None,
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
    assert!(screen_text(&h.app).contains("30 available"));
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
    h.app.selector_cursor = h.app.catalog.track_index("nodejs").unwrap();
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

    // hello-dojo (P0) is not done yet: the quiz is practice and completes nothing.
    assert!(h.app.quiz.as_ref().unwrap().practice);
    take_quiz(&mut h.app, 0);
    assert!(
        !h.app
            .profile
            .is_completed("rust", "rust.p1.variables-mutability")
    );
    assert!(
        h.app
            .status_message
            .as_deref()
            .unwrap()
            .contains("Nothing was recorded")
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

/// Records every node of the Rust track before `phase` (and of `phase` except its
/// checkpoint) as completed, so a test can start at that checkpoint.
fn complete_up_to_checkpoint(app: &mut App, phase: &str) {
    let checkpoint = format!("rust.{phase}.checkpoint");
    let rust = app.catalog.track("rust").unwrap().clone();
    let last = rust
        .phases
        .iter()
        .position(|p| p.nodes.iter().any(|node| node.id == checkpoint))
        .unwrap();
    let events: Vec<_> = rust.phases[..=last]
        .iter()
        .flat_map(|p| p.nodes.iter())
        .filter(|node| node.id != checkpoint)
        .map(|node| dojo_core::ProgressEvent::NodeCompleted {
            track_id: "rust".into(),
            node_id: node.id.clone(),
            xp: node.xp,
            on: App::today(),
        })
        .collect();
    events.into_iter().for_each(|event| app.record(event));
}

#[test]
fn a_checkpoint_needs_both_its_quiz_and_its_kata() {
    let mut h = harness(
        memory_store(),
        Mode::Learner,
        FakeRunner::returning(&[CheckOutcome::Passed]),
    );
    complete_up_to_checkpoint(&mut h.app, "p1");
    select(&mut h.app, "rust.p1.checkpoint");
    let screen = screen_text(&h.app);
    assert!(
        screen.contains("pass the quiz [Q] and the integration kata"),
        "{screen}"
    );

    h.app.start_lesson_quiz();
    take_quiz(&mut h.app, 0);
    assert!(!h.app.profile.is_completed("rust", "rust.p1.checkpoint"));
    assert!(
        h.app
            .status_message
            .as_deref()
            .unwrap()
            .contains("integration kata"),
        "{:?}",
        h.app.status_message
    );

    h.app.activate_selected();
    run_check(&mut h.app);
    assert!(h.app.profile.is_completed("rust", "rust.p1.checkpoint"));
    let message = h.app.status_message.clone().unwrap();
    assert!(message.contains("+300 XP"), "{message}");
    assert!(
        message.contains("🧭 Next:"),
        "closing a phase points at the next step: {message}"
    );
}

#[test]
fn a_checkpoint_stays_locked_until_its_phase_is_done() {
    let mut h = harness(memory_store(), Mode::Learner, FakeRunner::returning(&[]));
    select(&mut h.app, "rust.p1.checkpoint");

    h.app.start_lesson_quiz();

    assert!(
        h.app.quiz.as_ref().unwrap().practice,
        "only practice before the phase is done"
    );
    take_quiz(&mut h.app, 0);
    assert!(!h.app.profile.is_completed("rust", "rust.p1.checkpoint"));
}

/// Answers the open placement test, right while `right(phase)` says so, until it ends.
fn answer_placement(app: &mut App, right: impl Fn(&str) -> bool) {
    use crate::placement_flow::PlacementStage;
    for _ in 0..100 {
        let Some(PlacementStage::Asking { session, .. }) = app.placement.as_ref().map(|f| &f.stage)
        else {
            return;
        };
        let item = session.current().unwrap().clone();
        match (item.question.kind, right(&item.phase)) {
            (QuestionKind::SingleChoice { answer, .. }, true) => app.placement_choose(answer + 1),
            (QuestionKind::SingleChoice { .. }, false) => app.placement_choose(0),
            (QuestionKind::Text { answers }, true) => {
                answers[0].chars().for_each(|c| app.placement_type(c));
                app.placement_confirm();
            }
            (QuestionKind::Text { .. }, false) => app.placement_confirm(),
        }
    }
    panic!("the placement test did not end");
}

#[test]
fn never_used_it_places_nothing_out() {
    let mut h = harness(memory_store(), Mode::Learner, FakeRunner::returning(&[]));
    select(&mut h.app, "rust.p0.setup");
    h.app.start_placement();
    assert!(screen_text(&h.app).contains("How familiar are you with Rust?"));

    h.app.placement_choose(1);
    h.app.placement_confirm();

    assert!(h.app.placement.is_none());
    assert!(!h.app.profile.is_placed_out("rust", "rust.p0.setup"));
    assert!(h.app.profile.can_take_placement(h.app.active_track()));
}

#[test]
fn a_strong_learner_skips_to_the_belt_exam_and_passing_it_confirms_every_skip() {
    let mut h = harness(
        memory_store(),
        Mode::Learner,
        FakeRunner::returning(&[CheckOutcome::Passed]),
    );
    select(&mut h.app, "rust.p0.setup");
    h.app.start_placement();
    h.app.placement_choose(2);
    answer_placement(&mut h.app, |phase| phase != "p4");
    let screen = screen_text(&h.app);
    assert!(screen.contains("Belt Exam"), "{screen}");
    h.app.placement_confirm();

    let profile = &h.app.profile;
    assert!(profile.is_placed_out("rust", "rust.p1.checkpoint"));
    assert!(profile.is_placed_out("rust", "rust.p3.lifetimes"));
    assert!(!profile.is_placed_out("rust", "rust.p4.async-tokio"));
    assert_eq!(profile.belt_exam("rust"), Some("rust.p2.checkpoint"));
    assert_eq!(profile.total_xp, 0);
    assert!(!profile.can_take_placement(h.app.active_track()));
    let track = h.app.active_track().clone();
    assert!(!profile.is_unlocked(&track, track.node("rust.p4.async-tokio").unwrap()));

    select(&mut h.app, "rust.p2.checkpoint");
    h.app.start_lesson_quiz();
    take_quiz(&mut h.app, 0);
    h.app.activate_selected();
    run_check(&mut h.app);

    let message = h.app.status_message.clone().unwrap();
    assert!(message.contains("Belt Exam passed"), "{message}");
    assert!(h.app.profile.is_completed("rust", "rust.p3.lifetimes"));
    assert!(!h.app.profile.is_placed_out("rust", "rust.p1.checkpoint"));
    let skipped_xp: u32 = track
        .nodes()
        .filter(|node| h.app.profile.is_completed("rust", &node.id))
        .map(|node| node.xp)
        .sum();
    assert_eq!(h.app.profile.total_xp, skipped_xp);
}

#[test]
fn a_failed_belt_exam_quiz_sends_the_missed_skills_back() {
    let mut h = harness(memory_store(), Mode::Learner, FakeRunner::returning(&[]));
    select(&mut h.app, "rust.p0.setup");
    h.app.start_placement();
    h.app.placement_choose(2);
    answer_placement(&mut h.app, |phase| phase == "p1");
    h.app.placement_confirm();
    assert_eq!(h.app.profile.belt_exam("rust"), Some("rust.p1.checkpoint"));

    select(&mut h.app, "rust.p1.checkpoint");
    h.app.start_lesson_quiz();
    take_quiz(&mut h.app, 8);

    let message = h.app.status_message.clone().unwrap();
    assert!(message.contains("Back to study"), "{message}");
    assert!(!h.app.profile.is_placed_out("rust", "rust.p1.collections"));
    let track = h.app.active_track().clone();
    assert!(
        !h.app
            .profile
            .is_unlocked(&track, track.node("rust.p1.checkpoint").unwrap())
    );
}

#[test]
fn esc_leaves_the_placement_test_without_recording_anything() {
    let mut h = harness(memory_store(), Mode::Learner, FakeRunner::returning(&[]));
    select(&mut h.app, "rust.p0.setup");
    h.app.start_placement();
    h.app.placement_choose(2);
    h.app.cancel_placement();

    assert!(h.app.placement.is_none());
    assert!(h.app.profile.can_take_placement(h.app.active_track()));
}

#[test]
fn hints_open_one_at_a_time_and_lower_the_kata_xp() {
    let mut h = harness(
        memory_store(),
        Mode::Learner,
        FakeRunner::returning(&[CheckOutcome::Passed]),
    );
    pass_lesson(&mut h.app, "rust.p0.setup");
    select(&mut h.app, "rust.p0.hello-dojo");
    h.app.status_message = None;
    assert!(screen_text(&h.app).contains("[H] Hint 1/3"));

    h.app.reveal_hint();
    assert!(
        h.app
            .status_message
            .as_deref()
            .unwrap()
            .contains("Hint 1 of 3 is open in the kata panel (the kata now gives 90 XP)")
    );
    h.app.reveal_hint();
    let screen = screen_text(&h.app);
    assert!(screen.contains("Hints: 2 of 3 open"), "{screen}");
    assert!(screen.contains("Trim first, then decide"), "{screen}");

    h.app.activate_selected();
    run_check(&mut h.app);
    assert!(h.app.profile.is_completed("rust", "rust.p0.hello-dojo"));
    assert_eq!(h.app.profile.earned_xp("rust", "rust.p0.hello-dojo"), 80);
    assert!(h.app.status_message.as_deref().unwrap().contains("+80 XP"));
}

#[test]
fn due_questions_are_reviewed_with_r_and_their_answers_recorded() {
    let mut store = memory_store();
    let three_days_ago = App::today() - chrono::Days::new(3);
    store
        .append_event(&dojo_core::ProgressEvent::NodeCompleted {
            track_id: "rust".into(),
            node_id: "rust.p0.setup".into(),
            xp: 100,
            on: three_days_ago,
        })
        .unwrap();
    let mut h = harness(store, Mode::Learner, FakeRunner::returning(&[]));
    select(&mut h.app, "rust.p0.setup");
    let track = h.app.active_track();
    let due = h.app.profile.due_reviews(track, App::today()).len();
    assert!(due > 0);
    h.app.status_message = None;
    assert!(screen_text(&h.app).contains(&format!("[R] Review ({due})")));

    h.app.start_review();
    assert_eq!(h.app.screen, Screen::Quiz);
    assert!(screen_text(&h.app).contains("Review: Rust"));
    take_quiz(&mut h.app, 1);

    assert_eq!(h.app.screen, Screen::TrackView);
    let message = h.app.status_message.clone().unwrap();
    assert!(
        message.contains(&format!("{}/{due} right", due - 1)),
        "{message}"
    );
    assert_eq!(
        h.app.profile.practice_xp,
        (due as u32 - 1) * dojo_core::review::REVIEW_XP
    );
    assert_eq!(h.app.profile.total_xp, 100, "practice XP is kept apart");
    let track = h.app.active_track();
    assert!(
        h.app.profile.due_reviews(track, App::today()).is_empty(),
        "the missed one comes back tomorrow"
    );
    let reviews = h
        .app
        .store
        .load_events()
        .unwrap()
        .into_iter()
        .filter(|event| matches!(event, dojo_core::ProgressEvent::ReviewAnswered { .. }))
        .count();
    assert_eq!(reviews, due);

    h.app.start_review();
    assert!(
        h.app
            .status_message
            .as_deref()
            .unwrap()
            .starts_with("🔁 Nothing to review today. Next review:")
    );
}

#[test]
fn the_career_screen_sets_a_target_that_steers_the_next_steps() {
    let mut h = harness(memory_store(), Mode::Learner, FakeRunner::returning(&[]));
    select(&mut h.app, "rust.p0.setup");
    h.app.status_message = None;
    assert!(screen_text(&h.app).contains("[G] Career"));

    h.app.open_careers();
    assert_eq!(h.app.screen, Screen::Career);
    let rust_path = h
        .app
        .catalog
        .careers
        .iter()
        .position(|career| career.id == "rust-systems-engineer")
        .expect("the Rust career exists");
    while h.app.career_cursor != rust_path {
        h.app.career_down();
    }
    h.app.toggle_target_career();
    assert_eq!(
        h.app.profile.settings.target_career.as_deref(),
        Some("rust-systems-engineer")
    );
    let saved = h.app.store.load_settings().unwrap().unwrap();
    assert_eq!(
        saved.target_career.as_deref(),
        Some("rust-systems-engineer")
    );

    let screen = screen_text(&h.app);
    assert!(screen.contains("Career paths"), "{screen}");
    assert!(screen.contains("Rust up to P2"), "{screen}");
    assert!(screen.contains("Next steps"), "{screen}");
    let first = h.app.recommendations().into_iter().next().unwrap();
    assert_eq!(
        first.node_id, "rust.p0.setup",
        "the first open node of the needed track"
    );
    assert!(first.reasons.contains(&dojo_core::Reason::CareerGap));

    h.app.toggle_target_career();
    assert_eq!(
        h.app.profile.settings.target_career, None,
        "Enter again clears it"
    );
    h.app.close_careers();
    assert_eq!(h.app.screen, Screen::TrackView);
}

#[test]
fn a_on_a_lesson_first_opens_answer_md_then_saves_a_review_prompt_with_the_answer() {
    let mut h = harness(memory_store(), Mode::Learner, FakeRunner::returning(&[]));
    select(&mut h.app, "rust.p0.setup");

    h.app.ai_review_prompt();
    let answer_file = h.workspace.path().join("rust/p0/setup/ANSWER.md");
    let template = std::fs::read_to_string(&answer_file).expect("ANSWER.md created");
    assert!(template.contains(crate::review_prompt::ANSWER_MARKER));
    assert!(
        h.app
            .status_message
            .as_deref()
            .unwrap()
            .contains("ANSWER.md")
    );
    let prompt_file = h.workspace.path().join(".dojo/prompts/rust.p0.setup.md");
    assert!(
        !prompt_file.exists(),
        "no prompt before an answer is written"
    );

    std::fs::write(
        &answer_file,
        format!("{template}cargo build compiles the crate.\n"),
    )
    .unwrap();
    h.app.ai_review_prompt();

    let prompt = std::fs::read_to_string(&prompt_file).expect("prompt saved");
    assert!(prompt.contains("cargo build compiles the crate."));
    assert!(
        h.app
            .status_message
            .as_deref()
            .unwrap()
            .contains("Review prompt saved")
    );
    assert!(
        !h.app.profile.is_completed("rust", "rust.p0.setup"),
        "feedback only"
    );
}

#[test]
fn a_on_a_kata_lists_the_files_to_attach() {
    let mut h = harness(memory_store(), Mode::Learner, FakeRunner::returning(&[]));
    pass_lesson(&mut h.app, "rust.p0.setup");
    select(&mut h.app, "rust.p0.hello-dojo");

    h.app.ai_review_prompt();
    assert!(
        h.app
            .status_message
            .as_deref()
            .unwrap()
            .contains("Start the kata first")
    );

    h.app.activate_selected();
    h.app.ai_review_prompt();

    let status = h.app.status_message.clone().unwrap();
    assert!(status.contains("Attach: src/lib.rs"), "{status}");
    let prompt = std::fs::read_to_string(
        h.workspace
            .path()
            .join(".dojo/prompts/rust.p0.hello-dojo.md"),
    )
    .unwrap();
    assert!(prompt.contains("src/lib.rs"));
}

#[test]
fn b_opens_a_language_list_and_enter_switches_the_bridge() {
    let mut h = harness(memory_store(), Mode::Learner, FakeRunner::returning(&[]));
    select(&mut h.app, "rust.p1.ownership-basics");

    h.app.open_bridge_picker();
    assert_eq!(h.app.screen, Screen::BridgePicker);
    let screen = screen_text(&h.app);
    for name in ["Java", "Python", "JavaScript / TypeScript", "C#", "Go"] {
        assert!(screen.contains(name), "{name} is listed");
    }

    let languages = h.app.bridge_languages();
    h.app.bridge_cursor = languages.iter().position(|key| key == "python").unwrap();
    h.app.choose_bridge_language();

    assert_eq!(h.app.screen, Screen::TrackView);
    assert_eq!(h.app.profile.settings.bridge_from, "python");
    assert!(screen_text(&h.app).contains("Bridge: Python"));

    h.app.open_bridge_picker();
    h.app.bridge_picker_down();
    h.app.close_bridge_picker();
    assert_eq!(
        h.app.profile.settings.bridge_from, "python",
        "Esc changes nothing"
    );
}

#[test]
fn slash_finds_a_topic_in_any_track_and_opens_a_locked_lesson_as_practice() {
    let mut h = harness(memory_store(), Mode::Learner, FakeRunner::returning(&[]));
    select(&mut h.app, "rust.p0.setup");

    h.app.open_topic_study();
    assert_eq!(h.app.screen, Screen::TopicStudy);
    "systemd".chars().for_each(|c| h.app.topic_type(c));
    // Role tracks such as DevOps teach the same skill, so pick the Linux track's topic.
    let study = h.app.topic_study.as_mut().unwrap();
    let linux = study
        .matches()
        .iter()
        .position(|topic| topic.track_id == "linux")
        .expect("the Linux track teaches systemd");
    assert_eq!(study.matches()[linux].skill_id, "linux.systemd-services");
    study.cursor = linux;

    h.app.topic_enter();
    assert!(screen_text(&h.app).contains("study only"));
    h.app.topic_enter();

    assert_eq!(h.app.screen, Screen::TrackView);
    assert_eq!(h.app.active_track().id, "linux");
    assert_eq!(selected_ids(&h.app).1, "linux.p2.systemd-services");
    assert!(
        h.app
            .status_message
            .as_deref()
            .unwrap()
            .contains("Study mode")
    );

    h.app.start_lesson_quiz();
    let session = h
        .app
        .quiz
        .as_ref()
        .expect("a practice quiz opens in a locked phase");
    assert!(session.practice);
    take_quiz(&mut h.app, 0);
    assert!(
        h.app
            .status_message
            .as_deref()
            .unwrap()
            .contains("Nothing was recorded")
    );
    assert!(
        !h.app
            .profile
            .is_completed("linux", "linux.p2.systemd-services")
    );
}

#[test]
fn a_topic_practice_quiz_mixes_its_lessons_and_records_nothing() {
    let mut h = harness(memory_store(), Mode::Learner, FakeRunner::returning(&[]));
    h.app.open_topic_study();
    "permissions".chars().for_each(|c| h.app.topic_type(c));
    h.app.topic_enter();
    h.app.start_topic_quiz();

    assert_eq!(h.app.screen, Screen::Quiz);
    assert!(h.app.quiz.as_ref().unwrap().title.starts_with("Practice:"));
    take_quiz(&mut h.app, 1);
    assert!(
        h.app
            .status_message
            .as_deref()
            .unwrap()
            .starts_with("🎯 Practice")
    );
    let linux = h.app.catalog.track("linux").unwrap();
    assert_eq!(h.app.profile.track_xp(linux), 0);
}

#[test]
fn a_mock_interview_asks_reveals_rates_and_suggests_what_to_study() {
    let mut h = harness(memory_store(), Mode::Learner, FakeRunner::returning(&[]));
    select(&mut h.app, "rust.p0.setup");

    h.app.open_interview();
    assert_eq!(h.app.screen, Screen::Interview);
    assert!(screen_text(&h.app).contains("Which level"));
    h.app.interview_down(); // Senior
    h.app.interview_enter();
    let total = h.app.interview.as_ref().unwrap().questions.len();
    assert!(total >= 3);
    assert!(
        h.app
            .interview
            .as_ref()
            .unwrap()
            .questions
            .iter()
            .all(|q| q.level == dojo_core::InterviewLevel::Senior)
    );

    for index in 0..total {
        assert!(!screen_text(&h.app).contains("A strong answer covers"));
        h.app.interview_enter();
        assert!(screen_text(&h.app).contains("A strong answer covers"));
        h.app.interview_rate(if index == 0 {
            crate::interview_sim::Rating::Missed
        } else {
            crate::interview_sim::Rating::Covered
        });
    }
    let summary = screen_text(&h.app);
    assert!(summary.contains("Interview finished"));
    assert!(summary.contains("Study next"));

    h.app.interview_prompt();
    let prompt =
        std::fs::read_to_string(h.workspace.path().join(".dojo/prompts/interview-rust.md"))
            .unwrap();
    assert!(prompt.contains("A strong answer covers"));
    h.app.interview_enter();
    assert_eq!(h.app.screen, Screen::TrackView);
}

#[test]
fn written_interview_answers_become_an_evaluation_prompt() {
    let mut h = harness(memory_store(), Mode::Learner, FakeRunner::returning(&[]));
    select(&mut h.app, "rust.p0.setup");
    h.app.open_interview();
    h.app.interview_enter(); // Mid-level

    let file = h
        .app
        .interview
        .as_ref()
        .unwrap()
        .answers_file
        .clone()
        .expect("answers file");
    let template = std::fs::read_to_string(&file).unwrap();
    assert!(template.contains("## Question 1"));

    h.app.interview_evaluation_prompt();
    assert!(
        h.app
            .status_message
            .as_deref()
            .unwrap()
            .contains("Write your answers first")
    );

    let answered = template.replacen(
        "<!-- Answer 1 below this line -->",
        "<!-- Answer 1 below this line -->\nA trait object uses a vtable for dynamic dispatch.",
        1,
    );
    std::fs::write(&file, answered).unwrap();
    h.app.interview_enter();
    h.app.interview_rate(crate::interview_sim::Rating::Partly);
    h.app.interview_evaluation_prompt();

    let prompt = std::fs::read_to_string(
        h.workspace
            .path()
            .join(".dojo/prompts/interview-evaluation-rust.md"),
    )
    .unwrap();
    assert!(prompt.contains("A trait object uses a vtable for dynamic dispatch."));
    assert!(prompt.contains("I think I covered it partly"));
    assert!(prompt.contains("(no written answer)"));
    assert!(prompt.contains("Key points of a strong answer"));
}
