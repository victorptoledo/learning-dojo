mod ai_prompt_actions;
mod app;
#[cfg(test)]
mod app_tests;
mod bridge_picker;
mod career_actions;
mod career_view;
mod checks;
mod clipboard;
mod code_workspace;
mod config;
mod doctor;
mod editor;
mod environment;
mod interview_sim;
mod kata_actions;
mod kata_commands;
mod lesson_actions;
mod lesson_view;
mod mentor_actions;
mod placement_flow;
mod placement_view;
mod practice_actions;
mod quiz_session;
mod review_prompt;
mod setup;
mod setup_actions;
mod setup_view;
mod single_instance;
mod topic_study;
mod tree;
mod tui;
mod ui;
mod validate_command;
mod watcher;
mod workspace;

use anyhow::Result;
use clap::{Parser, Subcommand};
use config::{Config, dojo_home};
use single_instance::Acquired;
use std::path::PathBuf;
use std::process::ExitCode;

/// Learning Dojo: structured learning tracks in your terminal.
#[derive(Parser)]
#[command(name = "dojo", version)]
struct Cli {
    /// Folder with tracks, skills and schemas.
    #[arg(long, global = true, default_value = "content")]
    content: PathBuf,

    /// Progress database. Defaults to `~/.dojo/dojo.db`.
    #[arg(long, global = true)]
    db: Option<PathBuf>,

    /// Folder with your kata solutions (a git repository). Overrides `workspace` in ~/.dojo/config.toml.
    #[arg(long, global = true)]
    workspace: Option<PathBuf>,

    /// v1 progress file imported into the database on first run, then renamed to `profile.v1.json`.
    #[arg(long, global = true, default_value = "data/profile.json")]
    import_v1: PathBuf,

    /// Dev mode: tick any node and undo completions, bypassing the evidence rules.
    #[arg(long, global = true)]
    dev: bool,

    #[command(subcommand)]
    command: Option<Command>,
}

#[derive(Subcommand)]
enum Command {
    /// Work with the content folder.
    Content {
        #[command(subcommand)]
        action: ContentAction,
    },
    /// Manage the Docker images that check katas.
    Runner {
        #[command(subcommand)]
        action: RunnerAction,
    },
    /// Write dojo.code-workspace (lessons + solutions) and open it in VS Code.
    Vscode,
    /// Check that this machine has everything a track needs.
    Doctor {
        #[arg(default_value = "rust")]
        track: String,
    },
}

#[derive(Subcommand)]
enum ContentAction {
    /// Check every track, node, skill and bridge against the schemas and rules.
    Validate {
        /// Treat warnings as failures.
        #[arg(long)]
        strict: bool,
    },
    /// Run every kata in Docker: the reference must pass and the starter must fail.
    VerifyKatas {
        /// Only the katas of this track (e.g. `docker`).
        #[arg(long)]
        track: Option<String>,
    },
}

#[derive(Subcommand)]
enum RunnerAction {
    /// Build the images in runner-images/ (dojo-rust, ...).
    Build {
        #[arg(long, default_value = "runner-images")]
        images: PathBuf,
    },
}

fn main() -> Result<ExitCode> {
    let cli = Cli::parse();
    match cli.command {
        None => {
            let config = Config::load(&dojo_home().join("config.toml"))?;
            let options = tui::Options {
                content_dir: cli.content,
                db_path: cli.db.unwrap_or_else(|| dojo_home().join("dojo.db")),
                v1_profile: cli.import_v1,
                workspace: cli.workspace.unwrap_or(config.workspace),
                editor: config.editor,
                dev: cli.dev,
            };
            let Acquired::Lock(_lock) = single_instance::acquire(&dojo_home().join("dojo.lock"))?
            else {
                println!("Leaving the other Dojo open.");
                return Ok(ExitCode::SUCCESS);
            };
            tui::run(&options)?;
            Ok(ExitCode::SUCCESS)
        }
        Some(Command::Content {
            action: ContentAction::Validate { strict },
        }) => validate_command::run(&cli.content, strict),
        Some(Command::Content {
            action: ContentAction::VerifyKatas { track },
        }) => kata_commands::verify_katas(&cli.content, track.as_deref()),
        Some(Command::Runner {
            action: RunnerAction::Build { images },
        }) => kata_commands::build_runner_images(&images),
        Some(Command::Doctor { track }) => {
            let config = Config::load(&dojo_home().join("config.toml"))?;
            let environment = environment::Environment::new(
                editor::Editor::new(&config.editor),
                &std::path::absolute("runner-images")?,
            );
            doctor::run(&cli.content, &track, &environment)
        }
        Some(Command::Vscode) => {
            let config = Config::load(&dojo_home().join("config.toml"))?;
            let workspace = cli.workspace.unwrap_or(config.workspace);
            code_workspace::open_in_vscode(&cli.content, &workspace, &config.editor)
        }
    }
}
