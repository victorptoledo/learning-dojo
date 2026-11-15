//! Technical interview questions of a track, used by the mock interview: the learner answers
//! out loud or in writing, then compares with what a strong answer covers.

/// How senior a question is.
#[derive(Debug, Clone, Copy, PartialEq, Eq, PartialOrd, Ord, Hash)]
pub enum InterviewLevel {
    Junior,
    Mid,
    Senior,
}

impl InterviewLevel {
    pub const ALL: [InterviewLevel; 3] = [Self::Junior, Self::Mid, Self::Senior];

    pub fn parse(text: &str) -> Option<Self> {
        match text {
            "junior" => Some(Self::Junior),
            "mid" => Some(Self::Mid),
            "senior" => Some(Self::Senior),
            _ => None,
        }
    }

    pub fn label(self) -> &'static str {
        match self {
            Self::Junior => "Junior",
            Self::Mid => "Mid-level",
            Self::Senior => "Senior",
        }
    }
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct InterviewQuestion {
    pub id: String,
    pub level: InterviewLevel,
    pub skills: Vec<String>,
    pub question: String,
    /// What a strong answer covers.
    pub key_points: Vec<String>,
    /// The question an interviewer would ask next.
    pub follow_up: Option<String>,
}

/// A track's interview questions and the public guides their topics come from.
#[derive(Debug, Clone, PartialEq, Eq, Default)]
pub struct InterviewBank {
    pub sources: Vec<String>,
    pub questions: Vec<InterviewQuestion>,
}
