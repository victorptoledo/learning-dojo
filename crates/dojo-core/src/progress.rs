use crate::track::{Belt, Node, Track};
use chrono::NaiveDate;
use serde::{Deserialize, Serialize};
use std::collections::{BTreeMap, HashMap, HashSet};

const DEFAULT_SOURCE_LANGUAGE: &str = "java";

/// Learner progress. Temporary JSON-backed model until the SQLite store (M1) replaces it.
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Profile {
    pub name: String,
    pub total_xp: u32,
    pub current_streak: u32,
    pub best_streak: u32,
    pub last_trained: Option<NaiveDate>,
    pub active_track_id: String,
    /// Source language used to pick the Rosetta bridge of each node.
    #[serde(default = "default_source_language")]
    pub bridge_from: String,
    /// Completed node IDs per track.
    #[serde(default)]
    pub track_progress: HashMap<String, HashSet<String>>,
}

fn default_source_language() -> String {
    DEFAULT_SOURCE_LANGUAGE.to_string()
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum ToggleOutcome {
    Completed { xp: u32 },
    Uncompleted { xp: u32 },
    Locked,
}

impl Profile {
    pub fn new(name: &str, active_track_id: &str) -> Self {
        Self {
            name: name.to_string(),
            total_xp: 0,
            current_streak: 0,
            best_streak: 0,
            last_trained: None,
            active_track_id: active_track_id.to_string(),
            bridge_from: default_source_language(),
            track_progress: HashMap::new(),
        }
    }

    pub fn is_completed(&self, track_id: &str, node_id: &str) -> bool {
        self.track_progress
            .get(track_id)
            .is_some_and(|completed| completed.contains(node_id))
    }

    pub fn completed_count(&self, track: &Track) -> usize {
        track
            .nodes()
            .filter(|node| self.is_completed(&track.id, &node.id))
            .count()
    }

    pub fn is_unlocked(&self, track: &Track, node: &Node) -> bool {
        node.prerequisites
            .iter()
            .all(|prerequisite| self.is_completed(&track.id, prerequisite))
    }

    /// Manual completion toggle, kept for dev mode until katas are checked by the runner.
    pub fn toggle_complete(
        &mut self,
        track: &Track,
        node: &Node,
        today: NaiveDate,
    ) -> ToggleOutcome {
        if self.is_completed(&track.id, &node.id) {
            self.uncomplete(&track.id, node);
            return ToggleOutcome::Uncompleted { xp: node.xp };
        }
        if !self.is_unlocked(track, node) {
            return ToggleOutcome::Locked;
        }
        self.complete(&track.id, node, today);
        ToggleOutcome::Completed { xp: node.xp }
    }

    fn complete(&mut self, track_id: &str, node: &Node, today: NaiveDate) {
        self.track_progress
            .entry(track_id.to_string())
            .or_default()
            .insert(node.id.clone());
        self.total_xp += node.xp;
        self.record_training(today);
    }

    fn uncomplete(&mut self, track_id: &str, node: &Node) {
        if let Some(completed) = self.track_progress.get_mut(track_id) {
            completed.remove(&node.id);
        }
        self.total_xp = self.total_xp.saturating_sub(node.xp);
    }

    /// Extends the streak on consecutive days and restarts it after a gap.
    pub fn record_training(&mut self, today: NaiveDate) {
        let days_since_last = self.last_trained.map(|last| (today - last).num_days());
        match days_since_last {
            Some(0) => {}
            Some(1) => self.current_streak += 1,
            _ => self.current_streak = 1,
        }
        self.best_streak = self.best_streak.max(self.current_streak);
        self.last_trained = Some(today);
    }

    pub fn track_xp(&self, track: &Track) -> u32 {
        track
            .nodes()
            .filter(|node| self.is_completed(&track.id, &node.id))
            .map(|node| node.xp)
            .sum()
    }

    pub fn current_belt<'a>(&self, track: &'a Track) -> Option<&'a Belt> {
        let xp = self.track_xp(track);
        track.belts.iter().rev().find(|belt| xp >= belt.required_xp)
    }

    pub fn next_belt<'a>(&self, track: &'a Track) -> Option<&'a Belt> {
        let xp = self.track_xp(track);
        track.belts.iter().find(|belt| xp < belt.required_xp)
    }

    /// Rewrites v1 node IDs (`rust-03-ownership-basics`) to their current IDs.
    /// Returns how many entries were migrated.
    pub fn migrate_legacy_ids(&mut self, legacy_to_current: &BTreeMap<String, String>) -> usize {
        let mut migrated = 0;
        for completed in self.track_progress.values_mut() {
            let renamed: HashSet<String> = completed
                .drain()
                .map(|id| match legacy_to_current.get(&id) {
                    Some(current) => {
                        migrated += 1;
                        current.clone()
                    }
                    None => id,
                })
                .collect();
            *completed = renamed;
        }
        migrated
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::track::fixtures::{node, track};

    fn day(d: u32) -> NaiveDate {
        NaiveDate::from_ymd_opt(2026, 11, d).unwrap()
    }

    fn two_node_track() -> Track {
        track(vec![
            vec![node("rust.p0.setup", &[], 100)],
            vec![node("rust.p1.loops", &["rust.p0.setup"], 250)],
        ])
    }

    #[test]
    fn locked_node_cannot_be_completed() {
        let rust = two_node_track();
        let loops = rust.node("rust.p1.loops").unwrap().clone();
        let mut profile = Profile::new("tester", "rust");

        assert_eq!(
            profile.toggle_complete(&rust, &loops, day(14)),
            ToggleOutcome::Locked
        );
        assert_eq!(profile.total_xp, 0);
    }

    #[test]
    fn completing_prerequisite_unlocks_next_node_and_grants_xp() {
        let rust = two_node_track();
        let setup = rust.node("rust.p0.setup").unwrap().clone();
        let loops = rust.node("rust.p1.loops").unwrap().clone();
        let mut profile = Profile::new("tester", "rust");

        profile.toggle_complete(&rust, &setup, day(14));

        assert!(profile.is_unlocked(&rust, &loops));
        assert_eq!(profile.track_xp(&rust), 100);
        assert_eq!(
            profile.toggle_complete(&rust, &setup, day(14)),
            ToggleOutcome::Uncompleted { xp: 100 }
        );
        assert_eq!(profile.total_xp, 0);
    }

    #[test]
    fn belts_follow_track_xp() {
        let rust = two_node_track();
        let setup = rust.node("rust.p0.setup").unwrap().clone();
        let loops = rust.node("rust.p1.loops").unwrap().clone();
        let mut profile = Profile::new("tester", "rust");

        assert_eq!(profile.current_belt(&rust).unwrap().name, "White Belt");
        profile.toggle_complete(&rust, &setup, day(14));
        profile.toggle_complete(&rust, &loops, day(14));

        assert_eq!(profile.current_belt(&rust).unwrap().name, "Yellow Belt");
        assert_eq!(profile.next_belt(&rust).unwrap().name, "Green Belt");
    }

    #[test]
    fn streak_grows_on_consecutive_days_and_restarts_after_a_gap() {
        let mut profile = Profile::new("tester", "rust");

        profile.record_training(day(10));
        profile.record_training(day(10));
        profile.record_training(day(11));
        assert_eq!(profile.current_streak, 2);

        profile.record_training(day(14));
        assert_eq!(profile.current_streak, 1);
        assert_eq!(profile.best_streak, 2);
    }

    #[test]
    fn legacy_ids_are_rewritten_and_unknown_ids_kept() {
        let mut profile = Profile::new("tester", "rust");
        profile.track_progress.insert(
            "rust".into(),
            HashSet::from(["rust-01-setup".to_string(), "custom".to_string()]),
        );
        let map = BTreeMap::from([("rust-01-setup".to_string(), "rust.p0.setup".to_string())]);

        assert_eq!(profile.migrate_legacy_ids(&map), 1);
        assert!(profile.is_completed("rust", "rust.p0.setup"));
        assert!(profile.is_completed("rust", "custom"));
    }

    #[test]
    fn v1_profile_without_bridge_language_defaults_to_java() {
        let v1 = r#"{"name":"Dev","total_xp":0,"current_streak":1,"best_streak":1,
                     "last_trained":"2026-11-14","active_track_id":"backend","track_progress":{}}"#;
        let profile: Profile = serde_json::from_str(v1).unwrap();
        assert_eq!(profile.bridge_from, "java");
    }
}
