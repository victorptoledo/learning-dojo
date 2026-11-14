use crate::models::{Profile, Track};
use anyhow::{Context, Result};
use std::fs;
use std::path::Path;

#[derive(Debug, PartialEq, Eq, Clone, Copy)]
pub enum AppMode {
    TrackView,
    TrackSelector,
}

pub struct AppState {
    pub tracks: Vec<Track>,
    pub current_track_idx: usize,
    pub profile: Profile,
    pub selected_node_idx: usize,
    pub selector_cursor_idx: usize,
    pub mode: AppMode,
    pub profile_path: String,
    pub status_message: Option<String>,
}

impl AppState {
    pub fn new(tracks_dir: &str, profile_path: &str) -> Result<Self> {
        let mut tracks = Vec::new();

        let dir_path = Path::new(tracks_dir);
        if dir_path.exists() && dir_path.is_dir() {
            for entry in fs::read_dir(dir_path)? {
                let entry = entry?;
                let path = entry.path();
                if path.is_dir() {
                    let track_file = path.join("track.json");
                    if track_file.exists() {
                        let content = fs::read_to_string(&track_file)
                            .with_context(|| format!("Falha ao ler {}", track_file.display()))?;
                        match serde_json::from_str::<Track>(&content) {
                            Ok(track) => tracks.push(track),
                            Err(e) => eprintln!("Aviso: erro ao carregar {}: {}", track_file.display(), e),
                        }
                    }
                }
            }
        }

        if tracks.is_empty() {
            anyhow::bail!("Nenhuma trilha encontrada em {}", tracks_dir);
        }

        // Ordenar alfabeticamente ou manter Rust em primeiro se existir
        tracks.sort_by(|a, b| {
            if a.id == "rust" {
                std::cmp::Ordering::Less
            } else if b.id == "rust" {
                std::cmp::Ordering::Greater
            } else {
                a.name.cmp(&b.name)
            }
        });

        let mut profile = if Path::new(profile_path).exists() {
            let content = fs::read_to_string(profile_path).unwrap_or_default();
            serde_json::from_str(&content).unwrap_or_default()
        } else {
            Profile::default()
        };

        // Identifica qual trilha deve estar ativa
        let current_track_idx = tracks
            .iter()
            .position(|t| t.id == profile.active_track_id)
            .unwrap_or(0);

        profile.active_track_id = tracks[current_track_idx].id.clone();

        Ok(Self {
            tracks,
            current_track_idx,
            profile,
            selected_node_idx: 0,
            selector_cursor_idx: current_track_idx,
            mode: AppMode::TrackView,
            profile_path: profile_path.to_string(),
            status_message: None,
        })
    }

    pub fn active_track(&self) -> &Track {
        &self.tracks[self.current_track_idx]
    }

    pub fn save_profile(&self) -> Result<()> {
        if let Some(parent) = Path::new(&self.profile_path).parent() {
            fs::create_dir_all(parent)?;
        }
        let json = serde_json::to_string_pretty(&self.profile)?;
        fs::write(&self.profile_path, json)?;
        Ok(())
    }

    pub fn is_unlocked(&self, node_index: usize) -> bool {
        let track = self.active_track();
        if let Some(node) = track.nodes.get(node_index) {
            node.prerequisites
                .iter()
                .all(|req| self.profile.is_node_completed(&track.id, req))
        } else {
            false
        }
    }

    pub fn next_node(&mut self) {
        let total = self.active_track().nodes.len();
        if total > 0 {
            if self.selected_node_idx + 1 < total {
                self.selected_node_idx += 1;
            } else {
                self.selected_node_idx = 0;
            }
        }
    }

    pub fn prev_node(&mut self) {
        let total = self.active_track().nodes.len();
        if total > 0 {
            if self.selected_node_idx > 0 {
                self.selected_node_idx -= 1;
            } else {
                self.selected_node_idx = total - 1;
            }
        }
    }

    pub fn toggle_selected(&mut self) {
        let track = self.active_track().clone();
        if let Some(node) = track.nodes.get(self.selected_node_idx).cloned() {
            let is_comp = self.profile.is_node_completed(&track.id, &node.id);
            if !self.is_unlocked(self.selected_node_idx) && !is_comp {
                self.status_message = Some("🔒 Conclua os pré-requisitos antes de dominar este kata!".to_string());
                return;
            }
            self.profile.toggle_complete(&track, &node.id, node.xp);
            let _ = self.save_profile();
            if self.profile.is_node_completed(&track.id, &node.id) {
                self.status_message = Some(format!("⭐ Kata dominado! +{} XP!", node.xp));
            } else {
                self.status_message = Some(format!("Kata desmarcado (-{} XP).", node.xp));
            }
        }
    }

    // Gerenciamento do seletor de trilhas
    pub fn open_track_selector(&mut self) {
        self.selector_cursor_idx = self.current_track_idx;
        self.mode = AppMode::TrackSelector;
        self.status_message = Some("Navegue pelas jornadas e aperte [Enter] para entrar no tatame!".to_string());
    }

    pub fn close_track_selector(&mut self) {
        self.mode = AppMode::TrackView;
        self.status_message = None;
    }

    pub fn next_track_in_selector(&mut self) {
        if self.selector_cursor_idx + 1 < self.tracks.len() {
            self.selector_cursor_idx += 1;
        } else {
            self.selector_cursor_idx = 0;
        }
    }

    pub fn prev_track_in_selector(&mut self) {
        if self.selector_cursor_idx > 0 {
            self.selector_cursor_idx -= 1;
        } else {
            self.selector_cursor_idx = self.tracks.len() - 1;
        }
    }

    pub fn confirm_track_selection(&mut self) {
        self.current_track_idx = self.selector_cursor_idx;
        self.selected_node_idx = 0;
        self.profile.active_track_id = self.active_track().id.clone();
        let _ = self.save_profile();
        self.mode = AppMode::TrackView;
        self.status_message = Some(format!("🥋 Entrou na jornada: {}", self.active_track().name));
    }
}
