//! The setup screen: a summary, then one row per requirement with a clear status word,
//! what to do about it and why it is needed.

use crate::environment::Status;
use crate::setup::{SetupRow, SetupSession};
use crate::ui::centered;
use dojo_core::RequirementKind;
use ratatui::{
    Frame,
    layout::Rect,
    style::{Color, Modifier, Style, Stylize},
    text::{Line, Span},
    widgets::{Block, Borders, Clear, Paragraph, Wrap},
};

const LOG_LINES: usize = 6;

pub fn render_setup(frame: &mut Frame, session: &SetupSession, area: Rect) {
    let popup = centered(area, 84, 80);
    frame.render_widget(Clear, popup);
    let mut lines = vec![summary_line(session), Line::from("")];
    for row in &session.rows {
        lines.extend(row_lines(row));
        lines.push(Line::from(""));
    }
    lines.push(keys_line(session));
    if !session.log.is_empty() {
        lines.push(Line::from(""));
        lines.push(Line::from("Last actions".dark_gray().bold()));
        let start = session.log.len().saturating_sub(LOG_LINES);
        lines.extend(
            session.log[start..]
                .iter()
                .map(|entry| Line::from(format!("  {entry}").dark_gray())),
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

/// What a row's status means, as a fixed-width word and a color.
fn badge(row: &SetupRow) -> (&'static str, &'static str, Color) {
    match &row.status {
        None => ("…", "CHECKING  ", Color::DarkGray),
        Some(Status::Ready { .. }) => ("✓", "INSTALLED ", Color::Green),
        Some(Status::NotApplicable { .. }) => ("–", "NOT NEEDED", Color::DarkGray),
        Some(Status::Outdated { .. }) => ("⚠", "OUTDATED  ", Color::Yellow),
        Some(Status::Missing { .. }) if row.requirement.required => {
            ("✗", "MISSING   ", Color::LightRed)
        }
        Some(Status::Missing { .. }) => ("○", "MISSING   ", Color::Yellow),
    }
}

fn row_lines(row: &SetupRow) -> Vec<Line<'static>> {
    let requirement = &row.requirement;
    let (icon, word, color) = badge(row);
    let need = if requirement.required {
        "required"
    } else {
        "optional"
    };
    let detail = match &row.status {
        Some(Status::Ready { detail }) if !detail.is_empty() => format!("  {detail}"),
        Some(Status::Outdated { found, needed }) => format!("  {found} found, {needed} needed"),
        Some(Status::NotApplicable { reason }) => format!("  {reason}"),
        _ => String::new(),
    };
    let mut lines = vec![Line::from(vec![
        Span::styled(
            format!(" {icon} {word} "),
            Style::default().fg(color).add_modifier(Modifier::BOLD),
        ),
        Span::styled(
            requirement.name.clone(),
            Style::default().add_modifier(Modifier::BOLD),
        ),
        Span::styled(detail, Style::default().fg(color)),
        format!("  ({need})").dark_gray(),
    ])];
    if let Some(action) = action_line(row) {
        lines.push(action);
    }
    lines.push(Line::from(format!("     {}", requirement.why).dark_gray()));
    lines
}

/// What the learner can do about a row that is not ready.
fn action_line(row: &SetupRow) -> Option<Line<'static>> {
    let automatic = can_install(&row.requirement.kind);
    let text = match &row.status {
        Some(Status::Missing { hint }) if automatic => {
            format!("→ press [I] to install it  ({hint})")
        }
        Some(Status::Missing { hint }) => format!("→ install it yourself: {hint}"),
        Some(Status::Outdated { .. }) => "→ press [U] to update it".to_string(),
        _ => return None,
    };
    Some(Line::from(format!("     {text}").yellow()))
}

/// Requirements the dojo installs itself; tools are installed by the learner.
fn can_install(kind: &RequirementKind) -> bool {
    !matches!(kind, RequirementKind::Tool { .. })
}

fn summary_line(session: &SetupSession) -> Line<'static> {
    if session.rows.iter().any(|row| row.status.is_none()) {
        return Line::from("Checking what this step needs on your machine…".dark_gray());
    }
    let total = session.rows.len();
    let ready = session
        .rows
        .iter()
        .filter(|row| row.status.as_ref().is_some_and(Status::is_satisfied))
        .count();
    let missing_required = session
        .rows
        .iter()
        .filter(|row| {
            row.requirement.required && !row.status.as_ref().is_some_and(Status::is_satisfied)
        })
        .count();
    if ready == total {
        Line::from(
            format!("✓ Everything this step uses is installed ({ready} of {total}).")
                .green()
                .bold(),
        )
    } else if missing_required > 0 {
        Line::from(
            format!(
                "✗ {ready} of {total} ready. {missing_required} required item(s) missing: this step cannot start yet."
            )
            .light_red()
            .bold(),
        )
    } else {
        Line::from(
            format!(
                "✓ {ready} of {total} ready. Only optional items are missing: you can continue without them."
            )
            .yellow()
            .bold(),
        )
    }
}

/// Only the keys that do something right now.
fn keys_line(session: &SetupSession) -> Line<'static> {
    if let Some(busy) = session.busy {
        return Line::from(format!("⏳ {busy}").cyan());
    }
    let mut keys = Vec::new();
    if !session.blocks() {
        keys.push("[Enter] continue");
    }
    let installable = session.rows.iter().any(|row| {
        matches!(row.status, Some(Status::Missing { .. })) && can_install(&row.requirement.kind)
    });
    if installable {
        keys.push("[I] install what is missing");
    }
    let updatable = session.rows.iter().any(|row| {
        matches!(
            row.status,
            Some(Status::Ready { .. } | Status::Outdated { .. })
        ) && !matches!(row.requirement.kind, RequirementKind::DockerEngine { .. })
    });
    if updatable {
        keys.push("[U] update installed items");
    }
    keys.push("[R] check again");
    keys.push("[Esc] back");
    Line::from(keys.join(" · ").dark_gray())
}

#[cfg(test)]
mod tests {
    use super::*;
    use dojo_core::Requirement;
    use ratatui::{Terminal, backend::TestBackend};

    fn requirement(name: &str, kind: RequirementKind, required: bool) -> Requirement {
        Requirement {
            name: name.to_string(),
            why: format!("why {name}"),
            required,
            kind,
        }
    }

    fn tool(name: &str, required: bool) -> Requirement {
        requirement(
            name,
            RequirementKind::Tool {
                command: "java".into(),
                version_args: vec![],
                min_version: Some("25".into()),
                install_hint: "winget install X".into(),
                update: None,
            },
            required,
        )
    }

    fn extension(name: &str) -> Requirement {
        requirement(
            name,
            RequirementKind::VsCodeExtension { id: "x.y".into() },
            false,
        )
    }

    fn screen(session: &SetupSession) -> String {
        let mut terminal = Terminal::new(TestBackend::new(120, 40)).unwrap();
        terminal
            .draw(|frame| render_setup(frame, session, frame.area()))
            .unwrap();
        let buffer = terminal.backend().buffer().clone();
        (0..40)
            .map(|y| {
                (0..120)
                    .map(|x| buffer[(x, y)].symbol())
                    .collect::<String>()
            })
            .collect::<Vec<_>>()
            .join("\n")
    }

    #[test]
    fn everything_installed_says_so_and_offers_no_install() {
        let mut session = SetupSession::new(
            "java",
            "n",
            "Setting Up JDK 25",
            vec![tool("JDK 25", false), extension("Java pack")],
        );
        session.rows[0].status = Some(Status::Ready {
            detail: "25.0.5".into(),
        });
        session.rows[1].status = Some(Status::Ready {
            detail: "0.31.2".into(),
        });
        let text = screen(&session);
        assert!(text.contains("Everything this step uses is installed (2 of 2)"));
        assert!(text.contains("INSTALLED  JDK 25  25.0.5  (optional)"));
        assert!(!text.contains("[I] install"));
        assert!(text.contains("[Enter] continue"));
    }

    #[test]
    fn missing_items_say_what_to_do() {
        let mut session = SetupSession::new(
            "java",
            "n",
            "Kata",
            vec![tool("JDK 25", true), extension("Java pack")],
        );
        session.rows[0].status = Some(Status::Missing {
            hint: "winget install X".into(),
        });
        session.rows[1].status = Some(Status::Missing {
            hint: "code --install-extension x.y".into(),
        });
        let text = screen(&session);
        assert!(text.contains("0 of 2 ready. 1 required item(s) missing"));
        assert!(text.contains("MISSING    JDK 25  (required)"));
        assert!(text.contains("install it yourself: winget install X"));
        assert!(text.contains("press [I] to install it"));
        assert!(text.contains("[I] install what is missing"));
        assert!(!text.contains("[Enter] continue"));
    }
}
