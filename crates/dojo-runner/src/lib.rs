//! Runner adapter: implements `dojo_core::KataRunner` with Docker.
//!
//! A check stages a throwaway copy of the solution plus the grading tests, runs the
//! kata image on it with no network and tight limits, and parses the test output.
//! Docker katas (`dojo-docker`) first build the learner's image on the host engine;
//! see `image_check`.

mod docker;
mod image_check;
mod libtest;
mod stages;
mod staging;
mod tar_list;

pub use docker::{DockerRunner, build_images};
pub use libtest::parse_cargo_test;
pub use stages::{check_parts, copy_missing, stage_sidequest};
pub use staging::{copy_dir, stage_reference};
