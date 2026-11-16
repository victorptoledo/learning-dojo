//! `dojo runner build`, `dojo content verify-katas`: Docker-backed commands outside the TUI.

use anyhow::{Result, anyhow};
use dojo_core::{Kata, KataRunner, Track};
use dojo_runner::{DockerRunner, build_images, stage_reference};
use std::path::Path;
use std::process::ExitCode;

/// Builds every image in `runner-images/`.
pub fn build_runner_images(images_dir: &Path) -> Result<ExitCode> {
    let built = build_images(images_dir).map_err(|error| anyhow!(error))?;
    println!("\nBuilt: {}", built.join(", "));
    Ok(ExitCode::SUCCESS)
}

/// Every kata, and every practice kata attached to a node, must fail with its starter
/// and pass with its reference solution. `only_track` limits the run to one track.
pub fn verify_katas(content_dir: &Path, only_track: Option<&str>) -> Result<ExitCode> {
    let catalog = dojo_content::load(content_dir)?.catalog;
    let runner = DockerRunner::default();
    let mut problems = 0;
    let tracks = catalog
        .tracks
        .iter()
        .filter(|track| only_track.is_none_or(|id| track.id == id));
    let katas = tracks.flat_map(|track| {
        track.nodes().flat_map(move |node| {
            let main = node.kata.iter().map(|kata| (node.id.as_str(), kata));
            let practice = node
                .remedial
                .iter()
                .map(|remedial| (remedial.id.as_str(), &remedial.kata));
            main.chain(practice)
                .map(move |(id, kata)| (track, id, kata))
        })
    });
    for (track, id, kata) in katas {
        problems += verify_kata(&runner, track, id, kata)?;
    }
    println!("\n{problems} problems");
    Ok(if problems == 0 {
        ExitCode::SUCCESS
    } else {
        ExitCode::FAILURE
    })
}

fn verify_kata(runner: &DockerRunner, track: &Track, id: &str, kata: &Kata) -> Result<usize> {
    let reference = stage_reference(kata)?;
    let with_reference = runner
        .check(kata, reference.path())
        .map_err(|error| anyhow!(error))?;
    let with_starter = runner
        .check(kata, &kata.starter_dir)
        .map_err(|error| anyhow!(error))?;

    let mut problems = 0;
    let reference_ok = with_reference.is_pass();
    let starter_ok = !with_starter.is_pass();
    println!(
        "{} {}/{}: reference {} · starter {}",
        if reference_ok && starter_ok {
            "✓"
        } else {
            "✗"
        },
        track.id,
        id,
        with_reference.summary(),
        with_starter.summary(),
    );
    if !reference_ok {
        problems += 1;
        println!(
            "  the reference solution must pass:\n{}",
            indent(&with_reference.details)
        );
    }
    if !starter_ok {
        problems += 1;
        println!("  the starter must not pass on its own");
    }
    Ok(problems)
}

fn indent(text: &str) -> String {
    text.lines()
        .map(|line| format!("    {line}"))
        .collect::<Vec<_>>()
        .join("\n")
}
