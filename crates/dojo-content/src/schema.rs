use anyhow::{Context, Result, anyhow};
use jsonschema::Validator;
use serde_json::Value;
use std::fs;
use std::path::Path;

/// The compiled JSON Schemas of every content file type.
pub struct Schemas {
    pub track: Validator,
    pub node: Validator,
    pub skills: Validator,
    pub bridge: Validator,
    pub quiz: Validator,
    pub placement: Validator,
}

impl Schemas {
    pub fn load(schema_dir: &Path) -> Result<Self> {
        Ok(Self {
            track: compile(schema_dir, "track.schema.json")?,
            node: compile(schema_dir, "node.schema.json")?,
            skills: compile(schema_dir, "skills.schema.json")?,
            bridge: compile(schema_dir, "bridge.schema.json")?,
            quiz: compile(schema_dir, "quiz.schema.json")?,
            placement: compile(schema_dir, "placement.schema.json")?,
        })
    }
}

fn compile(schema_dir: &Path, file_name: &str) -> Result<Validator> {
    let path = schema_dir.join(file_name);
    let text = fs::read_to_string(&path)
        .with_context(|| format!("cannot read schema {}", path.display()))?;
    let schema: Value = serde_json::from_str(&text)
        .with_context(|| format!("invalid JSON in {}", path.display()))?;
    jsonschema::validator_for(&schema)
        .map_err(|error| anyhow!("invalid schema {}: {error}", path.display()))
}

/// Human-readable schema violations of `instance`, one per problem.
pub fn violations(validator: &Validator, instance: &Value) -> Vec<String> {
    validator
        .iter_errors(instance)
        .map(|error| {
            let pointer = error.instance_path().to_string();
            if pointer.is_empty() {
                error.to_string()
            } else {
                format!("{pointer}: {error}")
            }
        })
        .collect()
}
