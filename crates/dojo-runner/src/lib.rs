//! Runner adapter: implements `dojo_core::KataRunner` with Docker.
//!
//! A check stages a throwaway copy of the solution plus the grading tests, runs the
//! kata image on it with no network and tight limits, and parses the test output.

mod docker;
mod libtest;
mod staging;

pub use docker::{DockerRunner, build_images};
pub use libtest::parse_cargo_test;
pub use staging::{copy_dir, stage_reference};
