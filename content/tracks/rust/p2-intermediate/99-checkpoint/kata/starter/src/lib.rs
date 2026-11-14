use std::fmt;

/// One line of the grade book.
#[derive(Debug, Clone, PartialEq)]
pub struct Student {
    pub name: String,
    pub scores: Vec<u32>,
}

/// The first problem found in a grade book.
#[derive(Debug, Clone, PartialEq, Eq)]
pub enum GradeError {
    MissingColon { line: usize },
    MissingName { line: usize },
    NoScores { line: usize },
    BadScore { line: usize, value: String },
}

impl fmt::Display for GradeError {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        todo!("describe {self:?} on {f:p}")
    }
}

impl std::error::Error for GradeError {}

/// A one-line summary of something, for a printed report.
pub trait Report {
    fn report(&self) -> String;
}

impl Student {
    pub fn average(&self) -> f64 {
        todo!("average of {:?}", self.scores)
    }

    pub fn letter(&self) -> char {
        todo!("letter grade of {}", self.name)
    }
}

impl Report for Student {
    fn report(&self) -> String {
        todo!("report line for {}", self.name)
    }
}

/// Parses the whole grade book, stopping at the first problem.
pub fn parse_grade_book(text: &str) -> Result<Vec<Student>, GradeError> {
    todo!("parse {text:?}")
}

/// Names with an average of at least `min_average`, best first, alphabetical on a tie.
pub fn ranking(students: &[Student], min_average: f64) -> Vec<&str> {
    todo!("rank {} students above {min_average}", students.len())
}
