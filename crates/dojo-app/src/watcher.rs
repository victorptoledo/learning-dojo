//! Watches the workspace and reports which kata folder changed, debounced.

use notify::{Event, RecommendedWatcher, RecursiveMode, Watcher};
use std::path::{Component, Path, PathBuf};
use std::sync::mpsc::{self, Receiver};
use std::time::{Duration, Instant};

/// Wait this long after the last save before checking, so one save = one check.
const DEBOUNCE: Duration = Duration::from_millis(700);
const IGNORED_DIRS: [&str; 3] = ["target", ".git", "node_modules"];

pub struct WorkspaceWatcher {
    _watcher: RecommendedWatcher,
    events: Receiver<notify::Result<Event>>,
    pending: Option<(PathBuf, Instant)>,
}

impl WorkspaceWatcher {
    pub fn start(root: &Path) -> notify::Result<Self> {
        let (sender, events) = mpsc::channel();
        let mut watcher = notify::recommended_watcher(sender)?;
        watcher.watch(root, RecursiveMode::Recursive)?;
        Ok(Self {
            _watcher: watcher,
            events,
            pending: None,
        })
    }

    /// Returns a changed path once it has been quiet for the debounce period.
    pub fn settled_change(&mut self) -> Option<PathBuf> {
        while let Ok(Ok(event)) = self.events.try_recv() {
            if let Some(path) = event.paths.into_iter().find(|path| !is_ignored(path)) {
                self.pending = Some((path, Instant::now()));
            }
        }
        let (_, changed_at) = self.pending.as_ref()?;
        if changed_at.elapsed() < DEBOUNCE {
            return None;
        }
        self.pending.take().map(|(path, _)| path)
    }
}

fn is_ignored(path: &Path) -> bool {
    path.components().any(|component| match component {
        Component::Normal(name) => IGNORED_DIRS.iter().any(|ignored| name == *ignored),
        _ => false,
    })
}

#[cfg(test)]
mod tests {
    use super::is_ignored;
    use std::path::Path;

    #[test]
    fn build_output_and_git_changes_are_ignored() {
        assert!(is_ignored(Path::new("ws/rust/p0/hello/target/debug/x")));
        assert!(is_ignored(Path::new("ws/.git/index")));
        assert!(!is_ignored(Path::new("ws/rust/p0/hello/src/lib.rs")));
    }
}
