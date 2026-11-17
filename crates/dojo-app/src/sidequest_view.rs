//! Rendering of the sidequest screen: the active track's sidequests on the left (locked,
//! in progress or done), and the highlighted one on the right with what unlocks it, its
//! stages and the open stage's brief.

use crate::app::App;
use crate::ui::{centered, titled};
use dojo_core::{SIDEQUEST_STAGE_XP, Sidequest};
use ratatui::{
    Frame,
    layout::{Constraint, Layout, Rect},
    style::{Color, Style, Stylize},
    text::{Line, Span},
    widgets::{Clear, List, ListItem, ListState, Paragraph, Wrap},
};

pub fn render_sidequests(frame: &mut Frame, app: &App, area: Rect) {
    let popup = centered(area, 90, 85);
    frame.render_widget(Clear, popup);
    let [list_area, detail_area] =
        Layout::horizontal([Constraint::Percentage(36), Constraint::Percentage(64)]).areas(popup);
    render_list(frame, app, list_area);
    if let Some(quest) = app.selected_sidequest() {
        let mut lines = detail_lines(app, quest);
        lines.push(Line::from(""));
        lines.push(Line::from(
            "[↑/↓] Move · [Enter] start or open in the editor · [C] check the open stage · [Esc] back"
                .dark_gray(),
        ));
        frame.render_widget(
            Paragraph::new(lines)
                .wrap(Wrap { trim: false })
                .scroll((app.detail_scroll, 0))
                .block(titled(format!(" N{} · {} ", quest.level, quest.title))),
            detail_area,
        );
    }
}

/// `🔒`, `▶ 1/4`, `🏅` or `·` (unlocked, not started).
pub fn status_mark(app: &App, quest: &Sidequest) -> String {
    if quest.is_done(&app.profile) {
        "🏅".to_string()
    } else if !quest.is_unlocked(&app.catalog, &app.profile) {
        "🔒".to_string()
    } else {
        let passed = quest.passed_stages(&app.profile);
        if passed == 0 && !app.workspace.sidequest_dir(quest).exists() {
            "· ".to_string()
        } else {
            format!("▶ {passed}/{}", quest.stages.len())
        }
    }
}

fn render_list(frame: &mut Frame, app: &App, area: Rect) {
    let items: Vec<ListItem> = app
        .track_sidequests()
        .iter()
        .map(|quest| {
            ListItem::new(vec![
                Line::from(vec![
                    Span::raw(format!(" {} ", status_mark(app, quest))),
                    format!("N{} ", quest.level).dark_gray(),
                    quest.title.clone().bold(),
                ]),
                Line::from(format!("    {} stages", quest.stages.len()).dark_gray()),
            ])
        })
        .collect();
    let badges = app
        .track_sidequests()
        .iter()
        .filter(|quest| quest.is_done(&app.profile))
        .count();
    let list = List::new(items)
        .block(titled(format!(
            " ⚔ Sidequests of {} · 🏅 {badges} ",
            app.active_track().name
        )))
        .highlight_style(Style::default().bg(Color::DarkGray));
    let mut state = ListState::default().with_selected(Some(app.sidequest_cursor));
    frame.render_stateful_widget(list, area, &mut state);
}

fn detail_lines(app: &App, quest: &Sidequest) -> Vec<Line<'static>> {
    let mut lines = vec![
        Line::from(quest.summary.clone()),
        Line::from(
            "Optional project: +30 sidequest XP per stage and a badge, never a belt."
                .replace("30", &SIDEQUEST_STAGE_XP.to_string())
                .dark_gray(),
        ),
        Line::from(""),
        Line::from("Unlocks with".bold()),
    ];
    for requirement in &quest.requires {
        let met = requirement.is_met(&app.catalog, &app.profile);
        let mark = if met { "✓".green() } else { "✗".red() };
        lines.push(Line::from(vec![
            Span::raw("  "),
            mark,
            Span::raw(format!(" {}", requirement.label(&app.catalog))),
        ]));
    }
    let missing = quest.missing(&app.catalog, &app.profile);
    if !missing.is_empty() {
        let labels: Vec<String> = missing
            .iter()
            .map(|requirement| requirement.label(&app.catalog))
            .collect();
        lines.push(Line::from(""));
        lines.push(Line::from(
            format!("🔒 Missing: {}", labels.join(", ")).bold().yellow(),
        ));
        return lines;
    }
    lines.push(Line::from(""));
    lines.push(Line::from("Stages".bold()));
    let current = quest.current_stage(&app.profile);
    for (index, stage) in quest.stages.iter().enumerate() {
        let line = match current {
            Some(open) if index == open => {
                format!("  ▶ {}. {}", index + 1, stage.title).bold().cyan()
            }
            Some(open) if index > open => format!("  · {}. (opens later)", index + 1).dark_gray(),
            _ => format!("  ✓ {}. {}", index + 1, stage.title).green(),
        };
        lines.push(Line::from(line));
    }
    lines.push(Line::from(""));
    let dir = app.workspace.sidequest_dir(quest);
    match current {
        Some(open) => {
            if let Some(report) = app
                .last_reports
                .get(&(quest.track_id.clone(), quest.stages[open].id.clone()))
            {
                lines.push(Line::from(
                    format!("Last check: {}", report.summary()).yellow(),
                ));
                for test in report.tests.iter().filter(|test| !test.passed).take(8) {
                    lines.push(Line::from(format!("  ✗ {}", test.name).red()));
                    if let Some(message) = &test.message {
                        lines.push(Line::from(format!("    {message}").dark_gray()));
                    }
                }
                lines.push(Line::from(""));
            }
            if dir.exists() {
                lines.push(Line::from(format!("Folder: {}", dir.display()).dark_gray()));
            } else {
                lines.push(Line::from(
                    "Press [Enter] to create the project folder and open it.".green(),
                ));
            }
            lines.push(Line::from(""));
            lines.extend(
                quest.stages[open]
                    .kata
                    .statement
                    .lines()
                    .map(|line| Line::from(line.to_string())),
            );
        }
        None => lines.push(Line::from(
            format!(
                "🏅 Badge earned: {}. Folder: {}",
                quest.title,
                dir.display()
            )
            .bold()
            .green(),
        )),
    }
    lines
}
