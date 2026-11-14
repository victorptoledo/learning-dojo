mod models;
mod state;
mod ui;

use anyhow::Result;
use crossterm::{
    event::{self, Event, KeyCode, KeyEventKind},
    execute,
    terminal::{disable_raw_mode, enable_raw_mode, EnterAlternateScreen, LeaveAlternateScreen},
};
use ratatui::prelude::*;
use state::{AppMode, AppState};
use std::io::stdout;
use std::time::Duration;

fn main() -> Result<()> {
    // 1. Inicializa o estado com todas as trilhas da pasta data/tracks
    let tracks_dir = "data/tracks";
    let profile_path = "data/profile.json";
    let mut app_state = AppState::new(tracks_dir, profile_path)?;

    // 2. Configura o terminal para modo Raw e Alternate Screen
    enable_raw_mode()?;
    let mut stdout = stdout();
    execute!(stdout, EnterAlternateScreen)?;
    let backend = CrosstermBackend::new(stdout);
    let mut terminal = Terminal::new(backend)?;

    // 3. Loop principal de eventos do Dojo
    let res = run_app(&mut terminal, &mut app_state);

    // 4. Garante a restauração do terminal ao sair
    disable_raw_mode()?;
    execute!(terminal.backend_mut(), LeaveAlternateScreen)?;
    terminal.show_cursor()?;

    if let Err(err) = res {
        eprintln!("Erro na execução do Learning Dojo: {:#}", err);
    } else {
        println!("🥋 Bom treino no Learning Dojo! Até a próxima sessão no tatame.");
    }

    Ok(())
}

fn run_app(terminal: &mut Terminal<CrosstermBackend<std::io::Stdout>>, state: &mut AppState) -> Result<()> {
    loop {
        terminal.draw(|f| ui::render(f, state))?;

        if event::poll(Duration::from_millis(100))? {
            if let Event::Key(key) = event::read()? {
                if key.kind == KeyEventKind::Press {
                    match state.mode {
                        AppMode::TrackSelector => match key.code {
                            KeyCode::Esc | KeyCode::Char('q') | KeyCode::Char('m') | KeyCode::Char('M') => {
                                state.close_track_selector();
                            }
                            KeyCode::Down | KeyCode::Char('j') => {
                                state.next_track_in_selector();
                            }
                            KeyCode::Up | KeyCode::Char('k') => {
                                state.prev_track_in_selector();
                            }
                            KeyCode::Enter | KeyCode::Char(' ') => {
                                state.confirm_track_selection();
                            }
                            _ => {}
                        },
                        AppMode::TrackView => match key.code {
                            KeyCode::Char('q') | KeyCode::Esc => {
                                break;
                            }
                            KeyCode::Char('m') | KeyCode::Char('M') | KeyCode::Char('t') | KeyCode::Char('T') => {
                                state.open_track_selector();
                            }
                            KeyCode::Down | KeyCode::Char('j') => {
                                state.next_node();
                                state.status_message = None;
                            }
                            KeyCode::Up | KeyCode::Char('k') => {
                                state.prev_node();
                                state.status_message = None;
                            }
                            KeyCode::Enter | KeyCode::Char(' ') => {
                                state.toggle_selected();
                            }
                            _ => {}
                        },
                    }
                }
            }
        }
    }
    Ok(())
}
