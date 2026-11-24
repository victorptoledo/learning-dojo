//! Quizzes: the evidence that completes a lesson (a node without code).

use crate::rng::SplitMix64;

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
    /// Skills the question tests; a wrong answer in a Belt Exam sends them back to study.
    pub skills: Vec<String>,
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

    /// Puts the options of a single-choice question in a new order; the answer follows
    /// its option. Typed-answer questions are left as they are.
    pub fn shuffle_options(&mut self, rng: &mut SplitMix64) {
        if let QuestionKind::SingleChoice { options, answer } = &mut self.kind {
            let mut order: Vec<usize> = (0..options.len()).collect();
            rng.shuffle(&mut order);
            *options = order.iter().map(|&old| options[old].clone()).collect();
            if let Some(new_index) = order.iter().position(|&old| old == *answer) {
                *answer = new_index;
            }
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

    /// Shuffles the questions and the options of each one, so a retry cannot be passed
    /// by remembering positions. Returns the new order as old indexes, for callers that
    /// keep data per question.
    pub fn shuffle(&mut self, rng: &mut SplitMix64) -> Vec<usize> {
        let mut order: Vec<usize> = (0..self.questions.len()).collect();
        rng.shuffle(&mut order);
        let mut questions: Vec<Question> = order
            .iter()
            .map(|&old| self.questions[old].clone())
            .collect();
        for question in &mut questions {
            question.shuffle_options(rng);
        }
        self.questions = questions;
        order
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
            skills: vec![],
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

    fn four_options(answer: usize) -> Question {
        Question {
            kind: QuestionKind::SingleChoice {
                options: vec!["w".into(), "x".into(), "y".into(), "z".into()],
                answer,
            },
            ..choice()
        }
    }

    #[test]
    fn shuffled_options_keep_the_right_answer_and_move_it_around() {
        let mut positions = std::collections::HashSet::new();
        for seed in 0..40 {
            let mut question = four_options(1);
            question.shuffle_options(&mut SplitMix64::new(seed));
            assert_eq!(question.correct_answer(), "x", "seed {seed}");
            let QuestionKind::SingleChoice { options, answer } = &question.kind else {
                unreachable!()
            };
            let mut sorted = options.clone();
            sorted.sort();
            assert_eq!(sorted, ["w", "x", "y", "z"], "no option lost");
            assert!(question.is_correct(&Answer::Choice(*answer)));
            positions.insert(*answer);
        }
        assert_eq!(positions.len(), 4, "the answer lands in every position");
    }

    #[test]
    fn shuffling_a_quiz_reorders_questions_and_reports_the_order() {
        let mut quiz = Quiz {
            pass_percent: 80,
            questions: (0..6)
                .map(|n| Question {
                    id: format!("q{n}"),
                    ..four_options(n % 4)
                })
                .collect(),
        };
        let before = quiz.clone();
        let order = quiz.shuffle(&mut SplitMix64::new(7));
        assert_ne!(
            quiz.questions.iter().map(|q| &q.id).collect::<Vec<_>>(),
            before.questions.iter().map(|q| &q.id).collect::<Vec<_>>()
        );
        for (new, &old) in order.iter().enumerate() {
            assert_eq!(quiz.questions[new].id, before.questions[old].id);
            assert_eq!(
                quiz.questions[new].correct_answer(),
                before.questions[old].correct_answer()
            );
        }
    }

    #[test]
    fn text_questions_are_not_touched_by_shuffling_options() {
        let mut question = text(&["Some(5)"]);
        question.shuffle_options(&mut SplitMix64::new(3));
        assert_eq!(question, text(&["Some(5)"]));
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
