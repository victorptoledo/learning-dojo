//! Rules that need the whole catalog (cross-file references, graph shape, completeness).

use crate::report::{Report, Severity};
use dojo_core::{CareerRequirement, Catalog, ContentStatus, NodeKind, Track};
use std::collections::{BTreeMap, BTreeSet, HashMap};

const MIN_NODES_PER_TRACK: usize = 15;
const MAX_NODES_PER_TRACK: usize = 25;
const PHASES_WITH_CHECKPOINT: [&str; 2] = ["p1", "p2"];
const MIN_QUIZ_QUESTIONS: usize = 3;

pub fn check_catalog(catalog: &Catalog, report: &mut Report) {
    check_unique_tracks(catalog, report);
    check_unique_nodes(catalog, report);
    check_skill_mappings(catalog, report);
    check_careers(catalog, report);
    check_sidequests(catalog, report);
    for track in &catalog.tracks {
        check_phases(track, report);
        check_belts(track, report);
        check_prerequisites(track, report);
        check_prerequisite_cycles(track, report);
        check_node_skills(track, catalog, report);
        check_completeness(track, report);
        check_placement(track, catalog, report);
        check_interview(track, catalog, report);
        check_checkpoint_question_skills(track, catalog, report);
        check_kata_hints(track, report);
        check_lesson_exercises(track, report);
    }
}

/// Career paths must point at real tracks, phases, skills and careers.
fn check_careers(catalog: &Catalog, report: &mut Report) {
    let mut ids = BTreeSet::new();
    for career in &catalog.careers {
        let location = format!("careers/{}.yaml", career.id);
        if !ids.insert(career.id.as_str()) {
            report.error(&location, format!("duplicate career id `{}`", career.id));
        }
        for stage in &career.stages {
            for requirement in &stage.requires {
                check_career_requirement(catalog, requirement, &location, &stage.id, report);
            }
        }
        for next in &career.next_paths {
            if catalog.career(next).is_none() {
                report.error(&location, format!("next path `{next}` is not a career"));
            }
        }
    }
}

fn check_career_requirement(
    catalog: &Catalog,
    requirement: &CareerRequirement,
    location: &str,
    stage: &str,
    report: &mut Report,
) {
    match requirement {
        CareerRequirement::TrackPhase { track, phase } => match catalog.track(track) {
            None => report.error(
                location,
                format!("stage `{stage}`: unknown track `{track}`"),
            ),
            Some(found) if !found.phases.iter().any(|p| p.id == *phase) => report.error(
                location,
                format!("stage `{stage}`: track `{track}` has no phase `{phase}`"),
            ),
            Some(_) => {}
        },
        CareerRequirement::Skill { skill, .. } => {
            if !catalog.skills.contains_key(skill) {
                report.error(
                    location,
                    format!("stage `{stage}`: unknown skill `{skill}`"),
                );
            }
        }
        CareerRequirement::AnyOf(options) => {
            for option in options {
                check_career_requirement(catalog, option, location, stage, report);
            }
        }
    }
}

fn track_location(track: &Track) -> String {
    format!("tracks/{}", track.id)
}

/// Stages by sidequest level: N1 is small, N2 and N3 medium, N4 large.
const STAGES_BY_LEVEL: [usize; 4] = [3, 4, 4, 5];

/// Sidequests (D030): one per level and track, unlocked by `track:PN` requirements whose
/// main track comes first at the sidequest's own level and the others at most that.
fn check_sidequests(catalog: &Catalog, report: &mut Report) {
    let mut levels = BTreeSet::new();
    for quest in &catalog.sidequests {
        let location = format!(
            "tracks/{}/sidequests/{}/sidequest.yaml",
            quest.track_id, quest.slug
        );
        if catalog.track(&quest.track_id).is_none() {
            report.error(&location, format!("unknown track `{}`", quest.track_id));
        }
        if !levels.insert((quest.track_id.as_str(), quest.level)) {
            report.error(
                &location,
                format!(
                    "track `{}` already has a level {} sidequest",
                    quest.track_id, quest.level
                ),
            );
        }
        check_sidequest_requirements(catalog, quest, &location, report);
        for skill in &quest.skills {
            if !catalog.skills.contains_key(skill) {
                report.error(&location, format!("unknown skill `{skill}`"));
            }
        }
        let expected = STAGES_BY_LEVEL[usize::from(quest.level.clamp(1, 4)) - 1];
        if quest.stages.len() != expected {
            report.warning(
                &location,
                format!(
                    "a level {} sidequest should have {expected} stages, not {}",
                    quest.level,
                    quest.stages.len()
                ),
            );
        }
        check_stage_test_names(quest, &location, report);
    }
}

fn check_sidequest_requirements(
    catalog: &Catalog,
    quest: &dojo_core::Sidequest,
    location: &str,
    report: &mut Report,
) {
    match quest.requires.first() {
        Some(main) if main.track == quest.track_id && main.milestone.level() == quest.level => {}
        _ => report.error(
            location,
            format!(
                "the first requirement must be `{}:P{}` (the main track at the sidequest's level)",
                quest.track_id, quest.level
            ),
        ),
    }
    let mut tracks = BTreeSet::new();
    for requirement in &quest.requires {
        if !tracks.insert(requirement.track.as_str()) {
            report.error(
                location,
                format!("track `{}` is required twice", requirement.track),
            );
        }
        let Some(track) = catalog.track(&requirement.track) else {
            report.error(
                location,
                format!("requirement `{requirement}`: unknown track"),
            );
            continue;
        };
        if requirement.milestone.level() > quest.level {
            report.error(
                location,
                format!(
                    "requirement `{requirement}` is above the sidequest's level {}",
                    quest.level
                ),
            );
        }
        if requirement.evidence(track).is_empty() {
            report.error(
                location,
                format!("requirement `{requirement}`: the track has no node for it"),
            );
        }
    }
}

/// Stages checked in the same image run their tests together, so two stages must not
/// ship a test file with the same path.
fn check_stage_test_names(quest: &dojo_core::Sidequest, location: &str, report: &mut Report) {
    let mut seen: BTreeMap<(String, std::path::PathBuf), &str> = BTreeMap::new();
    for stage in &quest.stages {
        for file in relative_files(&stage.kata.tests_dir) {
            let key = (stage.kata.runner.image.clone(), file.clone());
            if let Some(other) = seen.insert(key, &stage.slug) {
                report.error(
                    location,
                    format!(
                        "stages `{other}` and `{}` both have tests/{}",
                        stage.slug,
                        file.display()
                    ),
                );
            }
        }
    }
}

fn relative_files(dir: &std::path::Path) -> Vec<std::path::PathBuf> {
    let mut files = Vec::new();
    let mut pending = vec![dir.to_path_buf()];
    while let Some(current) = pending.pop() {
        let Ok(entries) = std::fs::read_dir(&current) else {
            continue;
        };
        for entry in entries.flatten() {
            let path = entry.path();
            if path.is_dir() {
                pending.push(path);
            } else if let Ok(relative) = path.strip_prefix(dir) {
                files.push(relative.to_path_buf());
            }
        }
    }
    files.sort();
    files
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
        // Practice targets the node's own skills, so a stuck learner drills what the
        // node teaches.
        let own: Vec<&str> = node.skills().collect();
        for remedial in &node.remedial {
            for skill in &remedial.skills {
                if !catalog.skills.contains_key(skill) {
                    report.error(
                        node_location(track, &node.id),
                        format!("remedial `{}`: unknown skill `{skill}`", remedial.slug),
                    );
                } else if !own.contains(&skill.as_str()) {
                    report.warning(
                        node_location(track, &node.id),
                        format!(
                            "remedial `{}`: skill `{skill}` is not one of the node's skills",
                            remedial.slug
                        ),
                    );
                }
            }
        }
    }
}

/// Interview questions must have unique ids and point at real skills.
fn check_interview(track: &Track, catalog: &Catalog, report: &mut Report) {
    let Some(bank) = &track.interview else {
        return;
    };
    let location = format!("tracks/{}/interview.yaml", track.id);
    let mut seen = std::collections::HashSet::new();
    for question in &bank.questions {
        if !seen.insert(question.id.as_str()) {
            report.error(
                &location,
                format!("duplicate interview question id `{}`", question.id),
            );
        }
        for skill in &question.skills {
            if !catalog.skills.contains_key(skill) {
                report.error(
                    &location,
                    format!("question `{}` uses unknown skill `{skill}`", question.id),
                );
            }
        }
    }
}

/// The placement test must point at real phases and skills and have enough questions.
fn check_placement(track: &Track, catalog: &Catalog, report: &mut Report) {
    let Some(config) = &track.placement else {
        return;
    };
    let location = format!("tracks/{}/placement.yaml", track.id);
    let has_phase = |id: &str| track.phases.iter().any(|phase| phase.id == id);
    for option in &config.entry_options {
        if !has_phase(&option.start_phase) {
            report.error(
                &location,
                format!(
                    "entry option `{}` starts at unknown phase `{}`",
                    option.label, option.start_phase
                ),
            );
        }
    }
    if config.early_stop.wrong > config.early_stop.of {
        report.error(&location, "early_stop: wrong_in_first cannot exceed of");
    }
    let mut ids = BTreeSet::new();
    for item in &config.items {
        if !ids.insert(&item.question.id) {
            report.error(
                &location,
                format!("duplicate question id `{}`", item.question.id),
            );
        }
        if !config.phases.contains_key(&item.phase) {
            report.error(
                &location,
                format!(
                    "question `{}`: phase `{}` has no rule under phases",
                    item.question.id, item.phase
                ),
            );
        }
        for skill in &item.question.skills {
            if !catalog.skills.contains_key(skill) {
                report.error(
                    &location,
                    format!("question `{}`: unknown skill `{skill}`", item.question.id),
                );
            }
        }
    }
    for (phase, rule) in &config.phases {
        if !has_phase(phase) {
            report.error(&location, format!("rule for unknown phase `{phase}`"));
        }
        if rule.partial > rule.pass {
            report.error(
                &location,
                format!("phase `{phase}`: partial cannot exceed pass"),
            );
        }
        let available = config
            .items
            .iter()
            .filter(|item| item.phase == *phase)
            .count();
        if available < rule.items {
            report.warning(
                &location,
                format!(
                    "phase `{phase}` draws {} questions but has only {available}",
                    rule.items
                ),
            );
        }
        // Nodes of a partly known phase are skipped by the skills the test measured.
        let asked: BTreeSet<&str> = config
            .items
            .iter()
            .filter(|item| item.phase.as_str() <= phase.as_str())
            .flat_map(|item| item.question.skills.iter().map(String::as_str))
            .collect();
        let phase_nodes = track
            .phases
            .iter()
            .filter(|p| p.id == *phase)
            .flat_map(|p| p.nodes.iter());
        for node in phase_nodes.filter(|node| node.kind != NodeKind::Checkpoint) {
            if !node.skills().any(|skill| asked.contains(skill)) {
                report.warning(
                    &location,
                    format!("phase `{phase}`: no question tests a skill of `{}`, so it is only skipped with the whole phase", node.id),
                );
            }
        }
    }
}

/// A checkpoint is a Belt Exam: a missed question sends back the nodes of its skills,
/// so every checkpoint question should name the skills it tests.
fn check_checkpoint_question_skills(track: &Track, catalog: &Catalog, report: &mut Report) {
    for node in track
        .nodes()
        .filter(|node| node.kind == NodeKind::Checkpoint)
    {
        let Some(quiz) = &node.quiz else {
            continue;
        };
        for question in &quiz.questions {
            if question.skills.is_empty() {
                report.warning(
                    node_location(track, &node.id),
                    format!("checkpoint question `{}` names no skills", question.id),
                );
            }
            for skill in &question.skills {
                if !catalog.skills.contains_key(skill) {
                    report.error(
                        node_location(track, &node.id),
                        format!("question `{}`: unknown skill `{skill}`", question.id),
                    );
                }
            }
        }
    }
}

/// A ready lesson (document and quiz written) should offer a written exercise for AI review.
fn check_lesson_exercises(track: &Track, report: &mut Report) {
    for node in track.nodes() {
        let ready_lesson =
            node.kind == NodeKind::Lesson && node.lesson.is_some() && node.quiz.is_some();
        if ready_lesson && node.exercise.is_none() {
            report.warning(
                node_location(track, &node.id),
                "lesson has no exercise: add one under `exercise` (task and criteria)",
            );
        }
    }
}

/// Every kata should offer the hint ladder, so a stuck learner is never left alone.
fn check_kata_hints(track: &Track, report: &mut Report) {
    for node in track.nodes() {
        if node.kata.as_ref().is_some_and(|kata| kata.hints.is_empty()) {
            report.warning(
                node_location(track, &node.id),
                "kata has no hints: add up to three under `kata.hints`",
            );
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
            "learners see this node as under construction (a lesson needs its document and quiz, a kata its exercise, a checkpoint both)",
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
