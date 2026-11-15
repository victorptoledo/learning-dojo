# 🥋 Learning Dojo

A gamified learning platform for the terminal (TUI), written in **Rust** with [`ratatui`](https://ratatui.rs/).

30 tracks inspired by the [roadmap.sh](https://roadmap.sh/) catalogue, each organised in five phases (First Steps → Fundamentals → Intermediate → Advanced → Mastery) with checkpoints, XP belts, a global daily streak and a **Rosetta bridge** that explains every concept from the programming language you already know.

> **Status: v2 milestone M7.** Content model and validator (M0), SQLite progress (M1), katas checked in Docker (M2a), the complete Rust track (M2b: 22 nodes, 7 katas, Checkpoints 1 and 2), the complete Modern Java track (Java 25: 22 nodes, 7 katas, Checkpoints 1 and 2, placement bank), the complete Spring Boot track (Boot 4.1 on Java 25: 22 nodes, 7 katas tested with MockMvc, JPA on H2 and Spring Security), the complete Backend Developer track (22 nodes, 8 katas in Java and Spring: HTTP and ETags, a URL shortener, JWT from scratch, an issue tracker with optimistic concurrency, a rate limiter, idempotent payments, an outbox, a resilient client), the complete Linux track (Debian 13: 23 nodes, 8 bash katas checked with bats and ShellCheck in the `dojo-linux` image, Checkpoints 1 and 2, placement bank, lessons with further reading from man pages, the GNU manuals, the Wooledge BashGuide, the Arch Wiki and the systemd documentation), the complete Git & GitHub track (25 nodes from the object model to reflog rescues, 9 bash katas that drive `git` against repository fixtures, bare "servers" included, checked with bats in the `dojo-linux` image, Checkpoints 1 and 2, placement and interview banks, further reading from Pro Git, the Git reference, GitHub Docs and Atlassian), the placement test with Belt Exam (M3+M4: press `P` on a track you have not started) and mentoring without AI (M7: kata hints with `H`, spaced review with `R`, career paths and next steps with `G`). Every kata and every lesson exercise also has a review prompt for the AI chat of your choice: press `A` to save it and copy it to the clipboard (lesson answers go in `ANSWER.md`; for katas you attach your source files). It is feedback only and never changes progress. The other tracks are English drafts. Content review and more tracks come next; built-in AI review (M5) is postponed — see [`docs/architecture.md`](docs/architecture.md).

## Getting started (for friends trying the dojo)

The repository is private: accept the GitHub invitation first, then follow these steps once.

1. **Install the tools**
   - [Git](https://git-scm.com/downloads), and sign in to GitHub when the first clone asks (Git Credential Manager does it on Windows).
   - [Rust](https://rustup.rs/) (`rustup`, stable toolchain): the dojo is built from source.
   - [Docker Desktop](https://www.docker.com/products/docker-desktop/), running: every kata is checked inside a container.
   - Optional: [VS Code](https://code.visualstudio.com/). The dojo opens lessons and katas in it and recommends the extensions it needs.
   - Not needed: a JDK, Maven or bash on your machine. The Java, Linux and Git katas run in the dojo's own images.
2. **Get the code**

   ```bash
   git clone https://github.com/victorptoledo/learning-dojo.git
   cd learning-dojo
   ```

3. **Start it** from inside the `learning-dojo` folder (the dojo reads `content/` and `runner-images/` from there):
   - Windows: `.\dojo` (the first build takes a few minutes);
   - macOS and Linux: `cargo run --release --bin dojo`.
4. **First steps in the app**
   - Pick a track (`M`) and a lesson. When a step needs a tool or a Docker image, the dojo shows a Prepare screen: `[I]` installs what it can, including building the kata images. You can also build them all up front with `cargo run --release --bin dojo -- runner build`.
   - Your progress lives in `~/.dojo/dojo.db` and your kata solutions in a git repository of your own (`C:\dev\dojo-workspace` on Windows, `~/dojo-workspace` elsewhere; change it with `workspace = '...'` in `~/.dojo/config.toml`). Nothing is shared with anyone else.
5. **Updates**: `git pull`, then start the dojo again; it rebuilds itself.

Good to know:
- The complete tracks are Rust, Modern Java, Spring Boot, Backend Developer, Linux and Git & GitHub. The other tracks are drafts and show nodes "under construction".
- The kata reference solutions are in `content/` (`kata/reference`). Try not to peek; the hints (`H`) are there for that.
- The footer of each screen lists its keys; the key table below explains them.

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
| `B` | Pick the bridge source language from a list (Java, Python, JavaScript / TypeScript, C#, Go) |
| `/` | Study a topic: search every track for a subject (from work or an interview), open its lessons and katas, take a practice quiz on it (`Q`) or a mock interview (`I`) |
| `I` | Mock technical interview for the active track: pick a level, answer, reveal what a strong answer covers, rate yourself, see what to study. Write your answers in the answers file (`W` opens it) and `E` copies a prompt that asks an AI to evaluate them; `P` copies the interview for an AI interviewer instead |
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
