//! `/`: study a topic. The learner searches every track for a subject that came up at work
//! or in an interview, opens the lessons, katas and checkpoints that teach it, takes a
//! practice quiz over all of them, or copies a mock-interview prompt for an AI chat.
//! Nothing here changes progress: nodes in locked phases are practice only.

use crate::app::{App, Screen};
use crate::quiz_session::QuizSession;
use crate::ui::centered;
use dojo_core::{Node, NodeKind, Quiz, Track};
use ratatui::Frame;
use ratatui::layout::{Constraint, Layout, Rect};
use ratatui::style::{Color, Modifier, Style, Stylize};
use ratatui::text::{Line, Span};
use ratatui::widgets::{Block, Borders, Clear, List, ListItem, ListState, Paragraph, Wrap};

/// Most questions in a topic practice quiz.
pub const TOPIC_QUIZ_SIZE: usize = 12;

/// A skill of one track, with the ready nodes that teach it, in track order.
#[derive(Debug, Clone)]
pub struct Topic {
    pub skill_id: String,
    pub title: String,
    pub track_id: String,
    pub nodes: Vec<String>,
    /// Lower-case text the search matches: titles, summaries and ids.
    haystack: String,
}

/// The state of the study screen: the search, then optionally one open topic.
#[derive(Debug, Clone, Default)]
pub struct TopicStudy {
    pub query: String,
    pub cursor: usize,
    /// Index into `matches()` of the open topic.
    pub open: Option<usize>,
    pub node_cursor: usize,
    topics: Vec<Topic>,
}

impl TopicStudy {
    fn new(app: &App) -> Self {
        Self {
            topics: topics(app),
            ..Self::default()
        }
    }

    /// Topics matching every word of the query, best matches first.
    pub fn matches(&self) -> Vec<&Topic> {
        let words: Vec<String> = self
            .query
            .split_whitespace()
            .map(str::to_lowercase)
            .collect();
        let mut found: Vec<(usize, &Topic)> = self
            .topics
            .iter()
            .enumerate()
            .filter(|(_, topic)| {
                words
                    .iter()
                    .all(|word| topic.haystack.contains(word.as_str()))
            })
            .collect();
        // Topics whose own title matches come before those found through a lesson.
        found.sort_by_key(|(index, topic)| {
            let title = topic.title.to_lowercase();
            (
                !words.iter().all(|word| title.contains(word.as_str())),
                *index,
            )
        });
        found.into_iter().map(|(_, topic)| topic).collect()
    }

    pub fn open_topic(&self) -> Option<&Topic> {
        self.matches().get(self.open?).copied()
    }
}

/// Every skill taught by at least one ready node, grouped per track.
fn topics(app: &App) -> Vec<Topic> {
    let mut found = Vec::new();
    for track in &app.catalog.tracks {
        let mut skills: Vec<String> = Vec::new();
        for node in track.nodes().filter(|node| !node.under_construction()) {
            for skill in node_skills(node) {
                if !skills.contains(&skill) {
                    skills.push(skill);
                }
            }
        }
        for skill in skills {
            let nodes: Vec<&Node> = track
                .nodes()
                .filter(|node| !node.under_construction() && node_skills(node).contains(&skill))
                .collect();
            let title = app
                .catalog
                .skills
                .get(&skill)
                .map_or_else(|| skill.clone(), |s| s.title.clone());
            let mut haystack = format!("{title} {skill} {} {}", track.name, track.id);
            for node in &nodes {
                haystack.push_str(&format!(" {} {}", node.title, node.summary));
            }
            found.push(Topic {
                skill_id: skill,
                title,
                track_id: track.id.clone(),
                nodes: nodes.iter().map(|node| node.id.clone()).collect(),
                haystack: haystack.to_lowercase(),
            });
        }
    }
    found
}

/// The skills a node teaches, from its objectives.
fn node_skills(node: &Node) -> Vec<String> {
    let mut skills: Vec<String> = Vec::new();
    for skill in node.skills() {
        if !skills.iter().any(|known| known == skill) {
            skills.push(skill.to_string());
        }
    }
    skills
}

impl App {
    pub fn open_topic_study(&mut self) {
        self.topic_study = Some(TopicStudy::new(self));
        self.screen = Screen::TopicStudy;
        self.status_message = None;
    }

    pub fn close_topic_study(&mut self) {
        self.topic_study = None;
        self.screen = Screen::TrackView;
    }

    pub fn topic_type(&mut self, character: char) {
        if let Some(study) = &mut self.topic_study
            && study.open.is_none()
        {
            study.query.push(character);
            study.cursor = 0;
        }
    }

    pub fn topic_backspace(&mut self) {
        if let Some(study) = &mut self.topic_study
            && study.open.is_none()
        {
            study.query.pop();
            study.cursor = 0;
        }
    }

    pub fn topic_down(&mut self) {
        self.topic_move(1);
    }

    pub fn topic_up(&mut self) {
        self.topic_move(-1);
    }

    fn topic_move(&mut self, step: isize) {
        let Some(study) = &mut self.topic_study else {
            return;
        };
        let (count, cursor) = match study.open_topic().map(|topic| topic.nodes.len()) {
            Some(nodes) => (nodes, &mut study.node_cursor),
            None => (study.matches().len(), &mut study.cursor),
        };
        if count > 0 {
            *cursor = (*cursor as isize + step).rem_euclid(count as isize) as usize;
        }
    }

    /// Enter: opens the highlighted topic, or goes to the highlighted node of an open topic.
    pub fn topic_enter(&mut self) {
        let Some(study) = &mut self.topic_study else {
            return;
        };
        if study.open.is_none() {
            if study.cursor < study.matches().len() {
                study.open = Some(study.cursor);
                study.node_cursor = 0;
            }
            return;
        }
        let Some((track_id, node_id)) = study.open_topic().and_then(|topic| {
            Some((
                topic.track_id.clone(),
                topic.nodes.get(study.node_cursor)?.clone(),
            ))
        }) else {
            return;
        };
        self.go_to_node(&track_id, &node_id);
    }

    /// Esc: closes the open topic, or the whole screen.
    pub fn topic_back(&mut self) {
        match &mut self.topic_study {
            Some(study) if study.open.is_some() => study.open = None,
            _ => self.close_topic_study(),
        }
    }

    /// Switches to `track_id` with the cursor on `node_id`, and explains practice mode.
    pub fn go_to_node(&mut self, track_id: &str, node_id: &str) {
        let Some(index) = self.catalog.track_index(track_id) else {
            return;
        };
        if index != self.active_track_index() {
            self.selector_cursor = index;
            self.confirm_selection();
        }
        let track = &self.catalog.tracks[self.active_track_index()];
        if !self.tree.select_node(track, node_id) {
            return;
        }
        let track = self.active_track();
        let node = track.node(node_id).expect("selected above");
        let message = if self.profile.is_unlocked(track, node)
            || self.profile.is_completed(&track.id, &node.id)
        {
            format!(
                "📚 {} · {}: [Enter] opens it, [Q] quiz, [A] AI review prompt.",
                track.name, node.title
            )
        } else {
            format!(
                "📖 Study mode: {} is in a phase you have not reached yet. Read it, try its quiz ([Q]) and kata as practice: nothing is recorded until you get there.",
                node.title
            )
        };
        self.topic_study = None;
        self.screen = Screen::TrackView;
        self.status_message = Some(message);
    }

    /// Q in an open topic: a practice quiz over the questions of all its nodes.
    pub fn start_topic_quiz(&mut self) {
        let Some(topic) = self
            .topic_study
            .as_ref()
            .and_then(TopicStudy::open_topic)
            .cloned()
        else {
            return;
        };
        let Some(track) = self.catalog.track(&topic.track_id) else {
            return;
        };
        let questions: Vec<_> = topic
            .nodes
            .iter()
            .filter_map(|id| track.node(id))
            .filter_map(|node| node.quiz.as_ref())
            .flat_map(|quiz| quiz.questions.iter())
            .filter(|question| {
                question.skills.is_empty() || question.skills.contains(&topic.skill_id)
            })
            .take(TOPIC_QUIZ_SIZE)
            .cloned()
            .collect();
        if questions.is_empty() {
            self.status_message = Some("This topic has no quiz questions yet.".to_string());
            return;
        }
        let quiz = Quiz {
            pass_percent: 0,
            questions,
        };
        self.quiz = Some(QuizSession::practice(
            &format!("Practice: {}", topic.title),
            quiz,
        ));
        self.screen = Screen::Quiz;
        self.status_message = None;
    }
}

pub fn render_topic_study(frame: &mut Frame, app: &App, area: Rect) {
    let Some(study) = &app.topic_study else {
        return;
    };
    let popup = centered(area, 84, 84);
    frame.render_widget(Clear, popup);
    let block = Block::default()
        .borders(Borders::ALL)
        .title(" 📚 Study a topic ")
        .border_style(Style::default().fg(Color::Cyan));
    let inner = block.inner(popup);
    frame.render_widget(block, popup);
    match study.open_topic() {
        Some(topic) => render_topic(frame, app, study, topic, inner),
        None => render_search(frame, app, study, inner),
    }
}

fn render_search(frame: &mut Frame, app: &App, study: &TopicStudy, area: Rect) {
    let [input, list_area, help] = Layout::vertical([
        Constraint::Length(3),
        Constraint::Min(3),
        Constraint::Length(2),
    ])
    .areas(area);
    let matches = study.matches();
    let header = Paragraph::new(vec![
        Line::from(vec![
            "🔎 ".into(),
            study.query.clone().bold(),
            "▏".dark_gray(),
        ]),
        Line::from(
            format!(
                "Type a subject from work or an interview (jwt, systemd, indexes, ownership...). {} topics across every track.",
                matches.len()
            )
            .dark_gray(),
        ),
    ]);
    frame.render_widget(header, input);
    let items: Vec<ListItem> = matches
        .iter()
        .map(|topic| {
            let track = app.catalog.track(&topic.track_id);
            let (lessons, katas) = count_kinds(track, topic);
            ListItem::new(Line::from(vec![
                Span::raw(format!("{} ", track.map_or("", |t| t.icon.as_str()))),
                format!("{:<52} ", truncate(&topic.title, 52)).bold().cyan(),
                format!(
                    "{} · {lessons} lessons, {katas} katas",
                    track.map_or("", |t| t.name.as_str())
                )
                .dark_gray(),
            ]))
        })
        .collect();
    let list = List::new(items)
        .highlight_style(
            Style::default()
                .bg(Color::DarkGray)
                .add_modifier(Modifier::BOLD),
        )
        .highlight_symbol("▶ ");
    let mut state =
        ListState::default().with_selected((!matches.is_empty()).then_some(study.cursor));
    frame.render_stateful_widget(list, list_area, &mut state);
    frame.render_widget(
        Paragraph::new("[type] Search | [↑/↓] Move | [Enter] Open the topic | [Esc] Close"),
        help,
    );
}

fn render_topic(frame: &mut Frame, app: &App, study: &TopicStudy, topic: &Topic, area: Rect) {
    let Some(track) = app.catalog.track(&topic.track_id) else {
        return;
    };
    let [title, list_area, detail, help] = Layout::vertical([
        Constraint::Length(2),
        Constraint::Min(4),
        Constraint::Length(4),
        Constraint::Length(2),
    ])
    .areas(area);
    frame.render_widget(
        Paragraph::new(vec![
            Line::from(format!("{} {}", track.icon, topic.title).bold().cyan()),
            Line::from(format!("{} · {}", track.name, topic.skill_id).dark_gray()),
        ]),
        title,
    );
    let nodes: Vec<&Node> = topic.nodes.iter().filter_map(|id| track.node(id)).collect();
    let items: Vec<ListItem> = nodes
        .iter()
        .map(|node| {
            let (mark, note) = if app.profile.is_completed(&track.id, &node.id) {
                ("✓".green(), "")
            } else if app.profile.is_unlocked(track, node) {
                ("○".yellow(), "")
            } else {
                ("📖".dark_gray(), "  study only")
            };
            let kind = match node.kind {
                NodeKind::Kata => "kata",
                NodeKind::Checkpoint => "checkpoint",
                _ => "lesson",
            };
            ListItem::new(Line::from(vec![
                mark,
                format!(" {:<11}", kind).dark_gray(),
                node.title.clone().into(),
                note.dark_gray(),
            ]))
        })
        .collect();
    let list = List::new(items)
        .highlight_style(
            Style::default()
                .bg(Color::DarkGray)
                .add_modifier(Modifier::BOLD),
        )
        .highlight_symbol("▶ ");
    let mut state = ListState::default().with_selected(Some(study.node_cursor));
    frame.render_stateful_widget(list, list_area, &mut state);
    let summary = nodes
        .get(study.node_cursor)
        .map_or(String::new(), |node| node.summary.clone());
    let mut detail_lines = vec![Line::from(summary)];
    if let Some(message) = &app.status_message {
        detail_lines.push(Line::from(message.clone().yellow()));
    }
    frame.render_widget(
        Paragraph::new(detail_lines).wrap(Wrap { trim: true }),
        detail,
    );
    frame.render_widget(
        Paragraph::new(vec![
            Line::from("[Enter] Go to it | [Q] Practice quiz on the topic | [I] Mock interview on the topic | [Esc] Back"),
            Line::from("Practice only: nothing here changes your progress.".dark_gray()),
        ]),
        help,
    );
}

fn count_kinds(track: Option<&Track>, topic: &Topic) -> (usize, usize) {
    let Some(track) = track else { return (0, 0) };
    let nodes: Vec<&Node> = topic.nodes.iter().filter_map(|id| track.node(id)).collect();
    let katas = nodes.iter().filter(|node| node.kata.is_some()).count();
    (nodes.len() - katas, katas)
}

fn truncate(text: &str, max: usize) -> String {
    if text.chars().count() <= max {
        return text.to_string();
    }
    let cut: String = text.chars().take(max.saturating_sub(1)).collect();
    format!("{cut}…")
}
