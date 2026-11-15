//! One run through a lesson quiz: current question, typed input, feedback and score.

use dojo_core::{Answer, Question, QuestionKind, Quiz, QuizScore};

#[derive(Debug, Clone, PartialEq, Eq)]
pub enum Stage {
    /// Waiting for an answer to the current question.
    Asking,
    /// The answer was checked; the explanation is on screen.
    Feedback { correct: bool },
    /// Every question was answered.
    Finished(QuizScore),
}

#[derive(Debug, Clone)]
pub struct QuizSession {
    pub track_id: String,
    pub node_id: String,
    pub title: String,
    pub quiz: Quiz,
    pub index: usize,
    pub correct: u32,
    /// What the learner typed for a text question.
    pub input: String,
    pub stage: Stage,
    /// Indexes of the questions answered wrong.
    pub missed: Vec<usize>,
    /// In a spaced review: the node each question comes from, by index. Empty for a
    /// lesson or checkpoint quiz.
    pub review_sources: Vec<String>,
    /// A practice quiz (a topic, or a lesson in a locked phase): nothing is recorded.
    pub practice: bool,
}

impl QuizSession {
    pub fn new(track_id: &str, node_id: &str, title: &str, quiz: Quiz) -> Self {
        Self {
            track_id: track_id.to_string(),
            node_id: node_id.to_string(),
            title: title.to_string(),
            quiz,
            index: 0,
            correct: 0,
            input: String::new(),
            stage: Stage::Asking,
            missed: Vec::new(),
            review_sources: Vec::new(),
            practice: false,
        }
    }

    /// A review of questions from several finished nodes of a track.
    pub fn review(track_id: &str, track_name: &str, quiz: Quiz, sources: Vec<String>) -> Self {
        Self {
            review_sources: sources,
            ..Self::new(track_id, "", &format!("Review: {track_name}"), quiz)
        }
    }

    /// A quiz whose result is only shown, never recorded.
    pub fn practice(title: &str, quiz: Quiz) -> Self {
        Self {
            practice: true,
            ..Self::new("", "", title, quiz)
        }
    }

    pub fn is_review(&self) -> bool {
        !self.review_sources.is_empty()
    }

    pub fn question(&self) -> &Question {
        &self.quiz.questions[self.index]
    }

    pub fn is_text_question(&self) -> bool {
        matches!(self.question().kind, QuestionKind::Text { .. })
    }

    /// Picks option `number` (1-based, as typed on the keyboard) of a single-choice question.
    pub fn choose(&mut self, number: usize) {
        if self.stage != Stage::Asking || self.is_text_question() {
            return;
        }
        if let QuestionKind::SingleChoice { options, .. } = &self.question().kind
            && (1..=options.len()).contains(&number)
        {
            self.answer(&Answer::Choice(number - 1));
        }
    }

    pub fn type_char(&mut self, character: char) {
        if self.stage == Stage::Asking && self.is_text_question() {
            self.input.push(character);
        }
    }

    pub fn backspace(&mut self) {
        if self.stage == Stage::Asking {
            self.input.pop();
        }
    }

    /// Enter: submits a typed answer, or moves on after the feedback.
    pub fn confirm(&mut self) {
        match self.stage {
            Stage::Asking if self.is_text_question() && !self.input.trim().is_empty() => {
                let typed = Answer::Text(self.input.clone());
                self.answer(&typed);
            }
            Stage::Feedback { .. } => self.next(),
            Stage::Asking | Stage::Finished(_) => {}
        }
    }

    fn answer(&mut self, answer: &Answer) {
        let correct = self.question().is_correct(answer);
        if correct {
            self.correct += 1;
        } else {
            self.missed.push(self.index);
        }
        self.stage = Stage::Feedback { correct };
    }

    fn next(&mut self) {
        self.input.clear();
        if self.index + 1 < self.quiz.questions.len() {
            self.index += 1;
            self.stage = Stage::Asking;
        } else {
            self.stage = Stage::Finished(self.quiz.score(self.correct));
        }
    }

    /// Skills of the questions answered wrong.
    pub fn missed_skills(&self) -> std::collections::HashSet<String> {
        self.missed
            .iter()
            .flat_map(|&index| self.quiz.questions[index].skills.iter().cloned())
            .collect()
    }

    pub fn score(&self) -> Option<QuizScore> {
        match self.stage {
            Stage::Finished(score) => Some(score),
            Stage::Asking | Stage::Feedback { .. } => None,
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    fn quiz() -> Quiz {
        Quiz {
            pass_percent: 50,
            questions: vec![
                Question {
                    id: "q1".into(),
                    prompt: "Pick b".into(),
                    code: None,
                    kind: QuestionKind::SingleChoice {
                        options: vec!["a".into(), "b".into()],
                        answer: 1,
                    },
                    explanation: String::new(),
                    skills: vec![],
                },
                Question {
                    id: "q2".into(),
                    prompt: "Type 42".into(),
                    code: None,
                    kind: QuestionKind::Text {
                        answers: vec!["42".into()],
                    },
                    explanation: String::new(),
                    skills: vec![],
                },
            ],
        }
    }

    #[test]
    fn answering_shows_feedback_then_moves_on_and_scores_at_the_end() {
        let mut session = QuizSession::new("rust", "rust.p0.setup", "Setup", quiz());

        session.choose(2);
        assert_eq!(session.stage, Stage::Feedback { correct: true });
        session.confirm();
        assert_eq!(session.index, 1);

        session.type_char('4');
        session.type_char('1');
        session.backspace();
        session.type_char('3');
        session.confirm();
        assert_eq!(session.stage, Stage::Feedback { correct: false });
        session.confirm();

        assert_eq!(
            session.score(),
            Some(QuizScore {
                correct: 1,
                total: 2,
                passed: true
            })
        );
    }

    #[test]
    fn invalid_keys_are_ignored() {
        let mut session = QuizSession::new("rust", "rust.p0.setup", "Setup", quiz());

        session.choose(9);
        session.type_char('x');
        session.confirm();

        assert_eq!(session.stage, Stage::Asking);
        assert!(
            session.input.is_empty(),
            "typing does nothing on a choice question"
        );
    }

    #[test]
    fn an_empty_typed_answer_is_not_submitted() {
        let mut session = QuizSession::new("rust", "rust.p0.setup", "Setup", quiz());
        session.choose(1);
        session.confirm();

        session.type_char(' ');
        session.confirm();

        assert_eq!(session.stage, Stage::Asking);
    }
}
