use crate::app::{App, Mode};
use crate::profile_file::ProfileFile;
use crate::ui;
use anyhow::Result;
use crossterm::event::{self, Event, KeyCode, KeyEvent, KeyEventKind};
use dojo_content::Severity;
use ratatui::DefaultTerminal;
use std::path::Path;
use std::time::Duration;

const POLL_INTERVAL: Duration = Duration::from_millis(100);

pub fn run(content_dir: &Path, profile_path: &Path) -> Result<()> {
    let loaded = dojo_content::load(content_dir)?;
    let errors = loaded.report.count(Severity::Error);
    let mut app = App::new(loaded.catalog, ProfileFile::new(profile_path))?;
    if errors > 0 {
        app.status_message = Some(format!(
            "⚠ Content has {errors} errors — run `dojo content validate`."
        ));
    }

    let mut terminal = ratatui::init();
    let result = event_loop(&mut terminal, &mut app);
    ratatui::restore();
    result?;
    println!("🥋 Good training at the Learning Dojo. See you on the mat!");
    Ok(())
}

fn event_loop(terminal: &mut DefaultTerminal, app: &mut App) -> Result<()> {
    loop {
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
    match app.mode {
        Mode::TrackSelector => handle_selector_key(app, key.code),
        Mode::TrackView => return handle_track_key(app, key.code),
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
        KeyCode::Enter | KeyCode::Char(' ') => app.toggle_selected(),
        KeyCode::Char('b' | 'B') => app.cycle_bridge_language(),
        _ => {}
    }
    Flow::Continue
}
