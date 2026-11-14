//! Quizzes: the evidence that completes a lesson (a node without code).

/// Percentage of correct answers needed to pass when the quiz does not say otherwise.
pub const DEFAULT_PASS_PERCENT: u8 = 80;

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct Quiz {
    pub pass_percent: u8,
    pub questions: Vec<Question>,
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct Question {
    pub id: String,
    pub prompt: String,
    /// Code shown under the prompt, e.g. for "what does this print?".
    pub code: Option<String>,
    pub kind: QuestionKind,
    /// Shown after answering, right or wrong.
    pub explanation: String,
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub enum QuestionKind {
    /// Pick one option; `answer` is its index.
    SingleChoice { options: Vec<String>, answer: usize },
    /// Type the answer; any of `answers` is accepted (see `normalize`).
    Text { answers: Vec<String> },
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub enum Answer {
    Choice(usize),
    Text(String),
}

impl Question {
    pub fn is_correct(&self, answer: &Answer) -> bool {
        match (&self.kind, answer) {
            (
                QuestionKind::SingleChoice {
                    answer: expected, ..
                },
                Answer::Choice(given),
            ) => expected == given,
            (QuestionKind::Text { answers }, Answer::Text(given)) => {
                let given = normalize(given);
                answers.iter().any(|accepted| normalize(accepted) == given)
            }
            _ => false,
        }
    }

    /// The correct answer as text, for feedback.
    pub fn correct_answer(&self) -> &str {
        match &self.kind {
            QuestionKind::SingleChoice { options, answer } => {
                options.get(*answer).map_or("", String::as_str)
            }
            QuestionKind::Text { answers } => answers.first().map_or("", String::as_str),
        }
    }
}

/// Case and spacing do not matter in typed answers: `"  Some( 5 ) "` equals `"some(5)"`.
pub fn normalize(text: &str) -> String {
    text.chars()
        .filter(|c| !c.is_whitespace())
        .flat_map(char::to_lowercase)
        .collect()
}

/// A stored attempt at a lesson quiz.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct QuizAttempt {
    pub track_id: String,
    pub node_id: String,
    pub score: QuizScore,
    pub taken_at: chrono::DateTime<chrono::Utc>,
}

/// Outcome of one full attempt at a quiz.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub struct QuizScore {
    pub correct: u32,
    pub total: u32,
    pub passed: bool,
}

impl Quiz {
    pub fn score(&self, correct: u32) -> QuizScore {
        let total = self.questions.len() as u32;
        QuizScore {
            correct,
            total,
            passed: total > 0 && correct >= self.needed(),
        }
    }

    /// Correct answers needed to pass (the pass percentage, rounded up).
    pub fn needed(&self) -> u32 {
        let total = self.questions.len() as u32;
        (u32::from(self.pass_percent) * total).div_ceil(100)
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    fn choice() -> Question {
        Question {
            id: "q1".into(),
            prompt: "Which?".into(),
            code: None,
            kind: QuestionKind::SingleChoice {
                options: vec!["a".into(), "b".into()],
                answer: 1,
            },
            explanation: String::new(),
        }
    }

    fn text(accepted: &[&str]) -> Question {
        Question {
            kind: QuestionKind::Text {
                answers: accepted.iter().map(|a| a.to_string()).collect(),
            },
            ..choice()
        }
    }

    #[test]
    fn single_choice_checks_the_index() {
        assert!(choice().is_correct(&Answer::Choice(1)));
        assert!(!choice().is_correct(&Answer::Choice(0)));
        assert!(!choice().is_correct(&Answer::Text("b".into())));
        assert_eq!(choice().correct_answer(), "b");
    }

    #[test]
    fn text_answers_ignore_case_and_spaces_and_accept_alternatives() {
        let question = text(&["let mut x = 5;", "let mut x: i32 = 5;"]);
        assert!(question.is_correct(&Answer::Text("  LET mut x=5; ".into())));
        assert!(question.is_correct(&Answer::Text("let mut x: i32 = 5;".into())));
        assert!(!question.is_correct(&Answer::Text("let x = 5;".into())));
    }

    #[test]
    fn score_uses_the_pass_ratio() {
        let quiz = Quiz {
            pass_percent: 80,
            questions: vec![choice(); 5],
        };
        assert_eq!(quiz.needed(), 4);
        assert!(quiz.score(4).passed);
        assert!(!quiz.score(3).passed);

        let four = Quiz {
            pass_percent: 80,
            questions: vec![choice(); 4],
        };
        assert_eq!(four.needed(), 4, "80% of 4 is 3.2, rounded up to 4");
        assert!(!four.score(3).passed);
    }

    #[test]
    fn an_empty_quiz_never_passes() {
        assert!(
            !Quiz {
                pass_percent: 80,
                questions: vec![]
            }
            .score(0)
            .passed
        );
    }
}
