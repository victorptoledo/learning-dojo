use crate::app::{App, Screen};
use crate::lesson_view::{quiz_status_line, render_lesson, render_quiz};
use crate::setup_view::render_setup;
use crate::tree::TreeRow;
use dojo_core::{
    CheckOutcome, CheckReport, Kata, KataLanguage, Mode, Node, NodeKind, Phase, Track,
};
use ratatui::{
    Frame,
    layout::{Constraint, Layout, Rect},
    style::{Color, Modifier, Style, Stylize},
    text::{Line, Span},
    widgets::{Block, Borders, Clear, Gauge, List, ListItem, ListState, Paragraph, Wrap},
};

pub fn render(frame: &mut Frame, app: &App) {
    let [header, body, footer] = Layout::vertical([
        Constraint::Length(4),
        Constraint::Min(10),
        Constraint::Length(3),
    ])
    .areas(frame.area());
    render_header(frame, app, header);
    render_body(frame, app, body);
    render_footer(frame, app, footer);
    match app.screen {
        Screen::TrackSelector => render_track_selector(frame, app, frame.area()),
        Screen::Quiz => render_quiz(frame, app, frame.area()),
        Screen::Setup => {
            if let Some(session) = &app.setup {
                render_setup(frame, session, frame.area());
            }
        }
        Screen::Placement => crate::placement_view::render_placement(frame, app, frame.area()),
        Screen::Career => crate::career_view::render_careers(frame, app, frame.area()),
        Screen::TopicStudy => crate::topic_study::render_topic_study(frame, app, frame.area()),
        Screen::Interview => crate::interview_sim::render_interview(frame, app, frame.area()),
        Screen::BridgePicker => {
            crate::bridge_picker::render_bridge_picker(frame, app, frame.area())
        }
        Screen::TrackView => {}
    }
}

// ---- header -----------------------------------------------------------------

fn render_header(frame: &mut Frame, app: &App, area: Rect) {
    let [track_area, xp_area, streak_area] = Layout::horizontal([
        Constraint::Percentage(45),
        Constraint::Percentage(35),
        Constraint::Percentage(20),
    ])
    .areas(area);
    let track = app.active_track();

    let belt = app.profile.current_belt(track);
    let belt_label = belt.map_or_else(String::new, |belt| {
        format!("{} [{}]", belt.name, belt.title)
    });
    let track_info = vec![
        Line::from(
            format!(" {} {}  [M] switch track", track.icon, track.name)
                .bold()
                .cyan(),
        ),
        Line::from(vec![
            Span::raw(" 🥋 "),
            Span::raw(&app.profile.settings.name).bold(),
            Span::raw(" | "),
            Span::raw(belt_label).yellow(),
        ]),
    ];
    frame.render_widget(
        Paragraph::new(track_info).block(titled(" ⛩️ Active Track ")),
        track_area,
    );

    frame.render_widget(belt_gauge(app, track), xp_area);

    let streak = vec![
        Line::from(vec![
            " 🔥 STREAK ".bold().red(),
            format!("{} days", app.profile.streak_on(App::today()))
                .bold()
                .yellow(),
        ]),
        Line::from(vec![
            Span::raw(" Total XP: "),
            app.profile.total_xp.to_string().bold().green(),
            format!(" · 🔁 {}", app.profile.practice_xp).dark_gray(),
        ]),
    ];
    frame.render_widget(
        Paragraph::new(streak).block(titled(" Consistency ")),
        streak_area,
    );
}

fn belt_gauge<'a>(app: &App, track: &'a Track) -> Gauge<'a> {
    let xp = app.profile.track_xp(track);
    let floor = app
        .profile
        .current_belt(track)
        .map_or(0, |belt| belt.required_xp);
    let (ratio, label) = match app.profile.next_belt(track) {
        Some(next) => {
            let span = (next.required_xp - floor).max(1);
            let ratio = f64::from(xp.saturating_sub(floor)) / f64::from(span);
            (
                ratio.clamp(0.0, 1.0),
                format!("{xp}/{} XP (next: {})", next.required_xp, next.name),
            )
        }
        None => (1.0, format!("{xp} XP (highest belt!)")),
    };
    Gauge::default()
        .block(titled(" ⚡ Track XP "))
        .gauge_style(Style::default().fg(Color::Magenta).bg(Color::Black))
        .ratio(ratio)
        .label(label)
}

// ---- body -------------------------------------------------------------------

fn render_body(frame: &mut Frame, app: &App, area: Rect) {
    let [tree_area, detail_area] =
        Layout::horizontal([Constraint::Percentage(42), Constraint::Percentage(58)]).areas(area);
    render_tree(frame, app, tree_area);
    render_detail(frame, app, detail_area);
}

fn render_tree(frame: &mut Frame, app: &App, area: Rect) {
    let track = app.active_track();
    let items: Vec<ListItem> = app
        .tree
        .rows()
        .iter()
        .map(|row| match *row {
            TreeRow::Phase { phase } => phase_item(app, track, &track.phases[phase]),
            TreeRow::Node { phase, node } => {
                node_item(app, track, &track.phases[phase].nodes[node])
            }
        })
        .collect();
    let completed = app.profile.completed_count(track);
    let list = List::new(items)
        .block(titled(format!(
            " 🗺️ {} · {completed}/{} nodes ",
            track.name,
            track.node_count()
        )))
        .highlight_style(
            Style::default()
                .bg(Color::DarkGray)
                .add_modifier(Modifier::BOLD),
        )
        .highlight_symbol("▶ ");
    let mut state = ListState::default().with_selected(app.tree.cursor());
    frame.render_stateful_widget(list, area, &mut state);
}

fn phase_item<'a>(app: &App, track: &Track, phase: &'a Phase) -> ListItem<'a> {
    let done = phase
        .nodes
        .iter()
        .filter(|node| app.profile.is_completed(&track.id, &node.id))
        .count();
    let progress = if phase.nodes.is_empty() {
        "no nodes yet".to_string()
    } else {
        format!("{done}/{}", phase.nodes.len())
    };
    ListItem::new(Line::from(vec![
        format!("▾ {} {}", phase.id.to_uppercase(), phase.title)
            .bold()
            .cyan(),
        format!("  {progress}").dark_gray(),
    ]))
}

fn node_item<'a>(app: &App, track: &Track, node: &'a Node) -> ListItem<'a> {
    let (icon, style) = node_status(app, track, node);
    ListItem::new(Line::from(vec![
        Span::raw("    "),
        Span::styled(icon, style),
        Span::raw(" "),
        Span::styled(node.title.as_str(), style),
        format!("  +{} XP", node_xp(app, track, node)).dark_gray(),
    ]))
}

/// The XP shown for a node: what it gave, or what it gives now (hints lower it).
fn node_xp(app: &App, track: &Track, node: &Node) -> u32 {
    if app.profile.is_completed(&track.id, &node.id) {
        app.profile.earned_xp(&track.id, &node.id)
    } else {
        app.profile.xp_for(&track.id, node)
    }
}

fn node_status(app: &App, track: &Track, node: &Node) -> (&'static str, Style) {
    if app.profile.is_completed(&track.id, &node.id) {
        (
            "✓",
            Style::default()
                .fg(Color::Green)
                .add_modifier(Modifier::BOLD),
        )
    } else if app.profile.is_placed_out(&track.id, &node.id) {
        ("⏭", Style::default().fg(Color::Cyan))
    } else if node.under_construction() {
        ("🚧", Style::default().fg(Color::DarkGray))
    } else if !app.profile.is_unlocked(track, node) {
        ("🔒", Style::default().fg(Color::DarkGray))
    } else if app.is_checking(&track.id, &node.id) {
        ("⏳", Style::default().fg(Color::Cyan))
    } else if kata_started(app, track, node) {
        ("◐", Style::default().fg(Color::Yellow))
    } else if app.profile.belt_exam(&track.id) == Some(node.id.as_str()) {
        ("🥋", Style::default().fg(Color::Magenta))
    } else if node.kind == NodeKind::Checkpoint {
        ("📋", Style::default().fg(Color::Yellow))
    } else {
        ("○", Style::default().fg(Color::Yellow))
    }
}

fn render_detail(frame: &mut Frame, app: &App, area: Rect) {
    let track = app.active_track();
    let Some((phase, node)) = app.tree.selected(track) else {
        let empty = Paragraph::new("This track has no nodes yet.").block(titled(" 📖 Node "));
        frame.render_widget(empty, area);
        return;
    };
    // The lower panel holds the lesson document or the kata; the summary stays short.
    let summary_height = 25;
    let [summary_area, bridge_area] = Layout::vertical([
        Constraint::Percentage(summary_height),
        Constraint::Percentage(100 - summary_height),
    ])
    .areas(area);
    render_summary(frame, app, track, phase, node, summary_area);
    match &node.kata {
        Some(kata) => render_kata(frame, app, track, phase, node, kata, bridge_area),
        None => render_lesson(frame, app, track, node, bridge_area),
    }
}

fn kata_started(app: &App, track: &Track, node: &Node) -> bool {
    node.kata.is_some()
        && track
            .phase_of(&node.id)
            .is_some_and(|phase| app.workspace.kata_dir(track, phase, node).exists())
}

fn render_kata(
    frame: &mut Frame,
    app: &App,
    track: &Track,
    phase: &Phase,
    node: &Node,
    kata: &Kata,
    area: Rect,
) {
    let dir = app.workspace.kata_dir(track, phase, node);
    let key = (track.id.clone(), node.id.clone());
    let mut lines = Vec::new();
    if node.kind == NodeKind::Checkpoint {
        if app.profile.belt_exam(&track.id) == Some(node.id.as_str()) {
            lines.push(Line::from(
                "🥋 Belt Exam: passing this checkpoint confirms the nodes you placed out (⏭) and gives their XP."
                    .bold()
                    .yellow(),
            ));
        }
        lines.push(Line::from(
            "📋 Checkpoint: pass the quiz [Q] and the integration kata below."
                .bold()
                .magenta(),
        ));
        lines.push(quiz_status_line(app, track, node));
        lines.push(Line::from(""));
    }
    lines.extend(kata_steps(&dir, kata.language()));
    lines.push(Line::from(""));
    lines.push(check_status_line(app, &key));
    if let Some(report) = app.last_reports.get(&key) {
        lines.extend(report_lines(report));
    }
    lines.extend(hint_lines(app, track, node, kata));
    lines.push(Line::from(""));
    lines.push(Line::from(
        "🤖 [A] copies a review prompt for an AI of your choice: attach your files and get a code review (feedback only)."
            .dark_gray(),
    ));
    lines.push(Line::from(""));
    lines.push(Line::from("📜 The challenge".bold().cyan()));
    lines.extend(
        kata.statement
            .lines()
            .map(|line| Line::from(line.to_string())),
    );
    let paragraph = Paragraph::new(lines)
        .wrap(Wrap { trim: false })
        .scroll((app.detail_scroll, 0))
        .block(titled(" 🥋 Kata · [PgUp/PgDn] scroll "));
    frame.render_widget(paragraph, area);
}

/// The hints opened so far and how to open the next one.
fn hint_lines(app: &App, track: &Track, node: &Node, kata: &Kata) -> Vec<Line<'static>> {
    if kata.hints.is_empty() {
        return Vec::new();
    }
    let opened = app.profile.hints_revealed(&track.id, &node.id);
    let done = app.profile.is_completed(&track.id, &node.id);
    let total = kata.hints.len();
    let next = crate::mentor_actions::hint_key_label(opened, total, done)
        .map_or(String::new(), |label| format!(" · {label}"));
    let mut lines = vec![
        Line::from(""),
        Line::from(
            format!("💡 Hints: {opened} of {total} open{next}")
                .bold()
                .yellow(),
        ),
    ];
    for (level, hint) in kata.hints.iter().take(usize::from(opened)).enumerate() {
        lines.push(Line::from(format!("Hint {}", level + 1).bold()));
        lines.extend(crate::lesson_view::markdown_lines(hint));
    }
    lines
}

/// What to do next, as numbered steps that depend on whether the kata was started.
fn kata_steps(dir: &std::path::Path, language: KataLanguage) -> Vec<Line<'static>> {
    if !dir.exists() {
        return vec![
            Line::from("👉 How to start".bold().yellow()),
            Line::from("1. Press [Enter] to create your kata folder with the starter code."),
            Line::from("2. Then press [O] to open it in your editor."),
        ];
    }
    vec![
        Line::from("👉 What to do".bold().yellow()),
        Line::from(format!("1. Press [O] to open {}.", dir.display())),
        Line::from(match language {
            KataLanguage::Rust => {
                "2. Edit src/lib.rs. Each function that still has todo!(...) is yours to write:"
            }
            KataLanguage::Java => {
                "2. Edit the classes in src/main/java. Each method that still throws UnsupportedOperationException(\"todo ...\") is yours to write:"
            }
            KataLanguage::Bash => {
                "2. Edit the .sh scripts in the kata folder. Each one that still prints \"todo: ...\" and exits is yours to write:"
            }
            KataLanguage::Docker => {
                "2. Edit the Dockerfile (and compose.yaml when the kata has one). Lines marked \"todo\" are yours to write:"
            }
            KataLanguage::Sql => {
                "2. Edit the .sql files in the kata folder. Each one that holds only a \"-- todo: ...\" comment is yours to write:"
            }
            KataLanguage::Python => {
                "2. Edit the .py modules in the kata folder. Each function that still raises NotImplementedError(\"todo ...\") is yours to write:"
            }
            KataLanguage::TypeScript => {
                "2. Edit the .ts modules in the kata folder. Each function that still throws new Error(\"todo ...\") and each type marked todo is yours to write:"
            }
        }),
        Line::from(match language {
            KataLanguage::Rust => {
                "   todo!() is a placeholder that stops the program, so its tests fail until you replace it."
            }
            KataLanguage::Java => {
                "   that throw is a placeholder, so its tests fail until you replace it."
            }
            KataLanguage::Bash => {
                "   the tests run your scripts with bats, and shellcheck must find no warnings."
            }
            KataLanguage::Docker => {
                "   the dojo builds your image on Docker Desktop, runs it with no network, then checks it with bats and hadolint."
            }
            KataLanguage::Sql => {
                "   the tests run them on PostgreSQL 17 over fixture/; try them first with the play command in README.md."
            }
            KataLanguage::Python => {
                "   that raise is a placeholder; pytest runs the tests and ruff must find no mistakes."
            }
            KataLanguage::TypeScript => {
                "   that throw is a placeholder; tsc must find no type errors, then node:test runs the tests."
            }
        }),
        Line::from("3. Save the file. The tests run by themselves and the result appears below."),
        Line::from(
            "4. When every test passes, the kata is done (+XP) and your solution is committed.",
        ),
    ]
}

/// The failure comes from code still to write: Rust's `todo!()`, or a Java kata's
/// `UnsupportedOperationException("todo ...")`, also when Spring wraps it.
fn is_placeholder(message: &str) -> bool {
    message.starts_with("not yet implemented")
        || message.starts_with("todo")
        || message.contains("UnsupportedOperationException: todo")
        || message.contains("NotImplementedError: todo")
}

/// One line per failing test, or the compiler output when the code does not build.
fn report_lines(report: &CheckReport) -> Vec<Line<'static>> {
    let mut lines = Vec::new();
    if report.outcome == CheckOutcome::CompileError || report.outcome == CheckOutcome::TimedOut {
        lines.push(Line::from(""));
        lines.extend(
            report
                .details
                .lines()
                .map(|line| Line::from(line.to_string().light_red())),
        );
        return lines;
    }
    for test in report.tests.iter().filter(|test| !test.passed) {
        let explanation = match test.message.as_deref() {
            Some(message) if is_placeholder(message) => "not written yet (still todo!)".to_string(),
            Some(message) => message.lines().collect::<Vec<_>>().join(" · "),
            None => "failed".to_string(),
        };
        lines.push(Line::from(vec![
            format!("✗ {}", test.name.replace('_', " ")).light_red(),
            format!(" — {explanation}").dark_gray(),
        ]));
    }
    lines
}

fn check_status_line(app: &App, key: &(String, String)) -> Line<'static> {
    if app.is_checking(&key.0, &key.1) {
        return Line::from("⏳ Checking in Docker…".cyan());
    }
    match app.check_history.get(key) {
        None => Line::from("🧪 No checks yet. Save a file or press [C].".dark_gray()),
        Some((record, attempts)) => {
            let label = format!(
                "🧪 Last check: {}/{} tests · {} · {attempts} attempt{}",
                record.passed_tests,
                record.total_tests,
                record.outcome.as_str().replace('_', " "),
                if *attempts == 1 { "" } else { "s" }
            );
            let color = if record.outcome == CheckOutcome::Passed {
                Color::Green
            } else {
                Color::LightRed
            };
            Line::from(Span::styled(label, Style::default().fg(color)))
        }
    }
}

fn render_summary(
    frame: &mut Frame,
    app: &App,
    track: &Track,
    phase: &Phase,
    node: &Node,
    area: Rect,
) {
    let (icon, style) = node_status(app, track, node);
    let mut lines = vec![
        Line::from(vec![
            format!(
                "{} {} · {:?} · +{} XP · ~{} min · ",
                phase.id.to_uppercase(),
                phase.title,
                node.kind,
                node_xp(app, track, node),
                node.estimated_minutes
            )
            .dark_gray(),
            Span::styled(icon, style),
        ]),
        Line::from(""),
        Line::from(node.summary.as_str()),
        Line::from(""),
        Line::from(
            format!(
                "Objectives: {}",
                node.objectives
                    .iter()
                    .map(|o| o.text.as_str())
                    .collect::<Vec<_>>()
                    .join(" · ")
            )
            .dark_gray(),
        ),
    ];
    if app.profile.is_placed_out(&track.id, &node.id) {
        let exam = app
            .profile
            .belt_exam(&track.id)
            .and_then(|id| track.node(id))
            .map_or("the Belt Exam".to_string(), |exam| exam.title.clone());
        lines.insert(
            1,
            Line::from(
                format!("⏭ Placed out by the placement test. {exam} confirms it; you can still study it now.")
                    .cyan(),
            ),
        );
    }
    let paragraph = Paragraph::new(lines)
        .wrap(Wrap { trim: true })
        .block(titled(format!(" 📖 {} ", node.title)));
    frame.render_widget(paragraph, area);
}

pub(crate) fn labelled<'a>(label: &str, text: &'a str, color: Color) -> Line<'a> {
    Line::from(vec![
        Span::styled(
            label.to_string(),
            Style::default().fg(color).add_modifier(Modifier::BOLD),
        ),
        Span::raw(text),
    ])
}

// ---- footer & selector ------------------------------------------------------

fn render_footer(frame: &mut Frame, app: &App, area: Rect) {
    let text = app.status_message.clone().unwrap_or_else(|| {
        let track = app.active_track();
        let placement = if app.profile.can_take_placement(track) {
            " | [P] Placement test"
        } else {
            ""
        };
        let hint = app
            .tree
            .selected(track)
            .and_then(|(_, node)| {
                let total = node.kata.as_ref()?.hints.len();
                crate::mentor_actions::hint_key_label(
                    app.profile.hints_revealed(&track.id, &node.id),
                    total,
                    app.profile.is_completed(&track.id, &node.id),
                )
            })
            .map_or(String::new(), |label| format!(" | {label}"));
        let due = app.profile.due_reviews(track, App::today()).len();
        let review = if due > 0 {
            format!(" | [R] Review ({due})")
        } else {
            String::new()
        };
        format!("[M] Tracks | [↑/↓] Move | [Enter] Open in editor | [Q] Quiz | [C] Check kata{hint} | [A] AI review | [/] Study a topic | [I] Interview | [B] Bridge{review} | [G] Career{placement} | [Esc] Quit")
    });
    let mode = match app.mode {
        Mode::Learner => "",
        Mode::Dev => " · DEV MODE",
    };
    let title = format!(
        " Bridge: {}{mode} ",
        crate::bridge_picker::language_name(&app.profile.settings.bridge_from)
    );
    let footer = Paragraph::new(text).block(titled(title));
    frame.render_widget(footer, area);
}

fn render_track_selector(frame: &mut Frame, app: &App, area: Rect) {
    let popup = centered(area, 86, 80);
    frame.render_widget(Clear, popup);
    let items: Vec<ListItem> = app
        .catalog
        .tracks
        .iter()
        .enumerate()
        .map(|(index, track)| selector_item(app, track, index == app.active_track_index()))
        .collect();
    let title = format!(
        " 🥋 Tracks — {} available [{}/{}] ",
        app.catalog.tracks.len(),
        app.selector_cursor + 1,
        app.catalog.tracks.len()
    );
    let list = List::new(items)
        .block(
            Block::default()
                .borders(Borders::ALL)
                .title(title)
                .border_style(Style::default().fg(Color::Cyan)),
        )
        .highlight_style(
            Style::default()
                .bg(Color::Cyan)
                .fg(Color::Black)
                .add_modifier(Modifier::BOLD),
        )
        .highlight_symbol("👉 ");
    let mut state = ListState::default().with_selected(Some(app.selector_cursor));
    frame.render_stateful_widget(list, popup, &mut state);
}

fn selector_item<'a>(app: &App, track: &'a Track, is_active: bool) -> ListItem<'a> {
    let completed = app.profile.completed_count(track);
    let marker = if is_active { "● " } else { "  " };
    ListItem::new(Line::from(vec![
        Span::raw(marker),
        Span::raw(format!("{} ", track.icon)),
        format!("{:<32} ", track.name).bold().cyan(),
        format!("[{:<26}] ", track.category).magenta(),
        format!(
            "{completed}/{} nodes ({:>4} XP)",
            track.node_count(),
            app.profile.track_xp(track)
        )
        .white(),
    ]))
}

pub(crate) fn centered(area: Rect, width_percent: u16, height_percent: u16) -> Rect {
    let [_, middle, _] = Layout::vertical([
        Constraint::Percentage((100 - height_percent) / 2),
        Constraint::Percentage(height_percent),
        Constraint::Percentage((100 - height_percent) / 2),
    ])
    .areas(area);
    let [_, center, _] = Layout::horizontal([
        Constraint::Percentage((100 - width_percent) / 2),
        Constraint::Percentage(width_percent),
        Constraint::Percentage((100 - width_percent) / 2),
    ])
    .areas(middle);
    center
}

pub(crate) fn titled<'a>(title: impl Into<Line<'a>>) -> Block<'a> {
    Block::default().borders(Borders::ALL).title(title)
}
