//! I: a mock technical interview. The learner answers each question out loud or in notes,
//! reveals what a strong answer covers, rates themself, and gets a summary with what to
//! study. The answers can be written in a file (W opens it); E turns them into a prompt that
//! asks an AI to evaluate them, and P copies the interview for an AI interviewer instead.
//! Nothing is recorded in the progress.

use crate::app::{App, Screen};
use crate::topic_study::TopicStudy;
use crate::ui::centered;
use chrono::Datelike;
use dojo_core::{InterviewLevel, InterviewQuestion, Track};
use ratatui::Frame;
use ratatui::layout::Rect;
use ratatui::style::{Color, Style, Stylize};
use ratatui::text::Line;
use ratatui::widgets::{Block, Borders, Clear, Paragraph, Wrap};
use std::fs;
use std::path::PathBuf;

/// Questions in one mock interview.
pub const INTERVIEW_SIZE: usize = 8;

/// The levels offered in the picker; `None` mixes every level.
pub const LEVEL_CHOICES: [Option<InterviewLevel>; 4] = [
    Some(InterviewLevel::Junior),
    Some(InterviewLevel::Mid),
    Some(InterviewLevel::Senior),
    None,
];

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum Rating {
    Covered,
    Partly,
    Missed,
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub enum Stage {
    /// Choosing the level (track interviews only).
    Picking {
        cursor: usize,
    },
    Asking,
    Revealed,
    Done,
}

#[derive(Debug, Clone)]
pub struct InterviewSession {
    pub track_id: String,
    pub title: String,
    pub questions: Vec<InterviewQuestion>,
    pub index: usize,
    pub ratings: Vec<Rating>,
    pub stage: Stage,
    /// The Markdown file where the learner writes the answers, once the questions are known.
    pub answers_file: Option<PathBuf>,
}

impl InterviewSession {
    pub fn question(&self) -> Option<&InterviewQuestion> {
        self.questions.get(self.index)
    }
}

/// Up to `INTERVIEW_SIZE` questions of `level` (or all), starting at a place that changes
/// every day so repeated interviews do not always ask the same questions first.
pub fn pick_questions(
    track: &Track,
    level: Option<InterviewLevel>,
    day: u32,
) -> Vec<InterviewQuestion> {
    let Some(bank) = &track.interview else {
        return Vec::new();
    };
    let mut pool: Vec<InterviewQuestion> = bank
        .questions
        .iter()
        .filter(|question| level.is_none_or(|level| question.level == level))
        .cloned()
        .collect();
    if pool.is_empty() {
        return pool;
    }
    let offset = day as usize % pool.len();
    pool.rotate_left(offset);
    pool.truncate(INTERVIEW_SIZE);
    pool.sort_by_key(|question| question.level);
    pool
}

/// The interview questions about one skill, plus the lessons' written exercises when the
/// bank has few, so every topic can be practised.
pub fn topic_questions(track: &Track, skill: &str, node_ids: &[String]) -> Vec<InterviewQuestion> {
    let mut questions: Vec<InterviewQuestion> = track
        .interview
        .iter()
        .flat_map(|bank| bank.questions.iter())
        .filter(|question| question.skills.iter().any(|s| s == skill))
        .cloned()
        .collect();
    if questions.len() < 3 {
        for node in node_ids.iter().filter_map(|id| track.node(id)) {
            if let Some(exercise) = &node.exercise {
                questions.push(InterviewQuestion {
                    id: format!("exercise-{}", node.id),
                    level: InterviewLevel::Mid,
                    skills: vec![skill.to_string()],
                    question: exercise.task.trim().to_string(),
                    key_points: exercise.criteria.clone(),
                    follow_up: None,
                });
            }
        }
    }
    questions.sort_by_key(|question| question.level);
    questions.truncate(INTERVIEW_SIZE);
    questions
}

impl App {
    /// I on the track view: pick a level, then the interview starts.
    pub fn open_interview(&mut self) {
        let track = self.active_track();
        if track
            .interview
            .as_ref()
            .is_none_or(|bank| bank.questions.is_empty())
        {
            self.status_message = Some(format!(
                "🎤 {} has no interview questions yet. Press [/] to study a topic instead.",
                track.name
            ));
            return;
        }
        self.interview = Some(InterviewSession {
            track_id: track.id.clone(),
            title: format!("Mock interview: {}", track.name),
            questions: Vec::new(),
            index: 0,
            ratings: Vec::new(),
            stage: Stage::Picking { cursor: 1 },
            answers_file: None,
        });
        self.screen = Screen::Interview;
        self.status_message = None;
    }

    /// I in an open topic: an interview on that topic only.
    pub fn start_topic_interview(&mut self) {
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
        let questions = topic_questions(track, &topic.skill_id, &topic.nodes);
        if questions.is_empty() {
            self.status_message = Some("This topic has no interview questions yet.".to_string());
            return;
        }
        self.interview = Some(InterviewSession {
            track_id: track.id.clone(),
            title: format!("Mock interview: {}", topic.title),
            questions,
            index: 0,
            ratings: Vec::new(),
            stage: Stage::Asking,
            answers_file: None,
        });
        self.screen = Screen::Interview;
        self.create_answers_file();
    }

    pub fn interview_up(&mut self) {
        if let Some(InterviewSession {
            stage: Stage::Picking { cursor },
            ..
        }) = &mut self.interview
        {
            *cursor = (*cursor + LEVEL_CHOICES.len() - 1) % LEVEL_CHOICES.len();
        }
    }

    pub fn interview_down(&mut self) {
        if let Some(InterviewSession {
            stage: Stage::Picking { cursor },
            ..
        }) = &mut self.interview
        {
            *cursor = (*cursor + 1) % LEVEL_CHOICES.len();
        }
    }

    /// Enter: confirms the level, reveals the answer, or closes the summary.
    pub fn interview_enter(&mut self) {
        let day = Self::today().ordinal();
        let Some(session) = &mut self.interview else {
            return;
        };
        let mut started = false;
        match session.stage.clone() {
            Stage::Picking { cursor } => {
                let Some(track) = self.catalog.track(&session.track_id) else {
                    return;
                };
                let level = LEVEL_CHOICES[cursor];
                session.questions = pick_questions(track, level, day);
                if let Some(level) = level {
                    session.title = format!("{} · {}", session.title, level.label());
                }
                session.stage = Stage::Asking;
                started = true;
            }
            Stage::Asking => session.stage = Stage::Revealed,
            Stage::Revealed => {}
            Stage::Done => self.close_interview(),
        }
        if started {
            self.create_answers_file();
        }
    }

    /// Writes the answers file: every question with a marked place for its answer.
    fn create_answers_file(&mut self) {
        let Some(session) = &mut self.interview else {
            return;
        };
        let stamp = chrono::Local::now().format("%Y%m%d-%H%M%S");
        let path = self
            .workspace
            .root()
            .join(".dojo")
            .join("interviews")
            .join(format!("{}-{stamp}.md", session.track_id));
        let written = path
            .parent()
            .map_or(Ok(()), fs::create_dir_all)
            .and_then(|()| fs::write(&path, answers_template(&session.title, &session.questions)));
        match written {
            Ok(()) => session.answers_file = Some(path),
            Err(error) => {
                self.status_message = Some(format!("⚠ Could not create the answers file: {error}"));
            }
        }
    }

    /// W: opens the answers file in the editor.
    pub fn open_interview_answers(&mut self) {
        let Some(path) = self.interview.as_ref().and_then(|s| s.answers_file.clone()) else {
            return;
        };
        self.status_message = Some(match self.editor.open_file(&path) {
            Ok(()) => format!(
                "✍ Write each answer below its question in {} and save.",
                path.display()
            ),
            Err(error) => format!("✍ Write your answers in {} ({error}).", path.display()),
        });
    }

    /// E: an evaluation prompt with the learner's written answers, the key points and the
    /// self-ratings, for an AI to grade.
    pub fn interview_evaluation_prompt(&mut self) {
        let Some(session) = &self.interview else {
            return;
        };
        let Some(path) = session.answers_file.clone() else {
            return;
        };
        let text = fs::read_to_string(&path).unwrap_or_default();
        let answers = answers_from(&text, session.questions.len());
        if answers.iter().all(Option::is_none) {
            self.status_message = Some(format!(
                "✍ Write your answers first: [W] opens {}. Then press [E] again.",
                path.display()
            ));
            return;
        }
        let track_name = self
            .catalog
            .track(&session.track_id)
            .map_or(String::new(), |track| track.name.clone());
        let prompt = evaluation_prompt(
            &session.title,
            &track_name,
            &session.questions,
            &answers,
            &session.ratings,
        );
        let key = format!("interview-evaluation-{}", session.track_id);
        let written = answers.iter().filter(|answer| answer.is_some()).count();
        let extra = format!(" It holds your {written} written answers.");
        self.status_message = Some(self.deliver_prompt(&key, &prompt, &extra));
    }

    /// 1, 2 or 3 after the answer is revealed.
    pub fn interview_rate(&mut self, rating: Rating) {
        let Some(session) = &mut self.interview else {
            return;
        };
        if session.stage != Stage::Revealed {
            return;
        }
        session.ratings.push(rating);
        session.index += 1;
        session.stage = if session.index >= session.questions.len() {
            Stage::Done
        } else {
            Stage::Asking
        };
    }

    pub fn close_interview(&mut self) {
        self.interview = None;
        self.screen = Screen::TrackView;
    }

    /// P: the whole interview as a prompt for an AI interviewer.
    pub fn interview_prompt(&mut self) {
        let Some(session) = &self.interview else {
            return;
        };
        if session.questions.is_empty() {
            return;
        }
        let track_name = self
            .catalog
            .track(&session.track_id)
            .map_or(String::new(), |track| track.name.clone());
        let prompt = ai_interviewer_prompt(&session.title, &track_name, &session.questions);
        let key = format!("interview-{}", session.track_id);
        self.status_message = Some(self.deliver_prompt(&key, &prompt, ""));
    }

    /// A lesson to study for a question: the first ready node of the track with its skill.
    fn lesson_for(&self, track_id: &str, question: &InterviewQuestion) -> Option<String> {
        let track = self.catalog.track(track_id)?;
        track
            .nodes()
            .filter(|node| !node.under_construction())
            .find(|node| {
                node.skills()
                    .any(|skill| question.skills.iter().any(|s| s == skill))
            })
            .map(|node| node.title.clone())
    }
}

fn answer_marker(number: usize) -> String {
    format!("<!-- Answer {number} below this line -->")
}

/// The answers file: the questions in order, each with a marked place for its answer.
pub fn answers_template(title: &str, questions: &[InterviewQuestion]) -> String {
    let mut text = format!(
        "# {title}\n\nWrite each answer below its question as you would say it in the interview, save the file, then press [E] in the dojo for a prompt that asks an AI to evaluate your answers.\n"
    );
    for (index, question) in questions.iter().enumerate() {
        let number = index + 1;
        text.push_str(&format!(
            "\n## Question {number} ({})\n\n{}\n\n{}\n\n",
            question.level.label(),
            question.question,
            answer_marker(number)
        ));
    }
    text
}

/// The learner's answers, by question; `None` where nothing was written.
pub fn answers_from(text: &str, count: usize) -> Vec<Option<String>> {
    (1..=count)
        .map(|number| {
            let marker = answer_marker(number);
            let (_, after) = text.split_once(&marker)?;
            let answer = after.split("\n## Question ").next().unwrap_or("").trim();
            (!answer.is_empty()).then(|| answer.to_string())
        })
        .collect()
}

/// Asks an AI to grade written answers against the key points, and the self-ratings too.
pub fn evaluation_prompt(
    title: &str,
    track: &str,
    questions: &[InterviewQuestion],
    answers: &[Option<String>],
    ratings: &[Rating],
) -> String {
    let mut body = String::new();
    for (index, question) in questions.iter().enumerate() {
        let rating = match ratings.get(index) {
            Some(Rating::Covered) => "I think I covered it",
            Some(Rating::Partly) => "I think I covered it partly",
            Some(Rating::Missed) => "I think I missed it",
            None => "not rated",
        };
        let answer = answers
            .get(index)
            .and_then(Option::as_deref)
            .unwrap_or("(no written answer)");
        body.push_str(&format!(
            "### Question {} [{}]\n\n{}\n\nKey points of a strong answer: {}\n\nMy self-rating: {rating}\n\nMy answer:\n\n{answer}\n\n",
            index + 1,
            question.level.label(),
            question.question,
            question.key_points.join("; ")
        ));
    }
    format!(
        r#"You are a senior interviewer evaluating my written answers from a mock technical interview ({title}, {track}). For each question below you have the question, the key points a strong answer covers, my answer and how I rated myself.

{body}Evaluate my answers:
1. For each question: a grade (strong, partial, weak or no answer), what I got right, what is missing or wrong (use the key points and your own knowledge; they are not exhaustive), and a short model answer in two or three sentences.
2. Say where my self-rating was too generous or too harsh.
3. Finish with the level these answers show (junior, mid-level or senior) and the three topics I should study first, most important first.

Be honest and specific: an interviewer would not accept vague answers, and neither should you."#
    )
}

/// Turns any AI chat into an interviewer that asks these questions, follows up and grades
/// against the key points.
pub fn ai_interviewer_prompt(title: &str, track: &str, questions: &[InterviewQuestion]) -> String {
    let mut list = String::new();
    for (number, question) in questions.iter().enumerate() {
        list.push_str(&format!(
            "{}. [{}] {}\n   A strong answer covers: {}\n",
            number + 1,
            question.level.label(),
            question.question,
            question.key_points.join("; ")
        ));
        if let Some(follow_up) = &question.follow_up {
            list.push_str(&format!("   Possible follow-up: {follow_up}\n"));
        }
    }
    format!(
        r#"You are an experienced interviewer running a technical interview ({title}, {track}). Interview me with the questions below, in order.

{list}
Rules:
1. Ask one question at a time and wait for my answer. Do not show me the key points.
2. After each answer, ask one or two follow-ups that probe reasoning, trade-offs or edge cases, as a real interviewer would. Adapt them to what I said.
3. Grade each answer privately against its key points as strong, partial or missing.
4. When the questions are done, or when I write "end", give me the results: for each question the grade, what I explained well and what was missing, then the three topics I should study first.
5. Be honest and specific; vague answers should not pass.

Start with question 1."#
    )
}

pub fn render_interview(frame: &mut Frame, app: &App, area: Rect) {
    let Some(session) = &app.interview else {
        return;
    };
    let popup = centered(area, 84, 84);
    frame.render_widget(Clear, popup);
    let block = Block::default()
        .borders(Borders::ALL)
        .title(format!(" 🎤 {} ", session.title))
        .border_style(Style::default().fg(Color::Magenta));
    let inner = block.inner(popup);
    frame.render_widget(block, popup);
    let mut lines: Vec<Line> = Vec::new();
    match &session.stage {
        Stage::Picking { cursor } => {
            lines.push(Line::from("Which level are you preparing for?".bold()));
            lines.push(Line::from(""));
            for (index, choice) in LEVEL_CHOICES.iter().enumerate() {
                let label = choice.map_or("Mixed (all levels)", InterviewLevel::label);
                let marker = if index == *cursor { "▶ " } else { "  " };
                let line = Line::from(format!("{marker}{label}"));
                lines.push(if index == *cursor {
                    line.cyan().bold()
                } else {
                    line
                });
            }
            lines.push(Line::from(""));
            lines.push(Line::from(
                format!("{INTERVIEW_SIZE} questions. Answer out loud or in notes, as in a real interview, then compare with what a strong answer covers.").dark_gray(),
            ));
            lines.push(Line::from(""));
            lines.push(Line::from("[↑/↓] Choose | [Enter] Start | [Esc] Cancel"));
        }
        Stage::Asking | Stage::Revealed => {
            let Some(question) = session.question() else {
                return;
            };
            lines.push(Line::from(
                format!(
                    "Question {}/{} · {} · {}",
                    session.index + 1,
                    session.questions.len(),
                    question.level.label(),
                    question.skills.join(", ")
                )
                .dark_gray(),
            ));
            lines.push(Line::from(""));
            lines.push(Line::from(question.question.clone().bold()));
            lines.push(Line::from(""));
            if session.stage == Stage::Asking {
                lines.push(Line::from(
                    "Answer out loud, or write it below this question in the answers file ([W] opens it). Then press [Space] to see what a strong answer covers.".dark_gray(),
                ));
                lines.push(Line::from(""));
                lines.push(Line::from(
                    "[Space] Reveal | [W] Answers file | [E] Evaluation prompt | [P] AI interviewer prompt | [Esc] Stop",
                ));
            } else {
                lines.push(Line::from("A strong answer covers:".green().bold()));
                for point in &question.key_points {
                    lines.push(Line::from(format!("  • {point}")));
                }
                if let Some(follow_up) = &question.follow_up {
                    lines.push(Line::from(""));
                    lines.push(Line::from(
                        format!("🎙 Interviewer's follow-up: {follow_up}").yellow(),
                    ));
                }
                lines.push(Line::from(""));
                lines.push(Line::from(
                    "How did you do?  [1] I covered it   [2] Partly   [3] I missed it",
                ));
            }
        }
        Stage::Done => {
            let count = |rating| session.ratings.iter().filter(|r| **r == rating).count();
            lines.push(Line::from("Interview finished".bold()));
            lines.push(Line::from(format!(
                "✅ covered {}   ◐ partly {}   ✗ missed {}",
                count(Rating::Covered),
                count(Rating::Partly),
                count(Rating::Missed)
            )));
            lines.push(Line::from(""));
            let to_study: Vec<_> = session
                .questions
                .iter()
                .zip(&session.ratings)
                .filter(|(_, rating)| **rating != Rating::Covered)
                .collect();
            if to_study.is_empty() {
                lines.push(Line::from("Nothing to review: try a harder level, or [P] for an AI interviewer that pushes further.".green()));
            } else {
                lines.push(Line::from("Study next:".yellow().bold()));
                for (question, rating) in to_study {
                    let mark = if *rating == Rating::Missed {
                        "✗"
                    } else {
                        "◐"
                    };
                    let lesson = app
                        .lesson_for(&session.track_id, question)
                        .map_or(String::new(), |title| format!("  → lesson: {title}"));
                    lines.push(Line::from(format!(
                        "  {mark} {}{lesson}",
                        question.question
                    )));
                }
            }
            lines.push(Line::from(""));
            lines.push(Line::from(
                "Practice only: nothing was recorded. [/] on the track finds a topic to study."
                    .dark_gray(),
            ));
            lines.push(Line::from(
                "[E] Prompt to evaluate your written answers | [W] Answers file | [P] AI interviewer prompt | [Enter] Close",
            ));
        }
    }
    if let Some(message) = &app.status_message {
        lines.push(Line::from(""));
        lines.push(Line::from(message.clone().yellow()));
    }
    frame.render_widget(Paragraph::new(lines).wrap(Wrap { trim: false }), inner);
}
