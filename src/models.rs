use chrono::NaiveDate;
use serde::{Deserialize, Serialize};
use std::collections::{HashMap, HashSet};

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Belt {
    pub name: String,
    pub required_xp: u32,
    pub title: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Rosetta {
    pub concept: String,
    pub java_way: String,
    pub rust_way: String,
    pub sensei_tip: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Node {
    pub id: String,
    pub section: String,
    pub title: String,
    pub description: String,
    pub prerequisites: Vec<String>,
    pub xp: u32,
    pub rosetta: Rosetta,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Track {
    pub id: String,
    pub name: String,
    pub category: String,
    pub description: String,
    pub icon: String,
    pub belts: Vec<Belt>,
    pub nodes: Vec<Node>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Profile {
    pub name: String,
    pub total_xp: u32,
    pub current_streak: u32,
    pub best_streak: u32,
    pub last_trained: Option<NaiveDate>,
    pub active_track_id: String,
    // Progresso separado por ID da trilha: track_id -> Set de IDs de nós dominados
    #[serde(default)]
    pub track_progress: HashMap<String, HashSet<String>>,
}

impl Default for Profile {
    fn default() -> Self {
        Self {
            name: "Dev Sensei".to_string(),
            total_xp: 0,
            current_streak: 1,
            best_streak: 1,
            last_trained: Some(chrono::Local::now().date_naive()),
            active_track_id: "rust".to_string(),
            track_progress: HashMap::new(),
        }
    }
}

impl Profile {
    #[allow(dead_code)]
    pub fn completed_nodes_for<'a>(&'a self, track_id: &str) -> Option<&'a HashSet<String>> {
        self.track_progress.get(track_id)
    }

    pub fn is_node_completed(&self, track_id: &str, node_id: &str) -> bool {
        self.track_progress
            .get(track_id)
            .map(|set| set.contains(node_id))
            .unwrap_or(false)
    }

    pub fn track_xp(&self, track: &Track) -> u32 {
        if let Some(completed) = self.track_progress.get(&track.id) {
            track
                .nodes
                .iter()
                .filter(|n| completed.contains(&n.id))
                .map(|n| n.xp)
                .sum()
        } else {
            0
        }
    }

    pub fn current_belt<'a>(&self, track: &'a Track) -> &'a Belt {
        let xp = self.track_xp(track);
        track
            .belts
            .iter()
            .filter(|b| xp >= b.required_xp)
            .last()
            .unwrap_or(&track.belts[0])
    }

    pub fn next_belt<'a>(&self, track: &'a Track) -> Option<&'a Belt> {
        let xp = self.track_xp(track);
        track.belts.iter().find(|b| xp < b.required_xp)
    }

    pub fn toggle_complete(&mut self, track: &Track, node_id: &str, xp: u32) {
        let entry = self.track_progress.entry(track.id.clone()).or_default();

        if entry.contains(node_id) {
            entry.remove(node_id);
            self.total_xp = self.total_xp.saturating_sub(xp);
        } else {
            entry.insert(node_id.to_string());
            self.total_xp += xp;

            let today = chrono::Local::now().date_naive();
            if let Some(last) = self.last_trained {
                let diff = (today - last).num_days();
                if diff == 1 {
                    self.current_streak += 1;
                    if self.current_streak > self.best_streak {
                        self.best_streak = self.current_streak;
                    }
                } else if diff > 1 {
                    self.current_streak = 1;
                }
            }
            self.last_trained = Some(today);
        }
    }
}
