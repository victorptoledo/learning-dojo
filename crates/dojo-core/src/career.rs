//! Career paths: stages of tracks and skills, how far the learner is along each,
//! and which nodes to study next.

use crate::catalog::Catalog;
use crate::progress::Profile;
use crate::track::{Node, Track};
use std::collections::HashSet;

/// A path such as "Backend → Microservices Architect".
#[derive(Debug, Clone, PartialEq)]
pub struct Career {
    pub id: String,
    pub title: String,
    pub summary: String,
    pub roles: Vec<String>,
    pub stages: Vec<Stage>,
    /// Careers that naturally follow this one.
    pub next_paths: Vec<String>,
}

#[derive(Debug, Clone, PartialEq)]
pub struct Stage {
    pub id: String,
    pub title: String,
    pub requires: Vec<CareerRequirement>,
}

#[derive(Debug, Clone, PartialEq)]
pub enum CareerRequirement {
    /// Every node of `track` up to and including `phase` is done.
    TrackPhase { track: String, phase: String },
    /// The share of the nodes teaching `skill` that are done is at least `min`.
    Skill { skill: String, min: f64 },
    /// Any one of these.
    AnyOf(Vec<CareerRequirement>),
}

impl CareerRequirement {
    /// Progress from 0.0 to 1.0. Content that is not written yet counts as 0.
    pub fn progress(&self, catalog: &Catalog, profile: &Profile) -> f64 {
        match self {
            Self::TrackPhase { track, phase } => catalog
                .track(track)
                .map_or(0.0, |track| phase_progress(track, phase, profile)),
            Self::Skill { skill, min } => {
                let mastery = skill_mastery(catalog, profile, skill);
                if *min <= 0.0 {
                    1.0
                } else {
                    (mastery / min).min(1.0)
                }
            }
            Self::AnyOf(options) => options
                .iter()
                .map(|option| option.progress(catalog, profile))
                .fold(0.0, f64::max),
        }
    }

    pub fn is_met(&self, catalog: &Catalog, profile: &Profile) -> bool {
        self.progress(catalog, profile) >= 1.0
    }

    /// Tracks and skills this requirement asks for, to match study candidates.
    fn targets(&self, tracks: &mut HashSet<String>, skills: &mut HashSet<String>) {
        match self {
            Self::TrackPhase { track, .. } => {
                tracks.insert(track.clone());
            }
            Self::Skill { skill, .. } => {
                skills.insert(skill.clone());
            }
            Self::AnyOf(options) => options
                .iter()
                .for_each(|option| option.targets(tracks, skills)),
        }
    }
}

/// Share of the nodes of phases up to `phase` that no longer block (done or placed out).
fn phase_progress(track: &Track, phase: &str, profile: &Profile) -> f64 {
    let Some(limit) = track.phases.iter().find(|p| p.id == phase).map(|p| p.order) else {
        return 0.0;
    };
    let nodes: Vec<&Node> = track
        .phases
        .iter()
        .filter(|p| p.order <= limit)
        .flat_map(|p| p.nodes.iter())
        .collect();
    share(&nodes, |node| profile.is_completed(&track.id, &node.id))
}

/// Share of the catalog's nodes teaching `skill` that are completed.
pub fn skill_mastery(catalog: &Catalog, profile: &Profile, skill: &str) -> f64 {
    let nodes: Vec<(&Track, &Node)> = catalog
        .tracks
        .iter()
        .flat_map(|track| track.nodes().map(move |node| (track, node)))
        .filter(|(_, node)| node.skills().any(|taught| taught == skill))
        .collect();
    share(&nodes, |(track, node)| {
        profile.is_completed(&track.id, &node.id)
    })
}

fn share<T>(items: &[T], done: impl Fn(&T) -> bool) -> f64 {
    if items.is_empty() {
        return 0.0;
    }
    items.iter().filter(|item| done(item)).count() as f64 / items.len() as f64
}

/// How far a learner is along one career.
#[derive(Debug, Clone, PartialEq)]
pub struct CareerProgress {
    /// Progress of each stage, 0.0 to 1.0, in order.
    pub stages: Vec<f64>,
    /// The first stage not finished, if any.
    pub current: Option<usize>,
}

impl CareerProgress {
    pub fn finished_stages(&self) -> usize {
        self.stages
            .iter()
            .filter(|progress| **progress >= 1.0)
            .count()
    }

    /// Overall progress: the mean of the stages.
    pub fn overall(&self) -> f64 {
        if self.stages.is_empty() {
            0.0
        } else {
            self.stages.iter().sum::<f64>() / self.stages.len() as f64
        }
    }
}

impl Career {
    pub fn progress(&self, catalog: &Catalog, profile: &Profile) -> CareerProgress {
        let stages: Vec<f64> = self
            .stages
            .iter()
            .map(|stage| {
                if stage.requires.is_empty() {
                    return 1.0;
                }
                stage
                    .requires
                    .iter()
                    .map(|requirement| requirement.progress(catalog, profile))
                    .sum::<f64>()
                    / stage.requires.len() as f64
            })
            .collect();
        let current = stages.iter().position(|progress| *progress < 1.0);
        CareerProgress { stages, current }
    }

    /// Index of the first stage that asks for `track`, through a track phase or a
    /// skill of that track (skill ids start with their track id: `rust.concurrency`).
    pub fn first_stage_with_track(&self, track: &str) -> Option<usize> {
        self.stages.iter().position(|stage| {
            let mut tracks = HashSet::new();
            let mut skills = HashSet::new();
            stage
                .requires
                .iter()
                .for_each(|requirement| requirement.targets(&mut tracks, &mut skills));
            tracks.contains(track)
                || skills
                    .iter()
                    .any(|skill| skill.split('.').next() == Some(track))
        })
    }

    /// Tracks and skills of the unmet requirements of the current stage.
    fn gap(&self, catalog: &Catalog, profile: &Profile) -> (HashSet<String>, HashSet<String>) {
        let mut tracks = HashSet::new();
        let mut skills = HashSet::new();
        if let Some(stage) = self
            .progress(catalog, profile)
            .current
            .and_then(|index| self.stages.get(index))
        {
            stage
                .requires
                .iter()
                .filter(|requirement| !requirement.is_met(catalog, profile))
                .for_each(|requirement| requirement.targets(&mut tracks, &mut skills));
        }
        (tracks, skills)
    }
}

// ---- recommendations ----------------------------------------------------------

/// Weights of the recommendation score.
pub const WEIGHT_CAREER_GAP: f64 = 0.35;
pub const WEIGHT_WEAK_SKILL: f64 = 0.25;
pub const WEIGHT_ACTIVE_TRACK: f64 = 0.20;
pub const WEIGHT_MOMENTUM: f64 = 0.10;
pub const WEIGHT_DIFFICULTY_JUMP: f64 = 0.10;

/// A node worth studying next, with why.
#[derive(Debug, Clone, PartialEq)]
pub struct Recommendation {
    pub track_id: String,
    pub node_id: String,
    pub score: f64,
    pub reasons: Vec<Reason>,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum Reason {
    /// Needed by the current stage of the target career.
    CareerGap,
    /// Teaches a skill missed in a review.
    WeakSkill,
    /// On the track being trained.
    ActiveTrack,
    /// Continues a track already started.
    Momentum,
}

/// The `limit` best nodes to study next, among the open ones of every track.
///
/// ```text
/// score = 0.35·career_gap (1 for a needed skill, 0.5 for a needed track)
///       + 0.25·weak_skill + 0.20·active_track
///       + 0.10·momentum − 0.10·difficulty_jump
/// ```
pub fn recommend(
    catalog: &Catalog,
    careers: &[Career],
    profile: &Profile,
    active_track: &str,
    limit: usize,
) -> Vec<Recommendation> {
    let (gap_tracks, gap_skills) = profile
        .settings
        .target_career
        .as_deref()
        .and_then(|id| careers.iter().find(|career| career.id == id))
        .map(|career| career.gap(catalog, profile))
        .unwrap_or_default();
    let mut candidates: Vec<Recommendation> = catalog
        .tracks
        .iter()
        .flat_map(|track| {
            let weak = profile.weak_skills(track);
            let started = profile.completed_count(track) > 0;
            let level = profile.average_difficulty(track);
            track
                .nodes()
                .filter(|node| {
                    !profile.is_satisfied(&track.id, &node.id)
                        && !node.under_construction()
                        && profile.is_unlocked(track, node)
                })
                .map(|node| {
                    let mut reasons = Vec::new();
                    let mut score = 0.0;
                    // A node teaching a skill the stage asks for fills the gap fully;
                    // any open node of a track the stage asks for fills half of it.
                    let gap = if node.skills().any(|skill| gap_skills.contains(skill)) {
                        1.0
                    } else if gap_tracks.contains(&track.id) {
                        0.5
                    } else {
                        0.0
                    };
                    if gap > 0.0 {
                        score += WEIGHT_CAREER_GAP * gap;
                        reasons.push(Reason::CareerGap);
                    }
                    if node.skills().any(|skill| weak.contains(skill)) {
                        score += WEIGHT_WEAK_SKILL;
                        reasons.push(Reason::WeakSkill);
                    }
                    if track.id == active_track {
                        score += WEIGHT_ACTIVE_TRACK;
                        reasons.push(Reason::ActiveTrack);
                    }
                    if started {
                        score += WEIGHT_MOMENTUM;
                        reasons.push(Reason::Momentum);
                    }
                    let jump = (f64::from(node.difficulty) - level).max(0.0) / 4.0;
                    score -= WEIGHT_DIFFICULTY_JUMP * jump.min(1.0);
                    Recommendation {
                        track_id: track.id.clone(),
                        node_id: node.id.clone(),
                        score,
                        reasons,
                    }
                })
                .collect::<Vec<_>>()
        })
        .collect();
    // Best score first; ties keep the catalog and track order (stable sort).
    candidates.sort_by(|a, b| b.score.total_cmp(&a.score));
    candidates.truncate(limit);
    candidates
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::progress::{Evidence, Settings};
    use crate::quiz::QuizScore;
    use crate::track::fixtures::{node, track};
    use crate::track::{NodeKind, Objective};
    use chrono::NaiveDate;

    fn day() -> NaiveDate {
        NaiveDate::from_ymd_opt(2026, 11, 14).unwrap()
    }

    fn taught(id: &str, prerequisites: &[&str], skill: &str) -> Node {
        let mut lesson = Node {
            kind: NodeKind::Lesson,
            ..node(id, prerequisites, 100)
        };
        lesson.lesson = Some("# lesson".into());
        lesson.quiz = Some(crate::quiz::Quiz {
            pass_percent: 80,
            questions: vec![],
        });
        lesson.objectives = vec![Objective {
            id: "o1".into(),
            text: String::new(),
            skills: vec![skill.into()],
        }];
        lesson
    }

    /// rust: p0 setup · p1 loops, maps. go: p0 hello.
    fn catalog() -> Catalog {
        let rust = track(vec![
            vec![taught("rust.p0.setup", &[], "rust.setup")],
            vec![
                taught("rust.p1.loops", &[], "rust.loops"),
                taught("rust.p1.maps", &[], "rust.maps"),
            ],
        ]);
        let mut go = track(vec![vec![taught("go.p0.hello", &[], "go.basics")]]);
        go.id = "go".into();
        Catalog {
            careers: vec![],
            sidequests: vec![],
            tracks: vec![rust, go],
            skills: Default::default(),
        }
    }

    fn career() -> Career {
        Career {
            id: "systems".into(),
            title: "Systems".into(),
            summary: String::new(),
            roles: vec![],
            stages: vec![
                Stage {
                    id: "s1".into(),
                    title: "Basics".into(),
                    requires: vec![CareerRequirement::TrackPhase {
                        track: "rust".into(),
                        phase: "p0".into(),
                    }],
                },
                Stage {
                    id: "s2".into(),
                    title: "More".into(),
                    requires: vec![
                        CareerRequirement::Skill {
                            skill: "rust.maps".into(),
                            min: 1.0,
                        },
                        CareerRequirement::AnyOf(vec![
                            CareerRequirement::TrackPhase {
                                track: "go".into(),
                                phase: "p0".into(),
                            },
                            CareerRequirement::TrackPhase {
                                track: "rust".into(),
                                phase: "p1".into(),
                            },
                        ]),
                    ],
                },
            ],
            next_paths: vec![],
        }
    }

    fn complete(profile: &mut Profile, catalog: &Catalog, track_id: &str, node_id: &str) {
        let track = catalog.track(track_id).unwrap();
        let node = track.node(node_id).unwrap();
        let evidence = Evidence::Quiz(QuizScore {
            correct: 1,
            total: 1,
            passed: true,
        });
        let event = profile
            .complete_by_evidence(track, node, evidence, day())
            .unwrap();
        profile.apply(&event);
    }

    #[test]
    fn first_stage_with_track_looks_in_phases_skills_and_any_of() {
        let systems = career();
        assert_eq!(systems.first_stage_with_track("rust"), Some(0));
        assert_eq!(systems.first_stage_with_track("go"), Some(1));
        assert_eq!(systems.first_stage_with_track("java"), None);

        let mut by_skill = career();
        by_skill.stages.remove(0);
        assert_eq!(by_skill.first_stage_with_track("rust"), Some(0));
    }

    #[test]
    fn stages_fill_up_as_their_tracks_and_skills_are_done() {
        let catalog = catalog();
        let mut profile = Profile::new(Settings::new("rust"));
        let systems = career();
        assert_eq!(systems.progress(&catalog, &profile).current, Some(0));

        complete(&mut profile, &catalog, "rust", "rust.p0.setup");
        let progress = systems.progress(&catalog, &profile);
        assert_eq!(progress.stages[0], 1.0);
        assert!(
            (progress.stages[1] - 1.0 / 6.0).abs() < 1e-9,
            "rust up to p1 is a third done, so the any_of is too"
        );
        assert_eq!(progress.current, Some(1));

        complete(&mut profile, &catalog, "rust", "rust.p1.maps");
        let progress = systems.progress(&catalog, &profile);
        assert!(
            (progress.stages[1] - 5.0 / 6.0).abs() < 1e-9,
            "maps mastered, rust up to p1 two thirds done"
        );

        complete(&mut profile, &catalog, "go", "go.p0.hello");
        let progress = systems.progress(&catalog, &profile);
        assert_eq!(progress.current, None, "go p0 satisfies the any_of");
        assert_eq!(progress.finished_stages(), 2);
        assert_eq!(progress.overall(), 1.0);
    }

    #[test]
    fn missing_content_never_counts_as_done() {
        let catalog = catalog();
        let profile = Profile::new(Settings::new("rust"));
        let unknown = CareerRequirement::TrackPhase {
            track: "kotlin".into(),
            phase: "p0".into(),
        };
        assert_eq!(unknown.progress(&catalog, &profile), 0.0);
        assert_eq!(skill_mastery(&catalog, &profile, "nobody.teaches"), 0.0);
    }

    #[test]
    fn the_target_career_gap_comes_first_then_the_active_track() {
        let catalog = catalog();
        let mut profile = Profile::new(Settings::new("rust"));
        complete(&mut profile, &catalog, "rust", "rust.p0.setup");

        let ids = |profile: &Profile| -> Vec<String> {
            recommend(&catalog, &[career()], profile, "rust", 3)
                .into_iter()
                .map(|rec| rec.node_id)
                .collect()
        };
        assert_eq!(
            ids(&profile),
            vec!["rust.p1.loops", "rust.p1.maps", "go.p0.hello"],
            "no target: the active track first"
        );

        profile.settings.target_career = Some("systems".into());
        let top = recommend(&catalog, &[career()], &profile, "rust", 3);
        assert_eq!(top[0].node_id, "rust.p1.maps", "the stage needs rust.maps");
        assert!(top[0].reasons.contains(&Reason::CareerGap));
        assert_eq!(top.len(), 3);
    }
}
