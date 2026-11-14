use anyhow::Result;
use dojo_content::Severity;
use std::path::Path;
use std::process::ExitCode;

/// `dojo content validate`: prints every issue and fails on errors (or warnings with `--strict`).
pub fn run(content_dir: &Path, strict: bool) -> Result<ExitCode> {
    let loaded = dojo_content::load(content_dir)?;
    for issue in loaded.report.issues() {
        println!("{issue}");
    }

    let errors = loaded.report.count(Severity::Error);
    let warnings = loaded.report.count(Severity::Warning);
    let nodes: usize = loaded
        .catalog
        .tracks
        .iter()
        .map(|track| track.node_count())
        .sum();
    println!(
        "\n{} tracks, {nodes} nodes, {} skills, {} careers — {errors} errors, {warnings} warnings",
        loaded.catalog.tracks.len(),
        loaded.catalog.skills.len(),
        loaded.catalog.careers.len(),
    );

    let failed = errors > 0 || (strict && warnings > 0);
    Ok(if failed {
        ExitCode::FAILURE
    } else {
        ExitCode::SUCCESS
    })
}
