use std::fmt;

/// A line that is not a number.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct ParseError {
    pub line: usize,
    pub text: String,
}

impl fmt::Display for ParseError {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        write!(f, "line {}: `{}` is not a number", self.line, self.text)
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
    let values = text
        .lines()
        .enumerate()
        .map(|(index, line)| (index + 1, line.trim()))
        .filter(|(_, line)| !line.is_empty())
        .map(|(line, value)| {
            value.parse::<f64>().map_err(|_| ParseError {
                line,
                text: value.to_string(),
            })
        })
        .collect::<Result<Vec<f64>, ParseError>>()?;
    Ok(Readings { values })
}

impl Readings {
    pub fn max(&self) -> Option<f64> {
        self.values.iter().copied().reduce(f64::max)
    }

    pub fn mean(&self) -> Option<f64> {
        (!self.values.is_empty())
            .then(|| self.values.iter().sum::<f64>() / self.values.len() as f64)
    }
}

impl Summary for Readings {
    fn summary(&self) -> String {
        let (Some(max), Some(mean)) = (self.max(), self.mean()) else {
            return "no readings".to_string();
        };
        let count = match self.values.len() {
            1 => "1 reading".to_string(),
            n => format!("{n} readings"),
        };
        format!("{count}, max {max:.1}, mean {mean:.1}")
    }
}
