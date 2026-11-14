//! Rendering of lessons (document, quiz status, Rosetta bridge) and of the quiz screen.

use crate::app::App;
use crate::quiz_session::{QuizSession, Stage};
use crate::ui::{centered, labelled, titled};
use dojo_core::{Node, QuestionKind, Track};
use ratatui::{
    Frame,
    layout::Rect,
    style::{Color, Modifier, Style, Stylize},
    text::{Line, Span},
    widgets::{Block, Borders, Clear, Paragraph, Wrap},
};

pub fn render_lesson(frame: &mut Frame, app: &App, track: &Track, node: &Node, area: Rect) {
    let mut lines = if node.under_construction() {
        under_construction_lines()
    } else {
        let mut lines = vec![quiz_status_line(app, track, node), Line::from("")];
        lines.extend(markdown_lines(node.lesson.as_deref().unwrap_or_default()));
        lines
    };
    lines.extend(bridge_lines(app, node));
    let paragraph = Paragraph::new(lines)
        .wrap(Wrap { trim: false })
        .scroll((app.detail_scroll, 0))
        .block(titled(" 📖 Lesson · [PgUp/PgDn] scroll "));
    frame.render_widget(paragraph, area);
}

fn under_construction_lines() -> Vec<Line<'static>> {
    vec![
        Line::from("🚧 Under construction".bold().yellow()),
        Line::from("This lesson still needs its full document and its test."),
        Line::from(
            "It cannot be completed yet, and the nodes after it stay locked until it is written.",
        ),
    ]
}

fn quiz_status_line(app: &App, track: &Track, node: &Node) -> Line<'static> {
    let Some(quiz) = &node.quiz else {
        return Line::from("");
    };
    let key = (track.id.clone(), node.id.clone());
    let base = format!(
        "📝 Quiz: {} questions, {} correct to pass.",
        quiz.questions.len(),
        quiz.needed()
    );
    let text = match app.quiz_history.get(&key) {
        _ if app.profile.is_completed(&track.id, &node.id) => {
            format!("✓ Completed. {base} [Q] takes it again.")
        }
        Some(attempt) if !attempt.score.passed => format!(
            "{base} Last try: {}/{}. Read the lesson again ([Enter] opens it in your editor), then press [Q].",
            attempt.score.correct, attempt.score.total
        ),
        _ => format!(
            "{base} Read the lesson ([Enter] opens it in your editor), then press [Q] for the quiz."
        ),
    };
    Line::from(text.bold().yellow())
}

/// Minimal Markdown: headings, bullet lists and fenced code blocks.
/// Terminal rendering of the Markdown used in lessons: headings, lists, code
/// blocks, tables, block quotes and GitHub-style callouts (`> [!TIP]`).
pub fn markdown_lines(text: &str) -> Vec<Line<'static>> {
    let mut in_code = false;
    let mut lines = Vec::new();
    for raw in text.lines() {
        if raw.trim_start().starts_with("```") {
            in_code = !in_code;
            continue;
        }
        lines.push(if in_code {
            Line::from(format!("    {raw}").light_green())
        } else {
            prose_line(raw)
        });
    }
    lines
}

fn prose_line(raw: &str) -> Line<'static> {
    if let Some(title) = raw.strip_prefix("# ") {
        return Line::from(title.to_string().bold().cyan());
    }
    if let Some(title) = raw.strip_prefix("## ") {
        return Line::from(title.to_string().bold().yellow());
    }
    if let Some(title) = raw.strip_prefix("### ") {
        return Line::from(title.to_string().bold());
    }
    if let Some(quote) = raw.strip_prefix('>') {
        return quote_line(quote.trim_start());
    }
    if let Some(item) = raw.strip_prefix("- ") {
        return Line::from(format!("  • {item}"));
    }
    if raw.starts_with('|') {
        return table_line(raw);
    }
    if raw.trim() == "---" {
        return Line::from("─".repeat(40).dark_gray());
    }
    Line::from(raw.to_string())
}

/// `> [!TIP]` starts a callout; other quoted lines are its body.
fn quote_line(text: &str) -> Line<'static> {
    let label = match text {
        "[!NOTE]" => Some("ℹ Note"),
        "[!TIP]" => Some("💡 Tip"),
        "[!IMPORTANT]" => Some("❗ Important"),
        "[!WARNING]" => Some("⚠ Watch out"),
        "[!CAUTION]" => Some("⛔ Caution"),
        _ => None,
    };
    match label {
        Some(label) => Line::from(format!("┃ {label}").bold().magenta()),
        None => Line::from(format!("┃ {text}").magenta()),
    }
}

/// Tables keep their cells; the `|---|` separator row becomes a rule.
fn table_line(raw: &str) -> Line<'static> {
    let is_separator = raw.chars().all(|c| matches!(c, '|' | '-' | ':' | ' '));
    if is_separator {
        return Line::from("  ".to_string() + &"─".repeat(raw.len().saturating_sub(2)));
    }
    // `\|` is a literal pipe inside a cell (e.g. a closure `|x|`), not a column break.
    let cells: Vec<String> = raw
        .replace("\\|", "\u{0}")
        .trim_matches('|')
        .split('|')
        .map(|cell| cell.trim().replace('\u{0}', "|"))
        .collect();
    Line::from(format!("  {}", cells.join("  │  ")))
}

fn bridge_lines(app: &App, node: &Node) -> Vec<Line<'static>> {
    let source = &app.profile.settings.bridge_from;
    let Some(bridge) = node.bridge_for(source) else {
        return Vec::new();
    };
    let owned =
        |label: &str, text: &str, color: Color| labelled(label, text, color).to_owned_line();
    let mut lines = vec![
        Line::from(""),
        Line::from(
            format!(
                "🥋 Rosetta Bridge (from {}) · [B] changes the language",
                bridge.from
            )
            .bold()
            .magenta(),
        ),
        owned("🎯 Key concept: ", &bridge.concept, Color::Cyan),
        owned(
            &format!("↩ The {} way: ", bridge.from),
            &bridge.from_way,
            Color::LightRed,
        ),
        owned(
            "🚀 The way of this track: ",
            &bridge.native_way,
            Color::LightGreen,
        ),
        owned("💡 Sensei tip: ", &bridge.sensei_tip, Color::Yellow),
    ];
    lines.extend(markdown_lines(&bridge.body));
    lines
}

trait ToOwnedLine {
    fn to_owned_line(self) -> Line<'static>;
}

impl ToOwnedLine for Line<'_> {
    fn to_owned_line(self) -> Line<'static> {
        Line::from(
            self.spans
                .into_iter()
                .map(|span| Span::styled(span.content.into_owned(), span.style))
                .collect::<Vec<_>>(),
        )
    }
}

// ---- quiz screen ------------------------------------------------------------

pub fn render_quiz(frame: &mut Frame, app: &App, area: Rect) {
    let Some(session) = &app.quiz else {
        return;
    };
    let popup = centered(area, 80, 80);
    frame.render_widget(Clear, popup);
    let lines = match &session.stage {
        Stage::Finished(score) => finished_lines(session, score.correct, score.total, score.passed),
        Stage::Asking | Stage::Feedback { .. } => question_lines(session),
    };
    let title = format!(" 📝 Quiz: {} ", session.title);
    let block = Block::default()
        .borders(Borders::ALL)
        .title(title)
        .border_style(Style::default().fg(Color::Yellow));
    frame.render_widget(
        Paragraph::new(lines)
            .wrap(Wrap { trim: false })
            .block(block),
        popup,
    );
}

fn question_lines(session: &QuizSession) -> Vec<Line<'static>> {
    let question = session.question();
    let mut lines = vec![
        Line::from(
            format!(
                "Question {} of {} · {} correct so far",
                session.index + 1,
                session.quiz.questions.len(),
                session.correct
            )
            .dark_gray(),
        ),
        Line::from(""),
        Line::from(question.prompt.clone().bold()),
    ];
    if let Some(code) = &question.code {
        lines.push(Line::from(""));
        lines.extend(
            code.lines()
                .map(|line| Line::from(format!("    {line}").light_green())),
        );
    }
    lines.push(Line::from(""));
    match &question.kind {
        QuestionKind::SingleChoice { options, .. } => {
            lines.extend(
                options
                    .iter()
                    .enumerate()
                    .map(|(index, option)| Line::from(format!("  [{}] {option}", index + 1))),
            );
        }
        QuestionKind::Text { .. } => {
            lines.push(Line::from(vec![
                Span::raw("  Your answer: "),
                Span::styled(
                    format!("{}▏", session.input),
                    Style::default().add_modifier(Modifier::BOLD),
                ),
            ]));
        }
    }
    lines.push(Line::from(""));
    lines.extend(feedback_or_hint(session));
    lines
}

fn feedback_or_hint(session: &QuizSession) -> Vec<Line<'static>> {
    let question = session.question();
    match session.stage {
        Stage::Feedback { correct: true } => vec![
            Line::from("✅ Correct!".bold().green()),
            Line::from(question.explanation.clone()),
            Line::from(""),
            Line::from("[Enter] next question".dark_gray()),
        ],
        Stage::Feedback { correct: false } => vec![
            Line::from(
                format!("❌ Not quite. The answer is: {}", question.correct_answer())
                    .bold()
                    .light_red(),
            ),
            Line::from(question.explanation.clone()),
            Line::from(""),
            Line::from("[Enter] next question".dark_gray()),
        ],
        Stage::Asking if session.is_text_question() => {
            vec![Line::from(
                "Type your answer and press [Enter] · [Esc] leave the quiz".dark_gray(),
            )]
        }
        Stage::Asking | Stage::Finished(_) => {
            vec![Line::from(
                "Press the number of your answer · [Esc] leave the quiz".dark_gray(),
            )]
        }
    }
}

fn finished_lines(
    session: &QuizSession,
    correct: u32,
    total: u32,
    passed: bool,
) -> Vec<Line<'static>> {
    let verdict = if passed {
        Line::from(
            format!("🎉 Passed: {correct}/{total} correct.")
                .bold()
                .green(),
        )
    } else {
        Line::from(
            format!(
                "You got {correct}/{total}; {} are needed to pass.",
                session.quiz.needed()
            )
            .bold()
            .light_red(),
        )
    };
    let next = if passed {
        "The lesson will be marked as completed."
    } else {
        "Read the lesson again (the explanations above help) and retake the quiz."
    };
    vec![
        verdict,
        Line::from(""),
        Line::from(next),
        Line::from(""),
        Line::from("[Enter] back to the track".dark_gray()),
    ]
}

#[cfg(test)]
mod tests {
    use super::markdown_lines;

    fn text_of(markdown: &str) -> Vec<String> {
        markdown_lines(markdown)
            .into_iter()
            .map(|line| {
                line.spans
                    .iter()
                    .map(|span| span.content.to_string())
                    .collect()
            })
            .collect()
    }

    #[test]
    fn callouts_quotes_and_tables_are_drawn_for_the_terminal() {
        let rendered =
            text_of("> [!TIP]\n> Use iterators.\n| Rust | Java |\n|---|---|\n| `let` | `var` |\n");

        assert_eq!(rendered[0], "┃ 💡 Tip");
        assert_eq!(rendered[1], "┃ Use iterators.");
        assert_eq!(rendered[2], "  Rust  │  Java");
        assert!(rendered[3].trim().chars().all(|c| c == '─'));
        assert_eq!(rendered[4], "  `let`  │  `var`");
    }

    #[test]
    fn escaped_pipes_stay_inside_their_cell() {
        let rendered = text_of("| spawn | `thread::spawn(move \\|\\| work)` |\n");

        assert_eq!(rendered[0], "  spawn  │  `thread::spawn(move || work)`");
    }

    #[test]
    fn code_blocks_keep_their_content_and_drop_the_fences() {
        let rendered = text_of("```rust\nlet x = 5;\n```\n");

        assert_eq!(rendered, vec!["    let x = 5;"]);
    }
}
