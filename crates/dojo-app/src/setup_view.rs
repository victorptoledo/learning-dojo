//! The setup screen: one row per requirement with its status and why it is needed.

use crate::environment::Status;
use crate::setup::{SetupRow, SetupSession};
use crate::ui::centered;
use ratatui::{
    Frame,
    layout::Rect,
    style::{Color, Style, Stylize},
    text::{Line, Span},
    widgets::{Block, Borders, Clear, Paragraph, Wrap},
};

const LOG_LINES: usize = 6;

pub fn render_setup(frame: &mut Frame, session: &SetupSession, area: Rect) {
    let popup = centered(area, 84, 80);
    frame.render_widget(Clear, popup);
    let mut lines = vec![
        Line::from("Before this step, the dojo checks what it needs on your machine.".dark_gray()),
        Line::from(""),
    ];
    for row in &session.rows {
        lines.extend(row_lines(row));
    }
    lines.push(Line::from(""));
    lines.push(state_line(session));
    if !session.log.is_empty() {
        lines.push(Line::from(""));
        let start = session.log.len().saturating_sub(LOG_LINES);
        lines.extend(
            session.log[start..]
                .iter()
                .map(|entry| Line::from(entry.clone().dark_gray())),
        );
    }
    let block = Block::default()
        .borders(Borders::ALL)
        .title(format!(" 🧰 Prepare: {} ", session.title))
        .border_style(Style::default().fg(Color::Cyan));
    frame.render_widget(
        Paragraph::new(lines)
            .wrap(Wrap { trim: false })
            .block(block),
        popup,
    );
}

fn row_lines(row: &SetupRow) -> Vec<Line<'static>> {
    let requirement = &row.requirement;
    let tag = if requirement.required {
        ""
    } else {
        " (optional)"
    };
    let (icon, detail, color) = match &row.status {
        None => ("…", "checking".to_string(), Color::DarkGray),
        Some(Status::Ready { detail }) => ("✓", detail.clone(), Color::Green),
        Some(Status::NotApplicable { reason }) => ("–", reason.clone(), Color::DarkGray),
        Some(Status::Outdated { found, needed }) => (
            "⚠",
            format!("{found} found, {needed} needed"),
            Color::Yellow,
        ),
        Some(Status::Missing { hint }) if requirement.required => {
            ("✗", hint.clone(), Color::LightRed)
        }
        Some(Status::Missing { hint }) => ("○", hint.clone(), Color::Yellow),
    };
    vec![
        Line::from(vec![
            Span::styled(
                format!(" {icon} {}{tag}", requirement.name),
                Style::default().fg(color).bold(),
            ),
            Span::styled(format!("  {detail}"), Style::default().fg(color)),
        ]),
        Line::from(format!("     {}", requirement.why).dark_gray()),
    ]
}

fn state_line(session: &SetupSession) -> Line<'static> {
    if let Some(busy) = session.busy {
        return Line::from(format!("⏳ {busy}").cyan());
    }
    let keys = "[I] install missing · [U] update · [R] check again · [Esc] back";
    if session.blocks() {
        Line::from(format!("Something required is missing. {keys}").light_red())
    } else {
        Line::from(format!("Ready. [Enter] continue · {keys}").green())
    }
}
