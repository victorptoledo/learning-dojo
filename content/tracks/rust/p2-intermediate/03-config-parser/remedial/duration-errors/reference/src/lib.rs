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
        match self {
            Self::Empty => write!(f, "empty duration"),
            Self::MissingUnit => write!(f, "missing unit (use s, m or h)"),
            Self::UnknownUnit(unit) => write!(f, "unknown unit `{unit}`"),
            Self::BadNumber(number) => write!(f, "`{number}` is not a whole number"),
        }
    }
}

impl std::error::Error for DurationError {}

/// Seconds in a duration such as `90s`, `5m` or `2h`.
pub fn parse_duration(text: &str) -> Result<u64, DurationError> {
    let text = text.trim();
    let Some(unit) = text.chars().last() else {
        return Err(DurationError::Empty);
    };
    if unit.is_ascii_digit() {
        return Err(DurationError::MissingUnit);
    }
    let factor = match unit {
        's' => 1,
        'm' => 60,
        'h' => 3600,
        other => return Err(DurationError::UnknownUnit(other)),
    };
    let number = &text[..text.len() - unit.len_utf8()];
    let bad = || DurationError::BadNumber(number.to_string());
    let value: u64 = number.parse().map_err(|_| bad())?;
    value.checked_mul(factor).ok_or_else(bad)
}
