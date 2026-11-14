# 🥋 Learning Dojo

A gamified learning platform for the terminal (TUI), written in **Rust** with [`ratatui`](https://ratatui.rs/).

30 tracks inspired by the [roadmap.sh](https://roadmap.sh/) catalogue, each organised in five phases (First Steps → Fundamentals → Intermediate → Advanced → Mastery) with checkpoints, XP belts, a global daily streak and a **Rosetta bridge** that explains every concept from the programming language you already know.

> **Status: v2 milestone M7.** Content model and validator (M0), SQLite progress (M1), katas checked in Docker (M2a), the complete Rust track (M2b: 22 nodes, 7 katas, Checkpoints 1 and 2), the complete Modern Java track (Java 25: 22 nodes, 7 katas, Checkpoints 1 and 2, placement bank), the complete Spring Boot track (Boot 4.1 on Java 25: 22 nodes, 7 katas tested with MockMvc, JPA on H2 and Spring Security), the placement test with Belt Exam (M3+M4: press `P` on a track you have not started) and mentoring without AI (M7: kata hints with `H`, spaced review with `R`, career paths and next steps with `G`). The other tracks are English drafts. The Backend track comes next; AI review (M5) is postponed — see [`docs/architecture.md`](docs/architecture.md).

## Run

On Windows, start it with the launcher:

```bash
.\dojo
```

`dojo.cmd` builds the app and runs a copy of the binary, so a Dojo left open in another window never blocks the next build (arguments are passed through: `.\dojo --dev`, `.\dojo content validate`). `cargo run --bin dojo` works too. Only one Dojo runs at a time: opening a second one offers to close the first, because both would watch the workspace and check and commit twice.

Validate the content folder (schemas, IDs, prerequisites, skills, completeness):

```bash
cargo run --bin dojo -- content validate
```

Add `--strict` to fail on warnings as well. Draft tracks only produce warnings for completeness gaps; reviewed tracks turn them into errors.

## Katas

Katas are checked in Docker, so Docker Desktop must be running. Build the runner image once (and again after changing `runner-images/`):

```bash
cargo run --bin dojo -- runner build
```

In the TUI, press `Enter` on a kata to create its folder in your workspace (default `C:\dev\dojo-workspace`) and open it in your editor, with the statement (`README.md`) and the code to write (`src/lib.rs` for Rust, the classes under `src/main/java` for Java); `C` checks it. Saving any file in the folder runs the checks too. When every test passes the kata is completed and its folder is committed to the workspace git repository (never pushed). The grading tests stay in the dojo and are copied into the container only.

Check that every kata's reference solution passes and its starter fails:

```bash
cargo run --bin dojo -- content verify-katas
```

Optional `~/.dojo/config.toml`:

```toml
workspace = 'C:\dev\dojo-workspace'
editor = 'C:\Users\you\AppData\Local\Programs\antigravity\Antigravity.exe'
# Any command that opens a folder or file. VS Code-like editors (code.cmd, Antigravity, Cursor)
# open the folder plus files in one window; IntelliJ (idea64.exe) opens it as a project.
# Default: VS Code if installed, else IntelliJ, else Explorer.
```

## Inside VS Code (recommended)

```bash
.\dojo vscode
```

This writes `dojo.code-workspace` in your workspace and opens it: one window with two roots,
**📖 Lessons** (the course, read-only, with answers, grading tests and manifests hidden) and
**🥋 Solutions** (your katas, a git repository). Markdown opens as a rendered preview,
rust-analyzer gets every kata, and the **Dojo** task starts the dojo in VS Code's terminal when
the workspace opens (VS Code asks once to allow automatic tasks). From that terminal, `Enter`
opens lessons and kata files in the same window. The file is regenerated on every start and
whenever a kata folder is created, and it is ignored by git.

**Run and debug.** With a local Rust toolchain, the visible tests run in VS Code: **▶ Run Test** above each `#[test]` (or `cargo test` in the kata folder). With CodeLLDB, **⚙ Debug** next to it stops on your breakpoints. Only the hidden grading tests, run in Docker, complete a kata. The workspace recommends the extensions it relies on: rust-analyzer, CodeLLDB, Even Better TOML, Error Lens, Docker and YAML.

IntelliJ works too (`editor` pointing at `idea64.exe`; from its terminal, files open in the same
window), but it has no equivalent of the two-root workspace.

## Preparing each step

Every track lists what all of its nodes need, and each node adds its own (`requirements` in
`track.yaml` and `node.yaml`): command-line tools with a minimum version, VS Code extensions, a
running Docker engine, runner images. Required items block the step; optional ones only advise.

When you open a node for the first time in a session, the dojo checks them. If anything is
missing, a **Prepare** screen lists each item with ✓/✗, the version found and why the step
needs it: `I` installs what it can (VS Code extensions, runner images, starting Docker Desktop;
tools such as Rust show how to install them), `U` updates (for example `rustup update stable`,
extension updates, rebuilding the image with a fresh base), `R` checks again, `Enter` continues
when nothing required is missing. To see the whole track at once:

```bash
.\dojo doctor rust
```

## Controls

| Key | Action |
| :--- | :--- |
| `M` or `T` | Open the track selector |
| `↑` / `k`, `↓` / `j` | Move through the tree (or the selector) |
| `Enter` | Open the lesson document, or the kata folder (created on first use) with its statement and `src/lib.rs`, in your editor / pick a track |
| `Q` | Take the quiz of the selected lesson |
| `1`–`9`, typing, `Enter`, `Esc` | Answer a quiz question, confirm, leave the quiz |
| `Space` | `--dev` only: tick or undo any node |
| `C` | Check the selected kata now |
| `O` | Open the kata folder in your editor again |
| `PgUp` / `PgDn` | Scroll the detail panel |
| `B` | Change the bridge source language |
| `Esc` | Close the selector or quit (progress is saved) |

## Layout

```
crates/
  dojo-core/      pure domain: tracks, phases, nodes, skills, progress
  dojo-content/   loads content/ (YAML + Markdown) and validates it with JSON Schema
  dojo-store/     SQLite progress store and the v1 profile import
  dojo-runner/    runs kata checks in Docker and parses the test output
  dojo-app/       the `dojo` binary: TUI and subcommands
content/
  schemas/        JSON Schema for every content file type
  skills/         skill taxonomy, one file per namespace
  tracks/<id>/    track.yaml + p0-first-steps/ ... p4-mastery/<NN-slug>/
                  node.yaml, lesson.md, quiz.yaml, bridges/<language>.md,
                  kata/ (README.md, starter/, tests/, reference/)
runner-images/   one Dockerfile per runtime (rust/, java/)
docs/
  architecture.md full v2 design; examples/ shows every planned file format
```

Progress lives in `~/.dojo/dojo.db` (SQLite: settings plus an append-only event log; change it with `--db`). On first run a v1 `data/profile.json` is imported and renamed to `profile.v1.json`.

Nothing is ticked by hand. A lesson is a document plus a quiz: pass the quiz (80% by default) to complete it. A kata is completed when its tests pass. A node without its document and quiz (or exercise) shows 🚧 and cannot be completed, and a phase opens only when every node of the earlier phases is done. The Rust track is fully written; the other tracks are still drafts. `--dev` lifts these rules and lets you undo completions:

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
