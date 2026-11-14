//! Rendering of the placement test: entry question, questions and the result.

use crate::app::App;
use crate::placement_flow::PlacementStage;
use crate::ui::centered;
use dojo_core::{PhaseVerdict, PlacementSession, QuestionKind, Track};
use ratatui::{
    Frame,
    layout::Rect,
    style::{Color, Modifier, Style, Stylize},
    text::{Line, Span},
    widgets::{Block, Borders, Clear, Paragraph, Wrap},
};

pub fn render_placement(frame: &mut Frame, app: &App, area: Rect) {
    let Some(flow) = &app.placement else {
        return;
    };
    let Some(track) = app.catalog.track(&flow.track_id) else {
        return;
    };
    let popup = centered(area, 80, 80);
    frame.render_widget(Clear, popup);
    let lines = match &flow.stage {
        PlacementStage::Entry => entry_lines(track),
        PlacementStage::Asking { session, input } => question_lines(session, input),
        PlacementStage::Result { session, plan } => result_lines(track, session, plan),
    };
    let block = Block::default()
        .borders(Borders::ALL)
        .title(format!(" 🧭 Placement test: {} ", track.name))
        .border_style(Style::default().fg(Color::Cyan));
    frame.render_widget(
        Paragraph::new(lines)
            .wrap(Wrap { trim: false })
            .block(block),
        popup,
    );
}

fn entry_lines(track: &Track) -> Vec<Line<'static>> {
    let Some(config) = &track.placement else {
        return Vec::new();
    };
    let mut lines = vec![
        Line::from("Skip what you already know.".bold()),
        Line::from(
            "A few questions per phase find where you should start. Skipped nodes count as done; \
             a Belt Exam (a checkpoint) confirms them and gives their XP.",
        ),
        Line::from(""),
        Line::from(config.entry_prompt.clone().bold().yellow()),
        Line::from(""),
    ];
    lines.extend(
        config
            .entry_options
            .iter()
            .enumerate()
            .map(|(index, option)| Line::from(format!("  [{}] {}", index + 1, option.label))),
    );
    lines.push(Line::from(""));
    lines.push(Line::from(
        "Press the number of your answer · [Esc] leave".dark_gray(),
    ));
    lines
}

fn question_lines(session: &PlacementSession, input: &str) -> Vec<Line<'static>> {
    let Some(item) = session.current() else {
        return Vec::new();
    };
    let (position, total) = session.progress_in_phase();
    let phase = session.current_phase().unwrap_or_default().to_uppercase();
    let mut lines = vec![
        Line::from(
            format!(
                "Phase {phase} · question {position} of {total} · {} answered",
                session.asked()
            )
            .dark_gray(),
        ),
        Line::from(""),
        Line::from(item.question.prompt.clone().bold()),
    ];
    if let Some(code) = &item.question.code {
        lines.push(Line::from(""));
        lines.extend(
            code.lines()
                .map(|line| Line::from(format!("    {line}").light_green())),
        );
    }
    lines.push(Line::from(""));
    match &item.question.kind {
        QuestionKind::SingleChoice { options, .. } => {
            lines.extend(
                options
                    .iter()
                    .enumerate()
                    .map(|(index, option)| Line::from(format!("  [{}] {option}", index + 1))),
            );
            lines.push(Line::from("  [0] I don't know".dark_gray()));
            lines.push(Line::from(""));
            lines.push(Line::from(
                "Press the number of your answer · [Esc] leave (nothing is recorded)".dark_gray(),
            ));
        }
        QuestionKind::Text { .. } => {
            lines.push(Line::from(vec![
                Span::raw("  Your answer: "),
                Span::styled(
                    format!("{input}▏"),
                    Style::default().add_modifier(Modifier::BOLD),
                ),
            ]));
            lines.push(Line::from(""));
            lines.push(Line::from(
                "Type and press [Enter] · empty [Enter] = I don't know · [Esc] leave".dark_gray(),
            ));
        }
    }
    lines
}

fn result_lines(
    track: &Track,
    session: &PlacementSession,
    plan: &dojo_core::PlacementPlan,
) -> Vec<Line<'static>> {
    let mut lines = vec![Line::from("Your result".bold().cyan()), Line::from("")];
    for (phase_id, verdict) in session.verdicts() {
        let title = track
            .phases
            .iter()
            .find(|phase| phase.id == *phase_id)
            .map_or(String::new(), |phase| phase.title.clone());
        let (text, color) = match verdict {
            PhaseVerdict::Mastered => ("✓ mastered", Color::Green),
            PhaseVerdict::Partial => ("◐ partly known", Color::Yellow),
            PhaseVerdict::NotYet => ("○ to study", Color::LightRed),
        };
        lines.push(Line::from(vec![
            Span::raw(format!("  {} {title:<16} ", phase_id.to_uppercase())),
            Span::styled(
                text,
                Style::default().fg(color).add_modifier(Modifier::BOLD),
            ),
        ]));
    }
    lines.push(Line::from(""));
    if plan.is_empty() {
        lines.push(Line::from(
            "Nothing to skip: you start at the beginning of the track.",
        ));
    } else {
        let minutes: u32 = plan
            .placed_out
            .iter()
            .filter_map(|id| track.node(id))
            .map(|node| node.estimated_minutes)
            .sum();
        lines.push(Line::from(
            format!(
                "⏭ {} nodes will be placed out (about {} h {:02} min saved).",
                plan.placed_out.len(),
                minutes / 60,
                minutes % 60
            )
            .bold(),
        ));
        if let Some(exam) = plan.belt_exam.as_deref().and_then(|id| track.node(id)) {
            lines.push(Line::from(
                format!(
                    "🥋 Belt Exam: {}. Pass it to confirm the skips and earn their XP; \
                     a wrong answer sends the matching nodes back to study.",
                    exam.title
                )
                .yellow(),
            ));
        }
    }
    lines.push(Line::from(""));
    lines.push(Line::from(
        "[Enter] apply this plan · [Esc] discard it".dark_gray(),
    ));
    lines
}
