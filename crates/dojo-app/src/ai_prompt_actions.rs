//! A: the AI review prompt of the selected node. Lessons get their written exercise reviewed
//! (the learner answers in ANSWER.md); katas get their code reviewed (the learner attaches the
//! files). The dojo saves the prompt and copies it to the clipboard; it never calls an AI.

use crate::app::App;
use crate::review_prompt::{self, KataContext};
use std::fs;
use std::path::{Path, PathBuf};

pub const ANSWER_FILE: &str = "ANSWER.md";

impl App {
    pub fn ai_review_prompt(&mut self) {
        let track = self.active_track();
        let Some((phase, node)) = self.tree.selected(track) else {
            return;
        };
        let dir = self.workspace.kata_dir(track, phase, node);
        let outcome = if let Some(kata) = &node.kata {
            if !dir.exists() {
                Err("Start the kata first ([Enter]), write some code, then press [A] for a review prompt.".to_string())
            } else {
                let files = review_prompt::kata_files(&dir, kata.language());
                let key = (track.id.clone(), node.id.clone());
                let last_check = self.check_history.get(&key).map(|(record, _)| {
                    format!(
                        "{}/{} tests passed ({})",
                        record.passed_tests,
                        record.total_tests,
                        record.outcome.as_str().replace('_', " ")
                    )
                });
                let prompt = review_prompt::kata_prompt(&KataContext {
                    track,
                    phase,
                    node,
                    statement: &kata.statement,
                    language: kata.language(),
                    kata_dir: &dir,
                    files: &files,
                    last_check,
                });
                let attach = files
                    .iter()
                    .map(|file| file.display().to_string().replace('\\', "/"))
                    .collect::<Vec<_>>()
                    .join(", ");
                Ok((prompt, format!(" Attach: {attach}.")))
            }
        } else if let Some(exercise) = &node.exercise {
            let answer_file = dir.join(ANSWER_FILE);
            match fs::read_to_string(&answer_file)
                .ok()
                .as_deref()
                .and_then(review_prompt::answer_from)
            {
                Some(answer) => Ok((
                    review_prompt::lesson_prompt(track, phase, node, exercise, &answer),
                    String::new(),
                )),
                None => {
                    Err(self
                        .start_answer(&answer_file, review_prompt::answer_template(node, exercise)))
                }
            }
        } else {
            Err("This node has no written exercise to review.".to_string())
        };
        let node_id = node.id.clone();
        self.status_message = Some(match outcome {
            Ok((prompt, extra)) => self.deliver_prompt(&node_id, &prompt, &extra),
            Err(message) => message,
        });
    }

    /// Creates ANSWER.md from the template if needed and opens it in the editor.
    fn start_answer(&self, answer_file: &Path, template: String) -> String {
        if !answer_file.exists() {
            if let Some(parent) = answer_file.parent()
                && let Err(error) = fs::create_dir_all(parent)
            {
                return format!("⚠ Could not create {}: {error}", parent.display());
            }
            if let Err(error) = fs::write(answer_file, template) {
                return format!("⚠ Could not write {}: {error}", answer_file.display());
            }
        }
        let opened = match self.editor.open_file(answer_file) {
            Ok(()) => "It is open in your editor.".to_string(),
            Err(error) => format!("({error})"),
        };
        format!(
            "✍ Write your answer below the line in {} and save. {opened} Then press [A] again for the review prompt.",
            answer_file.display()
        )
    }

    /// Saves the prompt under <workspace>/.dojo/prompts and copies it to the clipboard.
    pub(crate) fn deliver_prompt(&self, node_id: &str, prompt: &str, extra: &str) -> String {
        let path = self.prompt_path(node_id);
        if let Some(parent) = path.parent() {
            let _ = fs::create_dir_all(parent);
        }
        if let Err(error) = fs::write(&path, prompt) {
            return format!("⚠ Could not save the prompt: {error}");
        }
        let copied = match self.clipboard.map(|copy| copy(prompt)) {
            Some(Ok(())) => "copied to the clipboard and saved",
            Some(Err(_)) | None => "saved",
        };
        format!(
            "🤖 Review prompt {copied} to {}. Paste it into your AI chat (feedback only, it does not change your progress).{extra}",
            path.display()
        )
    }

    pub(crate) fn prompt_path(&self, node_id: &str) -> PathBuf {
        self.workspace
            .root()
            .join(".dojo")
            .join("prompts")
            .join(format!("{node_id}.md"))
    }
}
