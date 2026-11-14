//! `dojo doctor`: checks everything a track needs and prints what is missing.

use crate::environment::{Environment, Status};
use anyhow::{Result, bail};
use dojo_core::{Requirement, Track};
use std::collections::BTreeMap;
use std::path::Path;
use std::process::ExitCode;

pub fn run(content_dir: &Path, track_id: &str, environment: &Environment) -> Result<ExitCode> {
    let catalog = dojo_content::load(content_dir)?.catalog;
    let Some(track) = catalog.track(track_id) else {
        bail!("no track `{track_id}`");
    };
    println!(
        "{} {}: what this track needs on your machine\n",
        track.icon, track.name
    );
    let mut missing_required = 0;
    for (requirement, nodes) in requirements_with_users(track) {
        let status = environment.check(requirement);
        let blocking = requirement.required && !status.is_satisfied();
        missing_required += usize::from(blocking);
        println!(
            "{} {}{}",
            icon(&status, requirement.required),
            requirement.name,
            optional_tag(requirement)
        );
        println!("    {}", describe(&status));
        println!("    used by: {}", nodes.join(", "));
    }
    println!();
    if missing_required == 0 {
        println!("Everything required is in place.");
        Ok(ExitCode::SUCCESS)
    } else {
        println!(
            "{missing_required} required item(s) missing. Open a node in the dojo and press [I], or follow the hints."
        );
        Ok(ExitCode::FAILURE)
    }
}

/// Each distinct requirement with the nodes that need it ("all nodes" for track-wide ones).
fn requirements_with_users(track: &Track) -> Vec<(&Requirement, Vec<String>)> {
    let mut by_key: BTreeMap<String, (&Requirement, Vec<String>)> = BTreeMap::new();
    for requirement in &track.requirements {
        by_key.insert(
            requirement.key(),
            (requirement, vec!["all nodes".to_string()]),
        );
    }
    for node in track.nodes() {
        for requirement in &node.requirements {
            let entry = by_key
                .entry(requirement.key())
                .or_insert((requirement, Vec::new()));
            entry.1.push(node.title.clone());
        }
    }
    by_key.into_values().collect()
}

fn icon(status: &Status, required: bool) -> &'static str {
    match status {
        Status::Ready { .. } => "✓",
        Status::NotApplicable { .. } => "–",
        Status::Outdated { .. } => "⚠",
        Status::Missing { .. } if required => "✗",
        Status::Missing { .. } => "○",
    }
}

fn optional_tag(requirement: &Requirement) -> &'static str {
    if requirement.required {
        ""
    } else {
        " (optional)"
    }
}

fn describe(status: &Status) -> String {
    match status {
        Status::Ready { detail } => format!("ok {detail}"),
        Status::NotApplicable { reason } => reason.clone(),
        Status::Outdated { found, needed } => format!("{found} found, {needed} needed"),
        Status::Missing { hint } => hint.clone(),
    }
}
