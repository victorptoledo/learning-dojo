//! Opening a node goes through its setup first: requirements are checked once per
//! session, and the node opens only when nothing required is missing.

use crate::app::{App, NodeKey, Screen};
use crate::setup::{SetupJob, SetupSession};

impl App {
    /// Starts the setup step for the selected node when it is needed.
    /// Returns true when the setup screen took over.
    pub(crate) fn begin_setup_if_needed(&mut self) -> bool {
        let Some(environment) = &self.environment else {
            return false;
        };
        let track = self.active_track();
        let Some((_, node)) = self.tree.selected(track) else {
            return false;
        };
        let key: NodeKey = (track.id.clone(), node.id.clone());
        let requirements: Vec<_> = track.requirements_for(node).into_iter().cloned().collect();
        if node.under_construction() || requirements.is_empty() || self.setup_cleared.contains(&key)
        {
            return false;
        }
        let mut session = SetupSession::new(&track.id, &node.id, &node.title, requirements);
        session.start(environment, SetupJob::Check);
        self.setup = Some(session);
        self.screen = Screen::Setup;
        true
    }

    /// Called every tick: applies finished setup work, and opens the node by itself
    /// when the first check found everything ready.
    pub fn poll_setup(&mut self) {
        let Some(session) = self.setup.as_mut() else {
            return;
        };
        if session.poll() && session.continue_when_ready && session.all_ready() {
            self.setup_continue();
        }
    }

    pub fn setup_run(&mut self, job: SetupJob) {
        if let (Some(session), Some(environment)) = (self.setup.as_mut(), self.environment.as_ref())
        {
            session.start(environment, job);
        }
    }

    /// Enter on the setup screen: continue when nothing required is missing.
    pub fn setup_continue(&mut self) {
        let Some(session) = self.setup.as_ref() else {
            return;
        };
        if session.busy.is_some() {
            return;
        }
        if session.blocks() {
            self.status_message = Some(
                "Something required is still missing: [I] installs, [R] checks again.".to_string(),
            );
            return;
        }
        let key: NodeKey = (session.track_id.clone(), session.node_id.clone());
        self.setup_cleared.insert(key);
        self.close_setup();
        self.activate_selected();
    }

    pub fn close_setup(&mut self) {
        self.setup = None;
        self.screen = Screen::TrackView;
    }
}
