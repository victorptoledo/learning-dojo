mod app;
mod profile_file;
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

    /// Progress file (temporary JSON store until the SQLite store lands).
    #[arg(long, global = true, default_value = "data/profile.json")]
    profile: PathBuf,

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
            tui::run(&cli.content, &cli.profile)?;
            Ok(ExitCode::SUCCESS)
        }
        Some(Command::Content {
            action: ContentAction::Validate { strict },
        }) => validate_command::run(&cli.content, strict),
    }
}
