# 🥋 Learning Dojo

A gamified learning platform for the terminal (TUI), written in **Rust** with [`ratatui`](https://ratatui.rs/).

29 tracks inspired by the [roadmap.sh](https://roadmap.sh/) catalogue, each organised in five phases (First Steps → Fundamentals → Intermediate → Advanced → Mastery) with checkpoints, XP belts, a global daily streak and a **Rosetta bridge** that explains every concept from the programming language you already know.

> **Status: v2 milestone M0.** The content model, loader and validator are in place and the 29 tracks were migrated to it as English drafts (94 nodes). Katas, checkpoints, placement tests and AI review come in the next milestones — see [`docs/architecture.md`](docs/architecture.md).

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
| `Space` / `Enter` | Mark the node as done / pick a track |
| `B` | Change the bridge source language |
| `q` / `Esc` | Close the selector or quit (progress is saved) |

## Layout

```
crates/
  dojo-core/      pure domain: tracks, phases, nodes, skills, progress
  dojo-content/   loads content/ (YAML + Markdown) and validates it with JSON Schema
  dojo-app/       the `dojo` binary: TUI and subcommands
content/
  schemas/        JSON Schema for every content file type
  skills/         skill taxonomy, one file per namespace
  tracks/<id>/    track.yaml + p0-first-steps/ ... p4-mastery/<NN-slug>/
                  node.yaml, lesson.md, bridges/<language>.md
docs/
  architecture.md full v2 design; examples/ shows every planned file format
```

Progress is stored in `data/profile.json` for now (a SQLite store replaces it in M1). Profiles from v1 are migrated automatically.

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
