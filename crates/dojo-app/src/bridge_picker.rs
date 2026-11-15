//! B: a small list of the languages that bridges are written from. The learner picks the
//! one they know, and every lesson explains its ideas from there.

use crate::app::{App, Screen};
use crate::ui::centered;
use ratatui::Frame;
use ratatui::layout::Rect;
use ratatui::style::{Color, Modifier, Style, Stylize};
use ratatui::text::{Line, Span};
use ratatui::widgets::{Block, Borders, Clear, List, ListItem, ListState, Paragraph};
use std::collections::BTreeSet;

/// The name shown for a bridge language key (the `from` of `bridges/<key>.md`).
pub fn language_name(key: &str) -> &str {
    match key {
        "java" => "Java",
        "python" => "Python",
        "javascript" => "JavaScript / TypeScript",
        "csharp" => "C#",
        "go" => "Go",
        "generic" => "general programming",
        other => other,
    }
}

impl App {
    /// Every language some lesson has a bridge from, sorted by key.
    pub(crate) fn bridge_languages(&self) -> Vec<String> {
        self.catalog
            .tracks
            .iter()
            .flat_map(|track| track.nodes())
            .flat_map(|node| node.bridges.keys().cloned())
            .filter(|key| key != dojo_core::GENERIC_BRIDGE)
            .collect::<BTreeSet<_>>()
            .into_iter()
            .collect()
    }

    pub fn open_bridge_picker(&mut self) {
        let languages = self.bridge_languages();
        if languages.is_empty() {
            self.status_message = Some("No bridges available yet.".to_string());
            return;
        }
        self.bridge_cursor = languages
            .iter()
            .position(|key| *key == self.profile.settings.bridge_from)
            .unwrap_or(0);
        self.screen = Screen::BridgePicker;
    }

    pub fn bridge_picker_down(&mut self) {
        let count = self.bridge_languages().len();
        if count > 0 {
            self.bridge_cursor = (self.bridge_cursor + 1) % count;
        }
    }

    pub fn bridge_picker_up(&mut self) {
        let count = self.bridge_languages().len();
        if count > 0 {
            self.bridge_cursor = (self.bridge_cursor + count - 1) % count;
        }
    }

    pub fn close_bridge_picker(&mut self) {
        self.screen = Screen::TrackView;
    }

    /// Enter: explain bridges from the highlighted language from now on.
    pub fn choose_bridge_language(&mut self) {
        let languages = self.bridge_languages();
        let Some(key) = languages.get(self.bridge_cursor) else {
            self.close_bridge_picker();
            return;
        };
        self.profile.settings.bridge_from = key.clone();
        self.save_settings();
        self.status_message = Some(format!(
            "Bridges now explained from {}.",
            language_name(key)
        ));
        self.close_bridge_picker();
    }

    /// Lessons of the active track with a bridge from `key`, out of all its lessons with bridges.
    fn bridge_coverage(&self, key: &str) -> (usize, usize) {
        let track = self.active_track();
        let with_bridges: Vec<_> = track
            .nodes()
            .filter(|node| !node.bridges.is_empty())
            .collect();
        let covered = with_bridges
            .iter()
            .filter(|node| node.bridges.contains_key(key))
            .count();
        (covered, with_bridges.len())
    }
}

pub fn render_bridge_picker(frame: &mut Frame, app: &App, area: Rect) {
    let languages = app.bridge_languages();
    let popup = centered(area, 60, 50);
    frame.render_widget(Clear, popup);
    let track_name = &app.active_track().name;
    let items: Vec<ListItem> = languages
        .iter()
        .map(|key| {
            let active = *key == app.profile.settings.bridge_from;
            let (covered, total) = app.bridge_coverage(key);
            ListItem::new(Line::from(vec![
                Span::raw(if active { "● " } else { "  " }),
                format!("{:<26}", language_name(key)).bold().cyan(),
                format!("{covered}/{total} lessons of {track_name}").dark_gray(),
            ]))
        })
        .collect();
    let block = Block::default()
        .borders(Borders::ALL)
        .title(" 🥋 Bridge: which language do you already know? ")
        .border_style(Style::default().fg(Color::Magenta));
    let inner = block.inner(popup);
    frame.render_widget(block, popup);
    let [list_area, help_area] = ratatui::layout::Layout::vertical([
        ratatui::layout::Constraint::Min(3),
        ratatui::layout::Constraint::Length(3),
    ])
    .areas(inner);
    let list = List::new(items)
        .highlight_style(
            Style::default()
                .bg(Color::DarkGray)
                .add_modifier(Modifier::BOLD),
        )
        .highlight_symbol("▶ ");
    let mut state = ListState::default().with_selected(Some(app.bridge_cursor));
    frame.render_stateful_widget(list, list_area, &mut state);
    let help = Paragraph::new(vec![
        Line::from(
            "Each lesson explains its ideas from the language you pick (● is the current one)."
                .dark_gray(),
        ),
        Line::from("[↑/↓] Move | [Enter] Choose | [Esc] Cancel"),
    ]);
    frame.render_widget(help, help_area);
}
