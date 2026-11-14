//! Career paths on screen: pick a target career and see what to study next.

use crate::app::{App, Screen};
use dojo_core::{Reason, Recommendation, recommend};

/// How many next steps the career screen and completion messages show.
pub const RECOMMENDATIONS: usize = 3;

impl App {
    /// G: opens the career screen on the target career, or the first one.
    pub fn open_careers(&mut self) {
        if self.catalog.careers.is_empty() {
            self.status_message = Some("No career paths in the content yet.".to_string());
            return;
        }
        self.career_cursor = self
            .profile
            .settings
            .target_career
            .as_deref()
            .and_then(|id| self.catalog.careers.iter().position(|c| c.id == id))
            .unwrap_or(0);
        self.screen = Screen::Career;
        self.status_message = None;
    }

    pub fn close_careers(&mut self) {
        self.screen = Screen::TrackView;
    }

    pub fn career_down(&mut self) {
        let count = self.catalog.careers.len().max(1);
        self.career_cursor = (self.career_cursor + 1) % count;
    }

    pub fn career_up(&mut self) {
        let count = self.catalog.careers.len().max(1);
        self.career_cursor = (self.career_cursor + count - 1) % count;
    }

    /// Enter: makes the highlighted career the target, or clears it if it already is.
    pub fn toggle_target_career(&mut self) {
        let Some(career) = self.catalog.careers.get(self.career_cursor) else {
            return;
        };
        let settings = &mut self.profile.settings;
        if settings.target_career.as_deref() == Some(career.id.as_str()) {
            settings.target_career = None;
            self.status_message =
                Some("No target career: recommendations follow your tracks.".to_string());
        } else {
            settings.target_career = Some(career.id.clone());
            self.status_message = Some(format!(
                "🎯 Target career: {}. Recommendations now aim at it.",
                career.title
            ));
        }
        self.save_settings();
    }

    /// The best nodes to study next across every track.
    pub fn recommendations(&self) -> Vec<Recommendation> {
        recommend(
            &self.catalog,
            &self.catalog.careers,
            &self.profile,
            &self.active_track().id,
            RECOMMENDATIONS,
        )
    }

    /// "Next: Lifetimes (Rust)" after a checkpoint closes a phase.
    pub(crate) fn next_step_hint(&self) -> String {
        self.recommendations()
            .first()
            .and_then(|rec| {
                let track = self.catalog.track(&rec.track_id)?;
                let node = track.node(&rec.node_id)?;
                Some(format!(
                    " 🧭 Next: {} ({}). [G] shows your career paths.",
                    node.title, track.name
                ))
            })
            .unwrap_or_default()
    }
}

/// Why a node is recommended, in a few words.
pub fn reason_text(reasons: &[Reason]) -> String {
    let words: Vec<&str> = reasons
        .iter()
        .map(|reason| match reason {
            Reason::CareerGap => "needed for your target career",
            Reason::WeakSkill => "reinforces a skill you missed",
            Reason::ActiveTrack => "on your current track",
            Reason::Momentum => "continues what you started",
        })
        .collect();
    if words.is_empty() {
        "open to start".to_string()
    } else {
        words.join(", ")
    }
}
