use crate::app::{App, Screen};
use crate::tree::TreeRow;
use dojo_core::{CheckOutcome, Kata, Mode, Node, NodeKind, Phase, Track};
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
    if app.screen == Screen::TrackSelector {
        render_track_selector(frame, app, frame.area());
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
        format!("  +{} XP", node.xp).dark_gray(),
    ]))
}

fn node_status(app: &App, track: &Track, node: &Node) -> (&'static str, Style) {
    if app.profile.is_completed(&track.id, &node.id) {
        (
            "✓",
            Style::default()
                .fg(Color::Green)
                .add_modifier(Modifier::BOLD),
        )
    } else if !app.profile.is_unlocked(track, node) {
        ("🔒", Style::default().fg(Color::DarkGray))
    } else if app.is_checking(&track.id, &node.id) {
        ("⏳", Style::default().fg(Color::Cyan))
    } else if kata_started(app, track, node) {
        ("◐", Style::default().fg(Color::Yellow))
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
    let [summary_area, bridge_area] =
        Layout::vertical([Constraint::Percentage(40), Constraint::Percentage(60)]).areas(area);
    render_summary(frame, app, track, phase, node, summary_area);
    match &node.kata {
        Some(kata) => render_kata(frame, app, track, phase, node, kata, bridge_area),
        None => render_bridge(frame, app, node, bridge_area),
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
    let folder = dir.display().to_string();
    let mut lines = vec![if dir.exists() {
        labelled("📁 Folder: ", &folder, Color::Cyan)
    } else {
        Line::from("📁 Not started yet — press [Enter] to create your kata folder.".yellow())
    }];
    lines.push(check_status_line(app, &key));
    if let Some(report) = app
        .last_reports
        .get(&key)
        .filter(|report| !report.details.is_empty())
    {
        lines.push(Line::from(""));
        lines.extend(
            report
                .details
                .lines()
                .map(|line| Line::from(line.to_string().light_red())),
        );
    }
    lines.push(Line::from(""));
    lines.extend(
        kata.statement
            .lines()
            .map(|line| Line::from(line.to_string())),
    );
    let paragraph = Paragraph::new(lines)
        .wrap(Wrap { trim: false })
        .block(titled(" 🥋 Kata "));
    frame.render_widget(paragraph, area);
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
    let lines = vec![
        Line::from(vec![
            format!(
                "{} {} · {:?} · +{} XP · ~{} min · ",
                phase.id.to_uppercase(),
                phase.title,
                node.kind,
                node.xp,
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
    let paragraph = Paragraph::new(lines)
        .wrap(Wrap { trim: true })
        .block(titled(format!(" 📖 {} ", node.title)));
    frame.render_widget(paragraph, area);
}

fn render_bridge(frame: &mut Frame, app: &App, node: &Node, area: Rect) {
    let source = &app.profile.settings.bridge_from;
    let Some(bridge) = node.bridge_for(source) else {
        let missing = Paragraph::new(format!(
            "No bridge from {source} for this node yet. Press [B] to change language."
        ))
        .wrap(Wrap { trim: true })
        .block(titled(" 🥋 Rosetta Bridge "));
        frame.render_widget(missing, area);
        return;
    };
    let mut lines = vec![
        labelled("🎯 Key concept: ", &bridge.concept, Color::Cyan),
        Line::from(""),
        labelled(
            &format!("↩ The {} way: ", bridge.from),
            &bridge.from_way,
            Color::LightRed,
        ),
        Line::from(""),
        labelled(
            "🚀 The way of this track: ",
            &bridge.native_way,
            Color::LightGreen,
        ),
        Line::from(""),
        labelled("💡 Sensei tip: ", &bridge.sensei_tip, Color::Yellow),
    ];
    if !bridge.body.is_empty() {
        lines.push(Line::from(""));
        lines.extend(bridge.body.lines().map(Line::from));
    }
    let paragraph = Paragraph::new(lines)
        .wrap(Wrap { trim: true })
        .block(titled(format!(
            " 🥋 Rosetta Bridge (from {}) ",
            bridge.from
        )));
    frame.render_widget(paragraph, area);
}

fn labelled<'a>(label: &str, text: &'a str, color: Color) -> Line<'a> {
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
        "[M] Tracks | [↑/↓] Move | [Enter] Start kata / mark lesson | [C] Check | [O] Open folder | [B] Bridge | [q] Quit"
            .to_string()
    });
    let mode = match app.mode {
        Mode::Learner => "",
        Mode::Dev => " · DEV MODE",
    };
    let title = format!(" Bridge: {}{mode} ", app.profile.settings.bridge_from);
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

fn centered(area: Rect, width_percent: u16, height_percent: u16) -> Rect {
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

fn titled<'a>(title: impl Into<Line<'a>>) -> Block<'a> {
    Block::default().borders(Borders::ALL).title(title)
}
