//! Rules that need the whole catalog (cross-file references, graph shape, completeness).

use crate::report::{Report, Severity};
use dojo_core::{Catalog, ContentStatus, Track};
use std::collections::{BTreeMap, BTreeSet, HashMap};

const MIN_NODES_PER_TRACK: usize = 15;
const MAX_NODES_PER_TRACK: usize = 25;
const PHASES_WITH_CHECKPOINT: [&str; 2] = ["p1", "p2"];
const MIN_QUIZ_QUESTIONS: usize = 3;

pub fn check_catalog(catalog: &Catalog, report: &mut Report) {
    check_unique_tracks(catalog, report);
    check_unique_nodes(catalog, report);
    check_skill_mappings(catalog, report);
    for track in &catalog.tracks {
        check_phases(track, report);
        check_belts(track, report);
        check_prerequisites(track, report);
        check_prerequisite_cycles(track, report);
        check_node_skills(track, catalog, report);
        check_completeness(track, report);
    }
}

fn track_location(track: &Track) -> String {
    format!("tracks/{}", track.id)
}

fn node_location(track: &Track, node_id: &str) -> String {
    format!("tracks/{}/{node_id}", track.id)
}

fn check_unique_tracks(catalog: &Catalog, report: &mut Report) {
    let mut seen = BTreeSet::new();
    for track in &catalog.tracks {
        if !seen.insert(&track.id) {
            report.error(
                track_location(track),
                format!("duplicate track id `{}`", track.id),
            );
        }
    }
}

fn check_unique_nodes(catalog: &Catalog, report: &mut Report) {
    let mut seen = BTreeSet::new();
    for track in &catalog.tracks {
        for node in track.nodes() {
            if !seen.insert(&node.id) {
                report.error(node_location(track, &node.id), "duplicate node id");
            }
        }
    }
}

fn check_skill_mappings(catalog: &Catalog, report: &mut Report) {
    for skill in catalog.skills.values() {
        for target in &skill.maps_to {
            if !catalog.skills.contains_key(target) {
                report.error(
                    format!("skills/{}", skill.id),
                    format!("maps_to unknown skill `{target}`"),
                );
            }
        }
    }
}

fn check_phases(track: &Track, report: &mut Report) {
    for (expected_order, phase) in track.phases.iter().enumerate() {
        if usize::from(phase.order) != expected_order {
            report.error(
                track_location(track),
                format!("phases must be listed in order p0..p4; found `{}` at position {expected_order}", phase.id),
            );
        }
    }
}

fn check_belts(track: &Track, report: &mut Report) {
    if track
        .belts
        .first()
        .is_some_and(|belt| belt.required_xp != 0)
    {
        report.error(track_location(track), "the first belt must require 0 XP");
    }
    let ascending = track
        .belts
        .windows(2)
        .all(|pair| pair[0].required_xp < pair[1].required_xp);
    if !ascending {
        report.error(
            track_location(track),
            "belts must be sorted by strictly increasing required_xp",
        );
    }
}

fn check_prerequisites(track: &Track, report: &mut Report) {
    let phase_by_node: HashMap<&str, u8> = track
        .phases
        .iter()
        .flat_map(|phase| {
            phase
                .nodes
                .iter()
                .map(move |node| (node.id.as_str(), phase.order))
        })
        .collect();

    for phase in &track.phases {
        for node in &phase.nodes {
            for prerequisite in &node.prerequisites {
                match phase_by_node.get(prerequisite.as_str()) {
                    None => report.error(
                        node_location(track, &node.id),
                        format!("prerequisite `{prerequisite}` does not exist in this track"),
                    ),
                    Some(&required_phase) if required_phase > phase.order => report.error(
                        node_location(track, &node.id),
                        format!("prerequisite `{prerequisite}` is in a later phase"),
                    ),
                    Some(_) => {}
                }
            }
        }
    }
}

fn check_prerequisite_cycles(track: &Track, report: &mut Report) {
    let graph: BTreeMap<&str, Vec<&str>> = track
        .nodes()
        .map(|node| {
            (
                node.id.as_str(),
                node.prerequisites.iter().map(String::as_str).collect(),
            )
        })
        .collect();
    let mut state: HashMap<&str, Visit> = HashMap::new();
    for &start in graph.keys() {
        if let Some(cycle_node) = find_cycle(start, &graph, &mut state) {
            report.error(
                node_location(track, cycle_node),
                "prerequisites form a cycle",
            );
            return;
        }
    }
}

#[derive(Clone, Copy, PartialEq, Eq)]
enum Visit {
    InProgress,
    Done,
}

/// Depth-first search; returns a node that closes a cycle, if any.
fn find_cycle<'a>(
    node: &'a str,
    graph: &BTreeMap<&'a str, Vec<&'a str>>,
    state: &mut HashMap<&'a str, Visit>,
) -> Option<&'a str> {
    match state.get(node) {
        Some(Visit::Done) => return None,
        Some(Visit::InProgress) => return Some(node),
        None => {}
    }
    state.insert(node, Visit::InProgress);
    for &next in graph.get(node).into_iter().flatten() {
        if let Some(found) = find_cycle(next, graph, state) {
            return Some(found);
        }
    }
    state.insert(node, Visit::Done);
    None
}

fn check_node_skills(track: &Track, catalog: &Catalog, report: &mut Report) {
    for node in track.nodes() {
        for skill in node.skills() {
            if !catalog.skills.contains_key(skill) {
                report.error(
                    node_location(track, &node.id),
                    format!("unknown skill `{skill}`"),
                );
            }
        }
    }
}

/// Pedagogical completeness: warnings while a track is a draft, errors once reviewed.
fn check_completeness(track: &Track, report: &mut Report) {
    let severity = match track.status {
        ContentStatus::Draft => Severity::Warning,
        ContentStatus::Reviewed => Severity::Error,
    };
    let location = track_location(track);

    let node_count = track.node_count();
    if !(MIN_NODES_PER_TRACK..=MAX_NODES_PER_TRACK).contains(&node_count) {
        report.push(
            severity,
            &location,
            format!("has {node_count} nodes; expected {MIN_NODES_PER_TRACK}–{MAX_NODES_PER_TRACK}"),
        );
    }
    for phase in &track.phases {
        if phase.nodes.is_empty() {
            report.push(
                severity,
                &location,
                format!("phase `{}` has no nodes", phase.id),
            );
        }
    }
    for phase_id in PHASES_WITH_CHECKPOINT {
        let Some(phase) = track.phases.iter().find(|phase| phase.id == phase_id) else {
            continue;
        };
        let last_is_checkpoint = phase.nodes.last().is_some_and(|node| {
            phase
                .checkpoint()
                .is_some_and(|checkpoint| checkpoint.id == node.id)
        });
        if !last_is_checkpoint {
            report.push(
                severity,
                &location,
                format!("phase `{phase_id}` must end with a checkpoint"),
            );
        }
    }
    for node in track.nodes().filter(|node| node.lesson.is_none()) {
        report.push(
            severity,
            node_location(track, &node.id),
            "node has no lesson",
        );
    }
    for node in track.nodes().filter(|node| node.under_construction()) {
        report.push(
            severity,
            node_location(track, &node.id),
            "learners see this node as under construction (a lesson needs its document and quiz, a kata its exercise)",
        );
    }
    for node in track.nodes() {
        if let Some(quiz) = &node.quiz
            && quiz.questions.len() < MIN_QUIZ_QUESTIONS
        {
            report.push(
                severity,
                node_location(track, &node.id),
                format!(
                    "quiz has {} questions; expected at least {MIN_QUIZ_QUESTIONS}",
                    quiz.questions.len()
                ),
            );
        }
    }
}
