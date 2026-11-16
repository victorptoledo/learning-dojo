use std::fmt;

/// A line that is not a number.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct ParseError {
    pub line: usize,
    pub text: String,
}

impl fmt::Display for ParseError {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        todo!("describe {self:?} on {f:p}")
    }
}

impl std::error::Error for ParseError {}

/// Something that can describe itself in one line.
pub trait Summary {
    fn summary(&self) -> String;
}

/// Temperatures in the order they were read.
#[derive(Debug, Clone, PartialEq)]
pub struct Readings {
    pub values: Vec<f64>,
}

/// One reading per line; blank lines are skipped.
pub fn parse_readings(text: &str) -> Result<Readings, ParseError> {
    todo!("parse {text:?}")
}

impl Readings {
    pub fn max(&self) -> Option<f64> {
        todo!("max of {:?}", self.values)
    }

    pub fn mean(&self) -> Option<f64> {
        todo!("mean of {:?}", self.values)
    }
}

impl Summary for Readings {
    fn summary(&self) -> String {
        todo!("summarise {:?}", self.values)
    }
}
