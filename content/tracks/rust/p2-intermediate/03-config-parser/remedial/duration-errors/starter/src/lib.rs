use std::fmt;

/// Why a duration could not be read.
#[derive(Debug, Clone, PartialEq, Eq)]
pub enum DurationError {
    Empty,
    MissingUnit,
    UnknownUnit(char),
    BadNumber(String),
}

impl fmt::Display for DurationError {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        todo!("describe {self:?} on {f:p}")
    }
}

impl std::error::Error for DurationError {}

/// Seconds in a duration such as `90s`, `5m` or `2h`.
pub fn parse_duration(text: &str) -> Result<u64, DurationError> {
    todo!("parse {text:?}")
}
