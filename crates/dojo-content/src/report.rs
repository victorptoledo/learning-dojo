use std::fmt;

#[derive(Debug, Clone, Copy, PartialEq, Eq, PartialOrd, Ord)]
pub enum Severity {
    Warning,
    Error,
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct Issue {
    pub severity: Severity,
    /// Content-relative path, optionally followed by a node or field reference.
    pub location: String,
    pub message: String,
}

impl fmt::Display for Issue {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        let label = match self.severity {
            Severity::Error => "error",
            Severity::Warning => "warning",
        };
        write!(f, "{label}: {}: {}", self.location, self.message)
    }
}

#[derive(Debug, Clone, Default)]
pub struct Report {
    issues: Vec<Issue>,
}

impl Report {
    pub fn push(
        &mut self,
        severity: Severity,
        location: impl Into<String>,
        message: impl Into<String>,
    ) {
        self.issues.push(Issue {
            severity,
            location: location.into(),
            message: message.into(),
        });
    }

    pub fn error(&mut self, location: impl Into<String>, message: impl Into<String>) {
        self.push(Severity::Error, location, message);
    }

    pub fn warning(&mut self, location: impl Into<String>, message: impl Into<String>) {
        self.push(Severity::Warning, location, message);
    }

    pub fn issues(&self) -> &[Issue] {
        &self.issues
    }

    pub fn count(&self, severity: Severity) -> usize {
        self.issues
            .iter()
            .filter(|issue| issue.severity == severity)
            .count()
    }

    pub fn has_errors(&self) -> bool {
        self.count(Severity::Error) > 0
    }
}
