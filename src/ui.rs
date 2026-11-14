use crate::state::{AppMode, AppState};
use ratatui::{
    layout::{Alignment, Constraint, Direction, Layout, Rect},
    style::{Color, Modifier, Style},
    text::{Line, Span},
    widgets::{Block, Borders, Clear, Gauge, List, ListItem, Paragraph, Wrap},
    Frame,
};

pub fn render(frame: &mut Frame, state: &AppState) {
    let chunks = Layout::default()
        .direction(Direction::Vertical)
        .constraints([
            Constraint::Length(4), // Header
            Constraint::Min(10),   // Corpo (Tree + Detalhes)
            Constraint::Length(3), // Footer
        ])
        .split(frame.area());

    render_header(frame, state, chunks[0]);
    render_body(frame, state, chunks[1]);
    render_footer(frame, state, chunks[2]);

    if state.mode == AppMode::TrackSelector {
        render_track_selector(frame, state, frame.area());
    }
}

fn render_header(frame: &mut Frame, state: &AppState, area: Rect) {
    let header_chunks = Layout::default()
        .direction(Direction::Horizontal)
        .constraints([
            Constraint::Percentage(45),
            Constraint::Percentage(35),
            Constraint::Percentage(20),
        ])
        .split(area);

    let track = state.active_track();
    let current_belt = state.profile.current_belt(track);
    let next_belt = state.profile.next_belt(track);
    let track_xp = state.profile.track_xp(track);

    let (xp_ratio, next_xp_label) = if let Some(next) = next_belt {
        let span = next.required_xp - current_belt.required_xp;
        let progress = track_xp.saturating_sub(current_belt.required_xp);
        let ratio = if span > 0 {
            (progress as f64 / span as f64).clamp(0.0, 1.0)
        } else {
            1.0
        };
        (ratio, format!("{}/{} XP (Próxima: {})", track_xp, next.required_xp, next.name))
    } else {
        (1.0, format!("{} XP (Grau Máximo!)", track_xp))
    };

    let title_text = format!(" {} {} [Pressione 'M' p/ trocar]", track.icon, track.name);
    let player_info = vec![
        Line::from(vec![
            Span::styled(title_text, Style::default().add_modifier(Modifier::BOLD).fg(Color::Cyan)),
        ]),
        Line::from(vec![
            Span::raw("🥋 "),
            Span::styled(&state.profile.name, Style::default().add_modifier(Modifier::BOLD)),
            Span::raw(" | "),
            Span::styled(format!("{} [{}]", current_belt.name, current_belt.title), Style::default().fg(Color::Yellow)),
        ]),
    ];

    let player_widget = Paragraph::new(player_info)
        .block(Block::default().borders(Borders::ALL).title("⛩️ Tatame Ativo"));
    frame.render_widget(player_widget, header_chunks[0]);

    let xp_gauge = Gauge::default()
        .block(Block::default().borders(Borders::ALL).title("⚡ XP na Trilha"))
        .gauge_style(Style::default().fg(Color::Magenta).bg(Color::DarkGray))
        .ratio(xp_ratio)
        .label(next_xp_label);
    frame.render_widget(xp_gauge, header_chunks[1]);

    let streak_text = vec![
        Line::from(vec![
            Span::styled(" 🔥 STREAK ", Style::default().add_modifier(Modifier::BOLD).fg(Color::Red)),
            Span::styled(format!("{} dias", state.profile.current_streak), Style::default().add_modifier(Modifier::BOLD).fg(Color::Yellow)),
        ]),
        Line::from(vec![
            Span::raw(" Total XP Geral: "),
            Span::styled(format!("{}", state.profile.total_xp), Style::default().fg(Color::Green).add_modifier(Modifier::BOLD)),
        ]),
    ];
    let streak_widget = Paragraph::new(streak_text)
        .block(Block::default().borders(Borders::ALL).title("Constância Global"));
    frame.render_widget(streak_widget, header_chunks[2]);
}

fn render_body(frame: &mut Frame, state: &AppState, area: Rect) {
    let body_chunks = Layout::default()
        .direction(Direction::Horizontal)
        .constraints([
            Constraint::Percentage(40), // Lista de nós
            Constraint::Percentage(60), // Detalhes + Rosetta
        ])
        .split(area);

    let track = state.active_track();

    let items: Vec<ListItem> = track
        .nodes
        .iter()
        .enumerate()
        .map(|(idx, node)| {
            let is_completed = state.profile.is_node_completed(&track.id, &node.id);
            let is_unlocked = state.is_unlocked(idx);

            let (icon, style) = if is_completed {
                (" [★]", Style::default().fg(Color::Green).add_modifier(Modifier::BOLD))
            } else if !is_unlocked {
                (" [🔒]", Style::default().fg(Color::DarkGray))
            } else {
                (" [ ]", Style::default().fg(Color::Yellow))
            };

            let line = Line::from(vec![
                Span::styled(icon, style),
                Span::raw(" "),
                Span::styled(&node.title, if is_completed {
                    Style::default().fg(Color::White)
                } else if !is_unlocked {
                    Style::default().fg(Color::DarkGray)
                } else {
                    Style::default().fg(Color::LightCyan)
                }),
                Span::styled(format!(" (+{} XP)", node.xp), Style::default().fg(Color::DarkGray)),
            ]);

            ListItem::new(line)
        })
        .collect();

    let list = List::new(items)
        .block(Block::default().borders(Borders::ALL).title(format!("🗺️ Skill Tree: {}", track.name)))
        .highlight_style(
            Style::default()
                .bg(Color::Rgb(40, 44, 52))
                .fg(Color::Cyan)
                .add_modifier(Modifier::BOLD),
        )
        .highlight_symbol("▶ ");

    let mut list_state = ratatui::widgets::ListState::default();
    list_state.select(Some(state.selected_node_idx));
    frame.render_stateful_widget(list, body_chunks[0], &mut list_state);

    let detail_chunks = Layout::default()
        .direction(Direction::Vertical)
        .constraints([
            Constraint::Length(7), // Resumo do Kata
            Constraint::Min(8),    // Rosetta Stone
        ])
        .split(body_chunks[1]);

    if let Some(node) = track.nodes.get(state.selected_node_idx) {
        let is_completed = state.profile.is_node_completed(&track.id, &node.id);
        let status_span = if is_completed {
            Span::styled("DOMINADO ⭐", Style::default().fg(Color::Green).add_modifier(Modifier::BOLD))
        } else if !state.is_unlocked(state.selected_node_idx) {
            Span::styled("BLOQUEADO 🔒", Style::default().fg(Color::Red))
        } else {
            Span::styled("EM TREINO ⚔️", Style::default().fg(Color::Yellow))
        };

        let kata_summary = vec![
            Line::from(vec![
                Span::styled(format!("Seção: {} | Recompensa: +{} XP | Status: ", node.section, node.xp), Style::default().fg(Color::DarkGray)),
                status_span,
            ]),
            Line::from(""),
            Line::from(vec![
                Span::styled(&node.description, Style::default().fg(Color::White)),
            ]),
        ];

        let kata_block = Paragraph::new(kata_summary)
            .block(Block::default().borders(Borders::ALL).title(format!("📖 Kata: {}", node.title)))
            .wrap(Wrap { trim: true });
        frame.render_widget(kata_block, detail_chunks[0]);

        let rosetta = &node.rosetta;
        let rosetta_lines = vec![
            Line::from(vec![
                Span::styled("🎯 Conceito Chave: ", Style::default().fg(Color::Cyan).add_modifier(Modifier::BOLD)),
                Span::styled(&rosetta.concept, Style::default().fg(Color::White)),
            ]),
            Line::from(""),
            Line::from(vec![
                Span::styled("☕ Ponto de Partida (Java/Tradicional): ", Style::default().fg(Color::LightRed).add_modifier(Modifier::BOLD)),
                Span::raw(&rosetta.java_way),
            ]),
            Line::from(""),
            Line::from(vec![
                Span::styled("🚀 O Jeito Desta Trilha: ", Style::default().fg(Color::LightGreen).add_modifier(Modifier::BOLD)),
                Span::raw(&rosetta.rust_way),
            ]),
            Line::from(""),
            Line::from(vec![
                Span::styled("💡 Conselho do Sensei: ", Style::default().fg(Color::Yellow).add_modifier(Modifier::BOLD)),
                Span::styled(&rosetta.sensei_tip, Style::default().fg(Color::LightYellow)),
            ]),
        ];

        let rosetta_block = Paragraph::new(rosetta_lines)
            .block(Block::default().borders(Borders::ALL).title("🥋 Tradução Mental & Boas Práticas"))
            .wrap(Wrap { trim: true });
        frame.render_widget(rosetta_block, detail_chunks[1]);
    }
}

fn render_footer(frame: &mut Frame, state: &AppState, area: Rect) {
    let msg = state.status_message.as_deref().unwrap_or(
        "[M] Menu de Trilhas | [↑/↓] Katas | [Espaço / Enter] Dominar Kata | [q / Esc] Sair"
    );

    let footer = Paragraph::new(msg)
        .block(Block::default().borders(Borders::ALL).title("Comandos do Tatame"))
        .style(Style::default().fg(Color::Cyan));
    frame.render_widget(footer, area);
}

// Modal para selecionar trilhas
fn render_track_selector(frame: &mut Frame, state: &AppState, area: Rect) {
    // Cria uma área centralizada de 70% de largura e 70% de altura
    let popup_area = centered_rect(75, 75, area);

    frame.render_widget(Clear, popup_area); // Limpa o fundo atrás da modal

    let items: Vec<ListItem> = state
        .tracks
        .iter()
        .enumerate()
        .map(|(idx, track)| {
            let is_current = idx == state.current_track_idx;
            let xp = state.profile.track_xp(track);
            let belt = state.profile.current_belt(track);
            let total_nodes = track.nodes.len();
            let completed = state
                .profile
                .track_progress
                .get(&track.id)
                .map(|s| s.len())
                .unwrap_or(0);

            let prefix = if is_current { "▶ [ATIVA] " } else { "  " };

            let line = Line::from(vec![
                Span::styled(prefix, Style::default().fg(Color::Yellow).add_modifier(Modifier::BOLD)),
                Span::styled(format!("{} ", track.icon), Style::default().fg(Color::White)),
                Span::styled(format!("{:<25} ", track.name), Style::default().fg(Color::Cyan).add_modifier(Modifier::BOLD)),
                Span::styled(format!("[{}] ", track.category), Style::default().fg(Color::Magenta)),
                Span::styled(format!("{}/{} Katas ({} XP) ", completed, total_nodes, xp), Style::default().fg(Color::White)),
                Span::styled(format!("Faixa: {}", belt.name), Style::default().fg(Color::Yellow)),
            ]);

            ListItem::new(line)
        })
        .collect();

    let list = List::new(items)
        .block(
            Block::default()
                .borders(Borders::ALL)
                .title("🥋 SELETOR DE JORNADAS (ROADMAP.SH) — Escolha seu Tatame")
                .title_alignment(Alignment::Center)
                .style(Style::default().bg(Color::Rgb(20, 24, 30))),
        )
        .highlight_style(
            Style::default()
                .bg(Color::Rgb(50, 60, 80))
                .fg(Color::Yellow)
                .add_modifier(Modifier::BOLD),
        )
        .highlight_symbol("👉 ");

    let mut list_state = ratatui::widgets::ListState::default();
    list_state.select(Some(state.selector_cursor_idx));
    frame.render_stateful_widget(list, popup_area, &mut list_state);
}

fn centered_rect(percent_x: u16, percent_y: u16, r: Rect) -> Rect {
    let popup_layout = Layout::default()
        .direction(Direction::Vertical)
        .constraints([
            Constraint::Percentage((100 - percent_y) / 2),
            Constraint::Percentage(percent_y),
            Constraint::Percentage((100 - percent_y) / 2),
        ])
        .split(r);

    Layout::default()
        .direction(Direction::Horizontal)
        .constraints([
            Constraint::Percentage((100 - percent_x) / 2),
            Constraint::Percentage(percent_x),
            Constraint::Percentage((100 - percent_x) / 2),
        ])
        .split(popup_layout[1])[1]
}
