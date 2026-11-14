use std::collections::BTreeMap;
use std::fmt;

#[derive(Debug, Clone, PartialEq, Eq)]
pub enum ConfigError {
    MissingEquals { line: usize },
    EmptyKey { line: usize },
    DuplicateKey { key: String, line: usize },
}

impl fmt::Display for ConfigError {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        todo!("write the message for {self:?} into {f:p}")
    }
}

impl std::error::Error for ConfigError {}

/// Parses `key = value` lines into a map.
pub fn parse_config(text: &str) -> Result<BTreeMap<String, String>, ConfigError> {
    todo!("parse {} bytes of config", text.len())
}
