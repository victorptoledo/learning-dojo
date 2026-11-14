//! Rendering of the career screen: the paths, the stages of the highlighted one, and
//! the next steps.

use crate::app::App;
use crate::career_actions::reason_text;
use crate::ui::{centered, titled};
use dojo_core::{Career, CareerRequirement, Catalog, Profile};
use ratatui::{
    Frame,
    layout::{Constraint, Layout, Rect},
    style::{Color, Modifier, Style, Stylize},
    text::{Line, Span},
    widgets::{Clear, List, ListItem, ListState, Paragraph, Wrap},
};

pub fn render_careers(frame: &mut Frame, app: &App, area: Rect) {
    let popup = centered(area, 90, 85);
    frame.render_widget(Clear, popup);
    let [list_area, detail_area] =
        Layout::horizontal([Constraint::Percentage(38), Constraint::Percentage(62)]).areas(popup);
    render_list(frame, app, list_area);
    if let Some(career) = app.catalog.careers.get(app.career_cursor) {
        let mut lines = career_lines(&app.catalog, &app.profile, career);
        lines.push(Line::from(""));
        lines.extend(next_step_lines(app));
        lines.push(Line::from(""));
        lines.push(Line::from(
            "[↑/↓] Move · [Enter] set or clear the target · [Esc] back".dark_gray(),
        ));
        frame.render_widget(
            Paragraph::new(lines)
                .wrap(Wrap { trim: false })
                .block(titled(format!(" {} ", career.title))),
            detail_area,
        );
    }
}

fn render_list(frame: &mut Frame, app: &App, area: Rect) {
    let target = app.profile.settings.target_career.as_deref();
    let items: Vec<ListItem> = app
        .catalog
        .careers
        .iter()
        .map(|career| {
            let progress = career.progress(&app.catalog, &app.profile);
            let mark = if target == Some(career.id.as_str()) {
                "🎯 "
            } else {
                "   "
            };
            ListItem::new(vec![
                Line::from(vec![Span::raw(mark), career.title.clone().bold()]),
                Line::from(
                    format!(
                        "   {} {}/{} stages",
                        bar(progress.overall(), 12),
                        progress.finished_stages(),
                        career.stages.len()
                    )
                    .dark_gray(),
                ),
            ])
        })
        .collect();
    let list = List::new(items)
        .block(titled(" 🧭 Career paths "))
        .highlight_style(Style::default().bg(Color::DarkGray));
    let mut state = ListState::default().with_selected(Some(app.career_cursor));
    frame.render_stateful_widget(list, area, &mut state);
}

fn career_lines(catalog: &Catalog, profile: &Profile, career: &Career) -> Vec<Line<'static>> {
    let progress = career.progress(catalog, profile);
    let mut lines = vec![Line::from(career.summary.clone())];
    if !career.roles.is_empty() {
        lines.push(Line::from(
            format!("Roles: {}", career.roles.join(" · ")).dark_gray(),
        ));
    }
    lines.push(Line::from(""));
    for (index, stage) in career.stages.iter().enumerate() {
        let done = progress.stages[index];
        let (icon, color) = if done >= 1.0 {
            ("✓", Color::Green)
        } else if progress.current == Some(index) {
            ("▶", Color::Yellow)
        } else {
            ("○", Color::DarkGray)
        };
        lines.push(Line::from(vec![
            Span::styled(
                format!("{icon} Stage {}: {} ", index + 1, stage.title),
                Style::default().fg(color).add_modifier(Modifier::BOLD),
            ),
            format!("{:.0}%", done * 100.0).dark_gray(),
        ]));
        for requirement in &stage.requires {
            let share = requirement.progress(catalog, profile);
            let mark = if share >= 1.0 { "✓" } else { "·" };
            lines.push(Line::from(format!(
                "    {mark} {} {} {:.0}%",
                requirement_text(catalog, requirement),
                bar(share, 8),
                share * 100.0
            )));
        }
    }
    let next: Vec<&str> = career
        .next_paths
        .iter()
        .filter_map(|id| catalog.career(id))
        .map(|next| next.title.as_str())
        .collect();
    if !next.is_empty() {
        lines.push(Line::from(""));
        lines.push(Line::from(
            format!("Then: {}", next.join(" · ")).dark_gray(),
        ));
    }
    lines
}

fn requirement_text(catalog: &Catalog, requirement: &CareerRequirement) -> String {
    match requirement {
        CareerRequirement::TrackPhase { track, phase } => {
            let name = catalog
                .track(track)
                .map_or(track.clone(), |found| found.name.clone());
            format!("{name} up to {}", phase.to_uppercase())
        }
        CareerRequirement::Skill { skill, min } => {
            let title = catalog
                .skills
                .get(skill)
                .map_or(skill.clone(), |found| found.title.clone());
            format!("{title} ({:.0}% of its nodes)", min * 100.0)
        }
        CareerRequirement::AnyOf(options) => {
            let texts: Vec<String> = options
                .iter()
                .map(|option| requirement_text(catalog, option))
                .collect();
            format!("one of: {}", texts.join(" / "))
        }
    }
}

fn next_step_lines(app: &App) -> Vec<Line<'static>> {
    let recommendations = app.recommendations();
    let mut lines = vec![Line::from("👉 Next steps".bold().cyan())];
    if recommendations.is_empty() {
        lines.push(Line::from(
            "Nothing open right now: the next nodes are still being written.".dark_gray(),
        ));
        return lines;
    }
    for (index, rec) in recommendations.iter().enumerate() {
        let Some(track) = app.catalog.track(&rec.track_id) else {
            continue;
        };
        let Some(node) = track.node(&rec.node_id) else {
            continue;
        };
        lines.push(Line::from(vec![
            format!(
                "{}. {} {} · {}",
                index + 1,
                track.icon,
                track.name,
                node.title
            )
            .bold(),
            format!("  {}", reason_text(&rec.reasons)).dark_gray(),
        ]));
    }
    lines
}

/// A text progress bar such as `███░░░░░`.
fn bar(share: f64, width: usize) -> String {
    let filled = ((share.clamp(0.0, 1.0) * width as f64).round() as usize).min(width);
    format!("{}{}", "█".repeat(filled), "░".repeat(width - filled))
}
