use dojo_core::{Node, Phase, Track};

/// One line of the Track › Phase › Node tree.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum TreeRow {
    Phase { phase: usize },
    Node { phase: usize, node: usize },
}

/// Flattened, navigable tree of a track. The cursor only ever rests on node rows.
#[derive(Debug, Clone, Default)]
pub struct TrackTree {
    rows: Vec<TreeRow>,
    cursor: Option<usize>,
}

impl TrackTree {
    pub fn new(track: &Track) -> Self {
        let rows: Vec<TreeRow> = track
            .phases
            .iter()
            .enumerate()
            .flat_map(|(phase_index, phase)| {
                std::iter::once(TreeRow::Phase { phase: phase_index }).chain(
                    (0..phase.nodes.len()).map(move |node| TreeRow::Node {
                        phase: phase_index,
                        node,
                    }),
                )
            })
            .collect();
        let cursor = rows
            .iter()
            .position(|row| matches!(row, TreeRow::Node { .. }));
        Self { rows, cursor }
    }

    pub fn rows(&self) -> &[TreeRow] {
        &self.rows
    }

    pub fn cursor(&self) -> Option<usize> {
        self.cursor
    }

    pub fn selected<'a>(&self, track: &'a Track) -> Option<(&'a Phase, &'a Node)> {
        match self.rows.get(self.cursor?)? {
            TreeRow::Node { phase, node } => {
                let phase = track.phases.get(*phase)?;
                Some((phase, phase.nodes.get(*node)?))
            }
            TreeRow::Phase { .. } => None,
        }
    }

    pub fn move_down(&mut self) {
        self.move_by(1);
    }

    pub fn move_up(&mut self) {
        self.move_by(-1);
    }

    /// Moves to the next node row in `direction`, wrapping around and skipping phase headers.
    /// Phases without nodes are skipped too, so the cursor never lands on an empty phase.
    fn move_by(&mut self, direction: isize) {
        let Some(start) = self.cursor else { return };
        let len = self.rows.len() as isize;
        let mut index = start as isize;
        for _ in 0..len {
            index = (index + direction).rem_euclid(len);
            if matches!(self.rows[index as usize], TreeRow::Node { .. }) {
                self.cursor = Some(index as usize);
                return;
            }
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use dojo_core::{Archetype, ContentStatus, NodeKind};
    use std::collections::BTreeMap;

    fn node(id: &str) -> Node {
        Node {
            id: id.into(),
            legacy_id: None,
            kind: NodeKind::Lesson,
            status: ContentStatus::Draft,
            title: id.into(),
            summary: String::new(),
            objectives: vec![],
            prerequisites: vec![],
            difficulty: 1,
            estimated_minutes: 10,
            xp: 100,
            lesson: None,
            lesson_file: None,
            bridges: BTreeMap::new(),
            kata: None,
            quiz: None,
            requirements: vec![],
        }
    }

    fn phase(id: &str, nodes: Vec<Node>) -> Phase {
        Phase {
            id: id.into(),
            order: 0,
            title: id.into(),
            goal: String::new(),
            nodes,
        }
    }

    fn track(phases: Vec<Phase>) -> Track {
        Track {
            id: "demo".into(),
            name: "Demo".into(),
            archetype: Archetype::Language,
            category: "Test".into(),
            icon: "🧪".into(),
            description: String::new(),
            status: ContentStatus::Draft,
            belts: vec![],
            phases,
            requirements: vec![],
        }
    }

    fn selected_id(tree: &TrackTree, track: &Track) -> String {
        tree.selected(track).unwrap().1.id.clone()
    }

    #[test]
    fn cursor_starts_on_first_node_and_skips_phase_headers_and_empty_phases() {
        let demo = track(vec![
            phase("p0", vec![]),
            phase("p1", vec![node("a"), node("b")]),
            phase("p2", vec![]),
            phase("p3", vec![node("c")]),
        ]);
        let mut tree = TrackTree::new(&demo);

        assert_eq!(selected_id(&tree, &demo), "a");
        tree.move_down();
        assert_eq!(selected_id(&tree, &demo), "b");
        tree.move_down();
        assert_eq!(selected_id(&tree, &demo), "c");
        tree.move_down();
        assert_eq!(selected_id(&tree, &demo), "a", "wraps around");
        tree.move_up();
        assert_eq!(selected_id(&tree, &demo), "c", "wraps backwards");
    }

    #[test]
    fn track_without_nodes_has_no_selection() {
        let empty = track(vec![phase("p0", vec![])]);
        let mut tree = TrackTree::new(&empty);

        tree.move_down();

        assert!(tree.cursor().is_none());
        assert!(tree.selected(&empty).is_none());
    }
}
