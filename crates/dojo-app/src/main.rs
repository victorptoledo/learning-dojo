mod app;
mod tree;
mod tui;
mod ui;
mod validate_command;

use anyhow::Result;
use clap::{Parser, Subcommand};
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
}

#[derive(Subcommand)]
enum ContentAction {
    /// Check every track, node, skill and bridge against the schemas and rules.
    Validate {
        /// Treat warnings as failures.
        #[arg(long)]
        strict: bool,
    },
}

fn main() -> Result<ExitCode> {
    let cli = Cli::parse();
    match cli.command {
        None => {
            let options = tui::Options {
                content_dir: cli.content,
                db_path: cli.db.unwrap_or_else(default_db_path),
                v1_profile: cli.import_v1,
                dev: cli.dev,
            };
            tui::run(&options)?;
            Ok(ExitCode::SUCCESS)
        }
        Some(Command::Content {
            action: ContentAction::Validate { strict },
        }) => validate_command::run(&cli.content, strict),
    }
}

fn default_db_path() -> PathBuf {
    std::env::home_dir()
        .unwrap_or_default()
        .join(".dojo")
        .join("dojo.db")
}
