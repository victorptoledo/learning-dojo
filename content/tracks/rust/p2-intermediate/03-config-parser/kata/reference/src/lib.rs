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
        match self {
            Self::MissingEquals { line } => write!(f, "line {line}: expected key = value"),
            Self::EmptyKey { line } => write!(f, "line {line}: empty key"),
            Self::DuplicateKey { key, line } => write!(f, "line {line}: duplicate key {key:?}"),
        }
    }
}

impl std::error::Error for ConfigError {}

/// Parses `key = value` lines into a map.
pub fn parse_config(text: &str) -> Result<BTreeMap<String, String>, ConfigError> {
    let mut config = BTreeMap::new();
    for (index, raw_line) in text.lines().enumerate() {
        let line = index + 1;
        let content = raw_line.trim();
        if content.is_empty() || content.starts_with('#') {
            continue;
        }
        let (key, value) = parse_entry(content, line)?;
        if config.contains_key(key) {
            return Err(ConfigError::DuplicateKey { key: key.to_string(), line });
        }
        config.insert(key.to_string(), value.to_string());
    }
    Ok(config)
}

fn parse_entry(content: &str, line: usize) -> Result<(&str, &str), ConfigError> {
    let (key, value) = content
        .split_once('=')
        .ok_or(ConfigError::MissingEquals { line })?;
    let key = key.trim();
    if key.is_empty() {
        return Err(ConfigError::EmptyKey { line });
    }
    Ok((key, value.trim()))
}
