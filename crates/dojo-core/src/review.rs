//! Spaced review: the questions of finished nodes come back after growing intervals.
//!
//! Each question is a card. A card starts the day after its node is completed; a right
//! answer moves it one step along `INTERVALS_DAYS`, a wrong one sends it back to the
//! first step, so what the learner misses returns sooner.

use chrono::{Days, NaiveDate};

/// Days until the next review, by step. The last step repeats.
pub const INTERVALS_DAYS: [u64; 5] = [1, 3, 7, 21, 60];

/// Practice XP per right answer. Practice XP never counts towards belts.
pub const REVIEW_XP: u32 = 5;

/// Questions per review session.
pub const SESSION_SIZE: usize = 8;

/// Where a question stands in the review schedule.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub struct Card {
    pub step: usize,
    pub due: NaiveDate,
}

impl Card {
    /// A card for a node completed on `completed_on`.
    pub fn new(completed_on: NaiveDate) -> Self {
        Self {
            step: 0,
            due: after(completed_on, 0),
        }
    }

    /// The card after an answer given on `on`.
    pub fn answered(self, correct: bool, on: NaiveDate) -> Self {
        let step = if correct {
            (self.step + 1).min(INTERVALS_DAYS.len() - 1)
        } else {
            0
        };
        Self {
            step,
            due: after(on, step),
        }
    }

    pub fn is_due(&self, today: NaiveDate) -> bool {
        self.due <= today
    }
}

fn after(day: NaiveDate, step: usize) -> NaiveDate {
    day.checked_add_days(Days::new(INTERVALS_DAYS[step]))
        .unwrap_or(day)
}

/// A question waiting for review.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct ReviewItem {
    pub node_id: String,
    pub question_id: String,
    pub card: Card,
}

#[cfg(test)]
mod tests {
    use super::*;

    fn day(d: u32) -> NaiveDate {
        NaiveDate::from_ymd_opt(2026, 11, d).unwrap()
    }

    #[test]
    fn right_answers_space_the_card_out_and_a_miss_brings_it_back_tomorrow() {
        let card = Card::new(day(1));
        assert_eq!(card.due, day(2));
        assert!(!card.is_due(day(1)));
        assert!(card.is_due(day(2)));

        let card = card.answered(true, day(2));
        assert_eq!((card.step, card.due), (1, day(5)));
        let card = card.answered(true, day(5));
        assert_eq!((card.step, card.due), (2, day(12)));

        let card = card.answered(false, day(12));
        assert_eq!((card.step, card.due), (0, day(13)));
    }

    #[test]
    fn the_last_interval_repeats() {
        let mut card = Card::new(day(1));
        for _ in 0..10 {
            card = card.answered(true, day(1));
        }
        assert_eq!(card.step, INTERVALS_DAYS.len() - 1);
    }
}
