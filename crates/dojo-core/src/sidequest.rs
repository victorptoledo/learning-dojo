//! Sidequests: optional projects of a track, built in stages and graded by contract
//! tests (decisions D028 and D030). A sidequest unlocks on its own once the learner
//! meets every requirement, such as `rust:P2` (the Rust checkpoint 2 is done). Its
//! stages open one at a time; each stage passed gives sidequest XP once, and passing
//! every stage earns the sidequest's badge. Nothing here completes a node or counts
//! towards belts.

use crate::catalog::Catalog;
use crate::progress::Profile;
use crate::track::{Kata, Node, Track};
use std::fmt;
use std::path::PathBuf;

/// Sidequest XP for passing a stage the first time.
pub const SIDEQUEST_STAGE_XP: u32 = 30;

/// How far into a track a requirement asks the learner to be: `P<n>` is the end of phase
/// `p<n>` (D046: tracks have as many phases as their subject needs, with no fixed limit).
#[derive(Debug, Clone, Copy, PartialEq, Eq, PartialOrd, Ord, Hash)]
pub struct Milestone(u8);

impl Milestone {
    pub const P1: Self = Self(1);
    pub const P2: Self = Self(2);
    pub const P3: Self = Self(3);
    pub const P4: Self = Self(4);

    /// The phase number, which is also the level of a sidequest that asks for it on its
    /// own track.
    pub fn level(self) -> u8 {
        self.0
    }

    fn parse(text: &str) -> Option<Self> {
        let digits = text.strip_prefix('P')?;
        if digits.starts_with('0') || !digits.bytes().all(|b| b.is_ascii_digit()) {
            return None;
        }
        digits.parse().ok().map(Self)
    }

    fn phase_id(self) -> String {
        format!("p{}", self.0)
    }
}

impl fmt::Display for Milestone {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        write!(f, "P{}", self.level())
    }
}

/// `track:P1`, `track:P2`, ...: evidence of progress on a track, never a manual tick.
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct SidequestRequirement {
    pub track: String,
    pub milestone: Milestone,
}

impl SidequestRequirement {
    /// Parses `rust:P2`. `None` for anything else.
    pub fn parse(text: &str) -> Option<Self> {
        let (track, milestone) = text.split_once(':')?;
        if track.is_empty() {
            return None;
        }
        Some(Self {
            track: track.to_string(),
            milestone: Milestone::parse(milestone)?,
        })
    }

    /// The nodes whose completion proves the milestone: the checkpoint of its phase, or,
    /// when the phase has none yet, every node of the phases up to it. So `P4` of a track
    /// that ends at `p4` without a checkpoint is the whole track. Empty when the track or
    /// phase does not exist or the phase has no nodes yet.
    pub fn evidence<'a>(&self, track: &'a Track) -> Vec<&'a Node> {
        let Some(phase) = track
            .phases
            .iter()
            .find(|phase| phase.id == self.milestone.phase_id())
        else {
            return Vec::new();
        };
        if phase.nodes.is_empty() {
            return Vec::new();
        }
        if let Some(checkpoint) = phase.checkpoint() {
            return vec![checkpoint];
        }
        track
            .phases
            .iter()
            .filter(|earlier| earlier.order <= phase.order)
            .flat_map(|earlier| earlier.nodes.iter())
            .collect()
    }

    /// Met when every evidence node is completed. Nodes skipped by the placement test
    /// count only once the Belt Exam confirms them (they then become completions).
    pub fn is_met(&self, catalog: &Catalog, profile: &Profile) -> bool {
        let Some(track) = catalog.track(&self.track) else {
            return false;
        };
        let nodes = self.evidence(track);
        !nodes.is_empty()
            && nodes
                .iter()
                .all(|node| profile.is_completed(&track.id, &node.id))
    }

    /// What the learner reads, such as `Checkpoint 2 of Go`, `the final exam of Design
    /// Patterns`, `the Advanced phase of Linux` or `all of Linux`.
    pub fn label(&self, catalog: &Catalog) -> String {
        let track = catalog.track(&self.track);
        let name = track.map_or(self.track.as_str(), |track| track.name.as_str());
        let level = self.milestone.level();
        let phase_id = self.milestone.phase_id();
        let phase = track.and_then(|track| track.phases.iter().find(|phase| phase.id == phase_id));
        let is_last = track
            .and_then(|track| track.phases.last())
            .is_some_and(|last| last.id == phase_id);
        match phase {
            Some(phase) if phase.checkpoint().is_some() && is_last => {
                format!("the final exam of {name}")
            }
            Some(phase) if phase.checkpoint().is_some() => format!("Checkpoint {level} of {name}"),
            Some(_) if is_last => format!("all of {name}"),
            Some(phase) => format!("the {} phase of {name}", phase.title),
            None => format!("phase {level} of {name}"),
        }
    }
}

impl fmt::Display for SidequestRequirement {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        write!(f, "{}:{}", self.track, self.milestone)
    }
}

/// One stage of a sidequest. Its `kata` holds the stage statement, the files it adds
/// to the learner's folder when it opens (`starter_dir`, which may not exist after the
/// first stage), its contract tests, its reference solution and its runner.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct SidequestStage {
    /// `<sidequest id>.<stage slug>`: used for checks and events.
    pub id: String,
    pub slug: String,
    pub title: String,
    pub kata: Kata,
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct Sidequest {
    /// `<track>.sidequest.<slug>`.
    pub id: String,
    pub slug: String,
    /// The main track: the sidequest is listed there.
    pub track_id: String,
    /// 1 to 4 (N1..N4): orders difficulty only, a level never needs the one before.
    pub level: u8,
    pub title: String,
    pub summary: String,
    /// The project brief in Markdown, shown above the open stages.
    pub statement: String,
    pub skills: Vec<String>,
    pub requires: Vec<SidequestRequirement>,
    pub stages: Vec<SidequestStage>,
}

/// One run of a stage check: an image and the contract tests it runs. `kata` carries
/// the runner and the build cache key; `tests_dirs` replaces its single tests folder.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct CheckPart {
    pub kata: Kata,
    pub tests_dirs: Vec<PathBuf>,
}

impl Sidequest {
    /// Requirements not met yet, in the order the manifest lists them.
    pub fn missing<'a>(
        &'a self,
        catalog: &Catalog,
        profile: &Profile,
    ) -> Vec<&'a SidequestRequirement> {
        self.requires
            .iter()
            .filter(|requirement| !requirement.is_met(catalog, profile))
            .collect()
    }

    pub fn is_unlocked(&self, catalog: &Catalog, profile: &Profile) -> bool {
        self.missing(catalog, profile).is_empty()
    }

    /// Index of the open stage (the first one not passed), `None` once all are passed.
    pub fn current_stage(&self, profile: &Profile) -> Option<usize> {
        self.stages
            .iter()
            .position(|stage| !profile.stage_passed(&stage.id))
    }

    pub fn passed_stages(&self, profile: &Profile) -> usize {
        self.stages
            .iter()
            .filter(|stage| profile.stage_passed(&stage.id))
            .count()
    }

    /// Every stage passed: the badge is earned.
    pub fn is_done(&self, profile: &Profile) -> bool {
        !self.stages.is_empty() && self.current_stage(profile).is_none()
    }

    /// The runs that check stage `index`. The contract tests of every stage up to it
    /// keep running, so a later stage cannot break an earlier one; stages that share
    /// an image run together, in the order their image first appears.
    pub fn check_plan(&self, index: usize) -> Vec<CheckPart> {
        let mut parts: Vec<CheckPart> = Vec::new();
        for stage in self.stages.iter().take(index + 1) {
            let image = &stage.kata.runner.image;
            match parts
                .iter_mut()
                .find(|part| &part.kata.runner.image == image)
            {
                Some(part) => {
                    part.tests_dirs.push(stage.kata.tests_dir.clone());
                    let timeout = &mut part.kata.runner.timeout_secs;
                    *timeout = (*timeout).max(stage.kata.runner.timeout_secs);
                }
                None => parts.push(CheckPart {
                    kata: stage.kata.clone(),
                    tests_dirs: vec![stage.kata.tests_dir.clone()],
                }),
            }
        }
        parts
    }

    /// The stage with this id and its index.
    pub fn stage(&self, stage_id: &str) -> Option<(usize, &SidequestStage)> {
        self.stages
            .iter()
            .enumerate()
            .find(|(_, stage)| stage.id == stage_id)
    }
}

impl Catalog {
    /// The sidequests of `track_id`, by level.
    pub fn sidequests_of(&self, track_id: &str) -> Vec<&Sidequest> {
        let mut found: Vec<&Sidequest> = self
            .sidequests
            .iter()
            .filter(|sidequest| sidequest.track_id == track_id)
            .collect();
        found.sort_by_key(|sidequest| (sidequest.level, sidequest.id.clone()));
        found
    }

    pub fn sidequest(&self, sidequest_id: &str) -> Option<&Sidequest> {
        self.sidequests
            .iter()
            .find(|sidequest| sidequest.id == sidequest_id)
    }

    /// The sidequest a stage id belongs to.
    pub fn sidequest_of_stage(&self, stage_id: &str) -> Option<&Sidequest> {
        self.sidequests
            .iter()
            .find(|sidequest| sidequest.stage(stage_id).is_some())
    }
}

#[cfg(test)]
pub(crate) mod fixtures {
    use super::*;
    use crate::track::RunnerSpec;

    pub fn stage(sidequest_id: &str, slug: &str, image: &str) -> SidequestStage {
        let root = PathBuf::from("/content").join(sidequest_id).join(slug);
        SidequestStage {
            id: format!("{sidequest_id}.{slug}"),
            slug: slug.to_string(),
            title: slug.to_string(),
            kata: Kata {
                statement: format!("# {slug}"),
                starter_dir: root.join("starter"),
                tests_dir: root.join("tests"),
                reference_dir: root.join("reference"),
                runner: RunnerSpec {
                    image: image.to_string(),
                    timeout_secs: 60,
                },
                hints: vec![],
            },
        }
    }

    pub fn sidequest(track_id: &str, level: u8, requires: &[&str]) -> Sidequest {
        let id = format!("{track_id}.sidequest.n{level}");
        Sidequest {
            stages: vec![
                stage(&id, "one", "dojo-rust"),
                stage(&id, "two", "dojo-rust"),
            ],
            id,
            slug: format!("n{level}"),
            track_id: track_id.to_string(),
            level,
            title: format!("Sidequest {level}"),
            summary: String::new(),
            statement: String::new(),
            skills: vec![],
            requires: requires
                .iter()
                .map(|text| SidequestRequirement::parse(text).unwrap())
                .collect(),
        }
    }
}

#[cfg(test)]
mod tests {
    use super::fixtures::{sidequest, stage};
    use super::*;
    use crate::progress::Settings;
    use crate::track::NodeKind;
    use crate::track::fixtures::{node, track};
    use chrono::NaiveDate;

    fn day() -> NaiveDate {
        NaiveDate::from_ymd_opt(2026, 11, 16).unwrap()
    }

    fn checkpoint(id: &str) -> Node {
        Node {
            kind: NodeKind::Checkpoint,
            ..node(id, &[], 100)
        }
    }

    /// p0: setup · p1: loops, checkpoint · p2: traits, checkpoint · p3: threads, async
    /// (no checkpoint yet) · p4: production, checkpoint (the final exam)
    fn catalog() -> Catalog {
        let rust = track(vec![
            vec![node("rust.p0.setup", &[], 10)],
            vec![
                node("rust.p1.loops", &[], 10),
                checkpoint("rust.p1.checkpoint"),
            ],
            vec![
                node("rust.p2.traits", &[], 10),
                checkpoint("rust.p2.checkpoint"),
            ],
            vec![
                node("rust.p3.threads", &[], 10),
                node("rust.p3.async", &[], 10),
            ],
            vec![
                node("rust.p4.production", &[], 10),
                checkpoint("rust.p4.checkpoint"),
            ],
        ]);
        let mut linux = track(vec![
            vec![node("linux.p0.setup", &[], 10)],
            vec![node("linux.p1.shell", &[], 10)],
        ]);
        linux.id = "linux".into();
        linux.name = "Linux".into();
        Catalog {
            tracks: vec![rust, linux],
            ..Catalog::default()
        }
    }

    fn complete(catalog: &Catalog, profile: &mut Profile, node_id: &str) {
        let track_id = node_id.split('.').next().unwrap();
        let track = catalog.track(track_id).unwrap();
        let node = track.node(node_id).unwrap();
        // Unlocking order is the track's business: a completion event is enough here.
        profile.apply(&crate::ProgressEvent::NodeCompleted {
            track_id: track.id.clone(),
            node_id: node.id.clone(),
            xp: node.xp,
            on: day(),
        });
    }

    fn profile() -> Profile {
        Profile::new(Settings::new("rust"))
    }

    #[test]
    fn requirements_parse_only_track_and_milestone() {
        let parsed = SidequestRequirement::parse("rust:P3").unwrap();
        assert_eq!(parsed.track, "rust");
        assert_eq!(parsed.milestone, Milestone::P3);
        assert_eq!(parsed.to_string(), "rust:P3");
        assert_eq!(
            SidequestRequirement::parse("rust:P7")
                .unwrap()
                .milestone
                .level(),
            7,
            "tracks may have more than four phases (D046)"
        );
        assert_eq!(
            SidequestRequirement::parse("rust:P12")
                .unwrap()
                .milestone
                .level(),
            12,
            "the number of phases has no fixed limit (D046)"
        );
        for bad in ["rust", "rust:P0", "rust:P01", "rust:P+1", ":P1", "rust:p1", "rust:P1:x"] {
            assert!(SidequestRequirement::parse(bad).is_none(), "{bad}");
        }
    }

    #[test]
    fn a_milestone_is_its_phase_checkpoint_or_every_phase_up_to_it() {
        let catalog = catalog();
        let mut profile = profile();
        let met = |profile: &Profile, text: &str| {
            SidequestRequirement::parse(text)
                .unwrap()
                .is_met(&catalog, profile)
        };
        assert!(!met(&profile, "rust:P1"));

        complete(&catalog, &mut profile, "rust.p1.checkpoint");
        assert!(met(&profile, "rust:P1"), "the checkpoint alone proves P1");
        assert!(!met(&profile, "rust:P2"));

        complete(&catalog, &mut profile, "rust.p2.checkpoint");
        complete(&catalog, &mut profile, "rust.p3.threads");
        complete(&catalog, &mut profile, "rust.p3.async");
        assert!(met(&profile, "rust:P2"));
        assert!(
            !met(&profile, "rust:P3"),
            "p3 has no checkpoint yet: P3 needs every node up to it"
        );
        for id in ["rust.p0.setup", "rust.p1.loops", "rust.p2.traits"] {
            complete(&catalog, &mut profile, id);
        }
        assert!(met(&profile, "rust:P3"));

        assert!(!met(&profile, "rust:P4"));
        complete(&catalog, &mut profile, "rust.p4.checkpoint");
        assert!(met(&profile, "rust:P4"), "the final exam proves P4");
        assert!(
            !met(&profile, "rust:P5"),
            "a phase the track lacks is never met"
        );

        // A phase without a checkpoint: P1 means every node up to it.
        assert!(!met(&profile, "linux:P1"));
        complete(&catalog, &mut profile, "linux.p1.shell");
        assert!(!met(&profile, "linux:P1"), "p0 is still open");
        complete(&catalog, &mut profile, "linux.p0.setup");
        assert!(met(&profile, "linux:P1"));
        assert!(!met(&profile, "go:P1"), "an unknown track is never met");
    }

    #[test]
    fn placed_out_checkpoints_count_only_after_the_belt_exam() {
        let catalog = catalog();
        let mut profile = profile();
        profile.apply(&crate::ProgressEvent::NodePlacedOut {
            track_id: "rust".into(),
            node_id: "rust.p1.checkpoint".into(),
            on: day(),
        });
        let p1 = SidequestRequirement::parse("rust:P1").unwrap();
        assert!(!p1.is_met(&catalog, &profile), "skipped, not confirmed");

        // The Belt Exam passing turns the skipped node into a completion.
        complete(&catalog, &mut profile, "rust.p1.checkpoint");
        assert!(p1.is_met(&catalog, &profile));
    }

    #[test]
    fn a_locked_sidequest_lists_what_is_missing() {
        let catalog = catalog();
        let mut profile = profile();
        let quest = sidequest("rust", 1, &["rust:P1", "linux:P1"]);
        let labels = |profile: &Profile| -> Vec<String> {
            quest
                .missing(&catalog, profile)
                .iter()
                .map(|requirement| requirement.label(&catalog))
                .collect()
        };
        assert_eq!(labels(&profile), ["Checkpoint 1 of Rust", "all of Linux"]);
        complete(&catalog, &mut profile, "rust.p1.checkpoint");
        assert_eq!(labels(&profile), ["all of Linux"]);
        assert!(!quest.is_unlocked(&catalog, &profile));
        complete(&catalog, &mut profile, "linux.p0.setup");
        complete(&catalog, &mut profile, "linux.p1.shell");
        assert!(quest.is_unlocked(&catalog, &profile));

        let p3 = SidequestRequirement::parse("rust:P3").unwrap();
        assert_eq!(p3.label(&catalog), "the Phase 3 phase of Rust");
        let final_exam = SidequestRequirement::parse("rust:P4").unwrap();
        assert_eq!(final_exam.label(&catalog), "the final exam of Rust");
        let missing_phase = SidequestRequirement::parse("linux:P4").unwrap();
        assert_eq!(missing_phase.label(&catalog), "phase 4 of Linux");
    }

    #[test]
    fn stages_open_one_at_a_time_and_xp_is_given_once() {
        let quest = sidequest("rust", 2, &["rust:P2"]);
        let mut profile = profile();
        assert_eq!(quest.current_stage(&profile), Some(0));

        let event = profile
            .pass_stage("rust", &quest.stages[0].id, day())
            .unwrap();
        profile.apply(&event);
        assert_eq!(profile.sidequest_xp, SIDEQUEST_STAGE_XP);
        assert_eq!(profile.total_xp, 0, "sidequest XP stays apart from belts");
        assert_eq!(profile.streak_on(day()), 1, "a stage counts as training");
        assert_eq!(quest.current_stage(&profile), Some(1));
        assert!(!quest.is_done(&profile));
        assert!(
            profile
                .pass_stage("rust", &quest.stages[0].id, day())
                .is_none()
        );
        profile.apply(&event);
        assert_eq!(
            profile.sidequest_xp, SIDEQUEST_STAGE_XP,
            "replay is idempotent"
        );

        let event = profile
            .pass_stage("rust", &quest.stages[1].id, day())
            .unwrap();
        profile.apply(&event);
        assert_eq!(quest.current_stage(&profile), None);
        assert!(quest.is_done(&profile));
        assert_eq!(quest.passed_stages(&profile), 2);
        assert_eq!(profile.sidequest_xp, 2 * SIDEQUEST_STAGE_XP);
    }

    #[test]
    fn a_stage_check_keeps_running_earlier_tests_grouped_by_image() {
        let mut quest = sidequest("rust", 3, &["rust:P3"]);
        quest.stages.push(stage(&quest.id, "alerts", "dojo-devops"));
        quest.stages.push(stage(&quest.id, "four", "dojo-rust"));
        quest.stages[3].kata.runner.timeout_secs = 200;

        let first = quest.check_plan(0);
        assert_eq!(first.len(), 1);
        assert_eq!(
            first[0].tests_dirs,
            [quest.stages[0].kata.tests_dir.clone()]
        );

        let last = quest.check_plan(3);
        assert_eq!(last.len(), 2);
        assert_eq!(last[0].kata.runner.image, "dojo-rust");
        assert_eq!(
            last[0].tests_dirs,
            [0, 1, 3].map(|i| quest.stages[i].kata.tests_dir.clone())
        );
        assert_eq!(
            last[0].kata.starter_dir, quest.stages[0].kata.starter_dir,
            "one build cache per sidequest and image"
        );
        assert_eq!(last[0].kata.runner.timeout_secs, 200);
        assert_eq!(last[1].kata.runner.image, "dojo-devops");
        assert_eq!(last[1].tests_dirs, [quest.stages[2].kata.tests_dir.clone()]);
    }

    #[test]
    fn the_catalog_lists_a_tracks_sidequests_by_level() {
        let mut catalog = catalog();
        catalog.sidequests = vec![
            sidequest("rust", 2, &["rust:P2"]),
            sidequest("linux", 1, &["linux:P1"]),
            sidequest("rust", 1, &["rust:P1"]),
        ];
        let levels: Vec<u8> = catalog
            .sidequests_of("rust")
            .iter()
            .map(|quest| quest.level)
            .collect();
        assert_eq!(levels, [1, 2]);
        let stage_id = &catalog.sidequests[0].stages[1].id;
        assert_eq!(
            catalog.sidequest_of_stage(stage_id).unwrap().id,
            "rust.sidequest.n2"
        );
        assert!(catalog.sidequest("rust.sidequest.n9").is_none());
    }
}
