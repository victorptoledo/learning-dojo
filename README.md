# 🥋 Learning Dojo

A gamified learning platform for the terminal (TUI), written in **Rust** with [`ratatui`](https://ratatui.rs/).

29 tracks inspired by the [roadmap.sh](https://roadmap.sh/) catalogue, each organised in five phases (First Steps → Fundamentals → Intermediate → Advanced → Mastery) with checkpoints, XP belts, a global daily streak and a **Rosetta bridge** that explains every concept from the programming language you already know.

> **Status: v2 milestone M1.** Content model, loader and validator (M0) plus progress stored in SQLite (M1). The 29 tracks are English drafts (94 nodes). Katas, checkpoints, placement tests and AI review come in the next milestones — see [`docs/architecture.md`](docs/architecture.md).

## Run

```bash
cargo run --bin dojo
```

Validate the content folder (schemas, IDs, prerequisites, skills, completeness):

```bash
cargo run --bin dojo -- content validate
```

Add `--strict` to fail on warnings as well. Draft tracks only produce warnings for completeness gaps; reviewed tracks turn them into errors.

## Controls

| Key | Action |
| :--- | :--- |
| `M` or `T` | Open the track selector |
| `↑` / `k`, `↓` / `j` | Move through the tree (or the selector) |
| `Space` / `Enter` | Mark a lesson as done (`--dev`: tick or undo any node) / pick a track |
| `B` | Change the bridge source language |
| `q` / `Esc` | Close the selector or quit (progress is saved) |

## Layout

```
crates/
  dojo-core/      pure domain: tracks, phases, nodes, skills, progress
  dojo-content/   loads content/ (YAML + Markdown) and validates it with JSON Schema
  dojo-store/     SQLite progress store and the v1 profile import
  dojo-app/       the `dojo` binary: TUI and subcommands
content/
  schemas/        JSON Schema for every content file type
  skills/         skill taxonomy, one file per namespace
  tracks/<id>/    track.yaml + p0-first-steps/ ... p4-mastery/<NN-slug>/
                  node.yaml, lesson.md, bridges/<language>.md
docs/
  architecture.md full v2 design; examples/ shows every planned file format
```

Progress lives in `~/.dojo/dojo.db` (SQLite: settings plus an append-only event log; change it with `--db`). On first run a v1 `data/profile.json` is imported and renamed to `profile.v1.json`.

Lessons can be marked as done; katas, quizzes and checkpoints are completed by passing their checks once the runner lands. `--dev` lifts that rule and lets you undo completions:

```bash
cargo run --bin dojo -- --dev
```

## Tracks

| Category | Tracks |
| :--- | :--- |
| Careers & Roles | Backend, Frontend, Full Stack, DevOps & Cloud, AI Engineer, Data Engineer, Cyber Security, React |
| Programming Languages | Rust, Go, Modern Java & Spring Boot, Modern Python, TypeScript, Modern C++, Node.js |
| Architecture & Engineering | System Design, Software Architecture, API Design, Design Patterns, Prompt Engineering |
| Databases | PostgreSQL, SQL & Relational Databases |
| Infrastructure & Tools | Docker, Kubernetes, Linux, Git & GitHub, AWS |
| CS Fundamentals | Computer Science Fundamentals, Data Structures & Algorithms |

Rust and Docker are the pilot tracks that get fully authored first.

## Development

```bash
cargo test
```

```bash
cargo clippy --all-targets -- -D warnings
```
