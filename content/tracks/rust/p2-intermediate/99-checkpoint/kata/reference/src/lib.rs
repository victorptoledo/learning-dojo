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
        match self {
            Self::MissingColon { line } => write!(f, "line {line}: missing ':'"),
            Self::MissingName { line } => write!(f, "line {line}: missing name"),
            Self::NoScores { line } => write!(f, "line {line}: no scores"),
            Self::BadScore { line, value } => {
                write!(f, "line {line}: '{value}' is not a score from 0 to 100")
            }
        }
    }
}

impl std::error::Error for GradeError {}

/// A one-line summary of something, for a printed report.
pub trait Report {
    fn report(&self) -> String;
}

impl Student {
    pub fn average(&self) -> f64 {
        let total: u32 = self.scores.iter().sum();
        f64::from(total) / self.scores.len() as f64
    }

    pub fn letter(&self) -> char {
        match self.average() {
            a if a >= 90.0 => 'A',
            a if a >= 80.0 => 'B',
            a if a >= 70.0 => 'C',
            a if a >= 60.0 => 'D',
            _ => 'F',
        }
    }
}

impl Report for Student {
    fn report(&self) -> String {
        format!("{}: {:.1} ({})", self.name, self.average(), self.letter())
    }
}

/// Parses the whole grade book, stopping at the first problem.
pub fn parse_grade_book(text: &str) -> Result<Vec<Student>, GradeError> {
    text.lines()
        .enumerate()
        .map(|(index, raw)| (index + 1, raw.trim()))
        .filter(|(_, raw)| !raw.is_empty() && !raw.starts_with('#'))
        .map(|(line, raw)| parse_line(line, raw))
        .collect()
}

fn parse_line(line: usize, raw: &str) -> Result<Student, GradeError> {
    let (name, scores) = raw.split_once(':').ok_or(GradeError::MissingColon { line })?;
    let name = name.trim();
    if name.is_empty() {
        return Err(GradeError::MissingName { line });
    }
    if scores.trim().is_empty() {
        return Err(GradeError::NoScores { line });
    }
    let scores = scores
        .split(',')
        .map(|value| parse_score(line, value.trim()))
        .collect::<Result<Vec<_>, _>>()?;
    Ok(Student { name: name.to_string(), scores })
}

fn parse_score(line: usize, value: &str) -> Result<u32, GradeError> {
    value
        .parse::<u32>()
        .ok()
        .filter(|score| *score <= 100)
        .ok_or_else(|| GradeError::BadScore { line, value: value.to_string() })
}

/// Names with an average of at least `min_average`, best first, alphabetical on a tie.
pub fn ranking(students: &[Student], min_average: f64) -> Vec<&str> {
    let mut ranked: Vec<(&str, f64)> = students
        .iter()
        .map(|student| (student.name.as_str(), student.average()))
        .filter(|(_, average)| *average >= min_average)
        .collect();
    ranked.sort_by(|a, b| b.1.total_cmp(&a.1).then_with(|| a.0.cmp(b.0)));
    ranked.into_iter().map(|(name, _)| name).collect()
}
