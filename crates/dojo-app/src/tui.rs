use crate::app::{App, Screen, Services};
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
    let services = Services {
        store: Box::new(store),
        runner: Arc::new(DockerRunner::default()),
        workspace: Workspace::new(&options.workspace),
        editor: options.editor.clone(),
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
        Screen::TrackView => return handle_track_key(app, key.code),
    }
    Flow::Continue
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
        KeyCode::Esc | KeyCode::Char('q') => return Flow::Quit,
        KeyCode::Char('m' | 'M' | 't' | 'T') => app.open_selector(),
        KeyCode::Down | KeyCode::Char('j') => app.move_down(),
        KeyCode::Up | KeyCode::Char('k') => app.move_up(),
        KeyCode::Enter => app.activate_selected(),
        KeyCode::Char(' ') => app.toggle_selected(),
        KeyCode::Char('c' | 'C') => app.check_selected(),
        KeyCode::Char('o' | 'O') => app.open_selected(),
        KeyCode::Char('b' | 'B') => app.cycle_bridge_language(),
        _ => {}
    }
    Flow::Continue
}
