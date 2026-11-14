use crate::app::{App, Screen, Services};
use crate::code_workspace;
use crate::editor::Editor;
use crate::environment::Environment;
use crate::setup::SetupJob;
use crate::ui;
use crate::watcher::WorkspaceWatcher;
use crate::workspace::Workspace;
use anyhow::{Result, anyhow};
use crossterm::event::{self, Event, KeyCode, KeyEvent, KeyEventKind};
use dojo_content::Severity;
use dojo_core::Mode;
use dojo_runner::DockerRunner;
use dojo_store::{ImportOutcome, SqliteStore, import_v1_profile};
use ratatui::DefaultTerminal;
use std::fs;
use std::path::PathBuf;
use std::sync::Arc;
use std::time::Duration;

const POLL_INTERVAL: Duration = Duration::from_millis(100);

pub struct Options {
    pub content_dir: PathBuf,
    pub db_path: PathBuf,
    pub v1_profile: PathBuf,
    pub workspace: PathBuf,
    pub editor: String,
    pub dev: bool,
}

pub fn run(options: &Options) -> Result<()> {
    let loaded = dojo_content::load(&options.content_dir)?;
    let mut store = SqliteStore::open(&options.db_path).map_err(|error| anyhow!(error))?;
    let import = import_v1_profile(
        &options.v1_profile,
        &loaded.catalog,
        &mut store,
        App::today(),
    )?;
    let mode = if options.dev {
        Mode::Dev
    } else {
        Mode::Learner
    };
    fs::create_dir_all(&options.workspace)?;
    let code_workspace = code_workspace::for_dojo(&options.content_dir, &options.workspace)?;
    let mut editor = Editor::new(&options.editor);
    let mut code_workspace_for_app = None;
    if editor.is_vscode_like() {
        editor = editor.with_project(code_workspace.write()?);
        code_workspace_for_app = Some(code_workspace);
    }
    let services = Services {
        store: Box::new(store),
        runner: Arc::new(DockerRunner::default()),
        workspace: Workspace::new(&options.workspace),
        environment: Some(Environment::new(
            editor.clone(),
            &std::path::absolute("runner-images")?,
        )),
        editor,
        code_workspace: code_workspace_for_app,
    };
    let mut app = App::new(loaded.catalog, services, mode)?;
    app.status_message = startup_message(loaded.report.count(Severity::Error), &import);
    let mut watcher = WorkspaceWatcher::start(&options.workspace)
        .map_err(|error| anyhow!("cannot watch {}: {error}", options.workspace.display()))?;

    let mut terminal = ratatui::init();
    let result = event_loop(&mut terminal, &mut app, &mut watcher);
    ratatui::restore();
    result?;
    println!("🥋 Good training at the Learning Dojo. See you on the mat!");
    Ok(())
}

fn startup_message(content_errors: usize, import: &ImportOutcome) -> Option<String> {
    if content_errors > 0 {
        return Some(format!(
            "⚠ Content has {content_errors} errors — run `dojo content validate`."
        ));
    }
    match import {
        ImportOutcome::NothingToImport => None,
        ImportOutcome::Imported {
            completed_nodes,
            unknown_nodes,
            backup,
        } => Some(format!(
            "Imported {completed_nodes} completed nodes from the v1 profile ({unknown_nodes} no longer exist); old file kept as {}.",
            backup.display()
        )),
    }
}

fn event_loop(
    terminal: &mut DefaultTerminal,
    app: &mut App,
    watcher: &mut WorkspaceWatcher,
) -> Result<()> {
    loop {
        app.poll_checks();
        app.poll_setup();
        if let Some(changed) = watcher.settled_change() {
            app.on_workspace_change(&changed);
        }
        terminal.draw(|frame| ui::render(frame, app))?;
        if !event::poll(POLL_INTERVAL)? {
            continue;
        }
        if let Event::Key(key) = event::read()?
            && key.kind == KeyEventKind::Press
            && handle_key(app, key) == Flow::Quit
        {
            return Ok(());
        }
    }
}

#[derive(PartialEq, Eq)]
enum Flow {
    Continue,
    Quit,
}

fn handle_key(app: &mut App, key: KeyEvent) -> Flow {
    match app.screen {
        Screen::TrackSelector => handle_selector_key(app, key.code),
        Screen::Quiz => handle_quiz_key(app, key.code),
        Screen::Setup => handle_setup_key(app, key.code),
        Screen::Placement => handle_placement_key(app, key.code),
        Screen::TrackView => return handle_track_key(app, key.code),
    }
    Flow::Continue
}

fn handle_placement_key(app: &mut App, code: KeyCode) {
    let typing = matches!(
        app.placement_question_kind(),
        Some(dojo_core::QuestionKind::Text { .. })
    );
    match code {
        KeyCode::Esc => app.cancel_placement(),
        KeyCode::Enter => app.placement_confirm(),
        KeyCode::Backspace => app.placement_backspace(),
        KeyCode::Char(character) if typing => app.placement_type(character),
        KeyCode::Char(digit @ '0'..='9') => app.placement_choose(digit as usize - '0' as usize),
        _ => {}
    }
}

fn handle_setup_key(app: &mut App, code: KeyCode) {
    match code {
        KeyCode::Esc => app.close_setup(),
        KeyCode::Enter => app.setup_continue(),
        KeyCode::Char('i' | 'I') => app.setup_run(SetupJob::Install),
        KeyCode::Char('u' | 'U') => app.setup_run(SetupJob::Update),
        KeyCode::Char('r' | 'R') => app.setup_run(SetupJob::Check),
        _ => {}
    }
}

fn handle_quiz_key(app: &mut App, code: KeyCode) {
    let finished = app
        .quiz
        .as_ref()
        .is_some_and(|session| session.score().is_some());
    if finished {
        if matches!(code, KeyCode::Enter | KeyCode::Esc) {
            app.close_finished_quiz();
        }
        return;
    }
    let Some(session) = app.quiz.as_mut() else {
        return;
    };
    match code {
        KeyCode::Esc => app.abandon_quiz(),
        KeyCode::Enter => session.confirm(),
        KeyCode::Backspace => session.backspace(),
        KeyCode::Char(character) if session.is_text_question() => session.type_char(character),
        KeyCode::Char(digit @ '1'..='9') => session.choose(digit as usize - '0' as usize),
        _ => {}
    }
}

fn handle_selector_key(app: &mut App, code: KeyCode) {
    match code {
        KeyCode::Esc | KeyCode::Char('q' | 'm' | 'M' | 't' | 'T') => app.close_selector(),
        KeyCode::Down | KeyCode::Char('j') => app.selector_down(),
        KeyCode::Up | KeyCode::Char('k') => app.selector_up(),
        KeyCode::Enter | KeyCode::Char(' ') => app.confirm_selection(),
        _ => {}
    }
}

fn handle_track_key(app: &mut App, code: KeyCode) -> Flow {
    match code {
        KeyCode::Esc => return Flow::Quit,
        KeyCode::Char('m' | 'M' | 't' | 'T') => app.open_selector(),
        KeyCode::Down | KeyCode::Char('j') => app.move_down(),
        KeyCode::Up | KeyCode::Char('k') => app.move_up(),
        KeyCode::Enter => app.activate_selected(),
        KeyCode::Char(' ') => app.toggle_selected(),
        KeyCode::Char('c' | 'C') => app.check_selected(),
        KeyCode::Char('q' | 'Q') => app.start_lesson_quiz(),
        KeyCode::Char('o' | 'O') => app.open_selected(),
        KeyCode::Char('b' | 'B') => app.cycle_bridge_language(),
        KeyCode::Char('p' | 'P') => app.start_placement(),
        KeyCode::Char('h' | 'H') => app.reveal_hint(),
        KeyCode::Char('r' | 'R') => app.start_review(),
        KeyCode::PageDown => app.scroll_detail_down(),
        KeyCode::PageUp => app.scroll_detail_up(),
        _ => {}
    }
    Flow::Continue
}
