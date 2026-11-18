# 🥋 Learning Dojo

**Learning Dojo** is a learning platform that runs in your terminal. It turns the
[roadmap.sh](https://roadmap.sh/) catalogue into structured tracks with written lessons, quizzes and
hands-on coding exercises (*katas*). Your code is checked by real tests inside Docker. You earn XP and
belts, keep a daily streak, and only move on when you have shown that you know the material.

It is written in Rust with [`ratatui`](https://ratatui.rs/) and works offline. No account, no
cloud, no built-in AI. When an AI review helps, the dojo writes a ready-made prompt for the AI chat
of your choice.

```text
┌ 🐧 Linux · Green Belt · 820 XP · 🔥 5 ─────────────────────────────────────────────┐
│ ▾ P1 Fundamentals        4/7 │ Permission Audit: Find and Fix Risky Modes          │
│   ✓ The Shell and the FHS    │ Report world-writable, setuid and setgid files...   │
│   ✓ Files, Links and Inodes  │ 👉 What to do                                       │
│   ✓ Streams and Pipes        │ 1. Press [O] to open the kata folder.               │
│   ◐ Permission Audit         │ 2. Edit the .sh scripts...                          │
│   🔒 Checkpoint 1            │ ✗ 9/12 tests passed                                 │
└──────────────────────────────┴─────────────────────────────────────────────────────┘
```

## What you get

- **Tracks in five phases.** Each track goes First Steps → Fundamentals → Intermediate → Advanced →
  Mastery. Phases open in order, and Fundamentals and Intermediate end with a **checkpoint**: a
  quiz plus an integration kata.
- **Lessons.** Each lesson has a document, a quiz you must pass and a written exercise, plus a
  "Further reading" list of reliable sources (official docs, man pages, well-known books and wikis).
- **Katas.** You write real code in your editor; saving a file runs the hidden tests in a sandboxed
  Docker container. When every test passes, the kata is completed and your solution is committed to
  your own git repository.
- **Rosetta bridges.** Every lesson explains its ideas from a language you already know: Java,
  Python, JavaScript/TypeScript, C# or Go.
- **Placement test.** If you already know part of a track, a short adaptive test lets you skip
  ahead. A Belt Exam then confirms the skip.
- **Mentoring without AI.** Kata hints that cost a little XP, practice katas on the same skills
  when you want more drill (Rust for now), spaced review of past quiz questions, and career paths
  that recommend your next step.
- **Sidequests.** Optional projects built from an almost empty folder, four per track (Rust,
  Java, Python, Go, TypeScript, C++, Spring Boot, SQL and Docker for now). Each unlocks on its own once you reach the checkpoints or phases it asks for,
  even of other tracks, and opens one stage at a time; contract tests check what your program
  does, not how you wrote it. Every stage gives sidequest XP and the last one a badge, never a
  belt.
- **Study a topic.** Search every track for a subject that came up at work, open its lessons and
  katas (even in phases you have not reached, as practice) and take a quiz on it.
- **Mock technical interviews.** 20+ interview questions per track, from junior to senior. You
  answer, reveal what a strong answer covers and rate yourself, then get a list of what to study.
  The dojo can also turn your written answers into a prompt for an AI to evaluate.
- **AI review prompts.** For any kata or lesson exercise, `A` copies a review prompt to the
  clipboard. It is feedback only and never changes your progress.

### Tracks

All 30 tracks are complete, with 721 lessons and checkpoints and 258 katas between them:

| Track | Teaches | Nodes | Katas | Katas run in |
| :--- | :--- | ---: | ---: | :--- |
| Rust | Rust 2024 edition | 22 | 7 | `dojo-rust` (cargo test) |
| Modern Java | Java 25 | 22 | 7 | `dojo-java` (JUnit 6) |
| Modern Python | Python 3.15 | 24 | 7 | `dojo-python` (pytest, ruff; FastAPI for a sidequest) |
| Go | Go 1.27 | 23 | 8 | `dojo-go` (go test -race, go vet, gofmt) |
| TypeScript | TypeScript 7 on Node.js 26 | 23 | 9 | `dojo-node` (tsc, node:test) |
| Node.js | Node.js 26 backends in TypeScript | 25 | 9 | `dojo-node` (tsc, node:test; real servers, workers and child processes) |
| Modern C++ | C++23 on GCC 16 | 24 | 9 | `dojo-cpp` (GoogleTest under the sanitizers, clang-tidy, clang-format) |
| Spring Boot | Boot 4.1 on Java 25 | 22 | 7 | `dojo-java` (MockMvc, JPA on H2, Spring Security) |
| React Developer | React 19 with TypeScript | 25 | 8 | `dojo-react` (tsc, Testing Library in jsdom) |
| Linux | Debian 13, bash 5.2 | 23 | 8 | `dojo-linux` (bats, ShellCheck) |
| Git & GitHub | Git from the object model to rescues | 25 | 9 | `dojo-linux` (bash scripts that drive `git`) |
| Docker & Containers | images, Compose, production | 25 | 9 | your Docker engine + `dojo-docker` (hadolint, bats) |
| Kubernetes | Kubernetes 1.36, Kustomize, Helm | 25 | 7 | `dojo-k8s` (kubeconform, bats; no cluster) |
| AWS Cloud Architecture | IAM, VPC, EC2, S3, CloudFormation, serverless, containers, security, cost | 25 | 9 | `dojo-aws` (cfn-lint, cfn-guard, IAM policy evaluator, moto; no AWS account) |
| SQL & Relational Databases | standard SQL on PostgreSQL 17 | 25 | 8 | `dojo-sql` (a throwaway PostgreSQL 17) |
| PostgreSQL | PostgreSQL 17 internals and operations | 25 | 9 | `dojo-sql` (other sessions opened with dblink for lock katas) |
| System Design | building blocks of large systems | 23 | 10 | `dojo-java` |
| Data Structures & Algorithms | structures, techniques, interview problems | 25 | 10 | `dojo-java` (with time limits on large inputs) |
| Design Patterns | principles and patterns in modern Java | 24 | 10 | `dojo-java` (refactoring katas) |
| API Design | HTTP, REST, errors, evolution, contracts | 25 | 8 | `dojo-java` (Spring Boot and MockMvc, plus an OpenAPI linter) |
| Software Architecture | modularity, hexagonal, DDD, events, fitness functions | 23 | 9 | `dojo-java` (fitness functions over compiled classes) |
| Computer Science | from bits and caches to compilers, computability and database internals | 25 | 9 | `dojo-python`, `dojo-rust` and `dojo-java` |
| Cyber Security | defensive application and infrastructure security | 25 | 10 | `dojo-python`, `dojo-java` (JCA) and `dojo-linux` (bash, awk); defensive and offline |
| Prompt Engineering | prompts for LLMs, from instructions to evals | 23 | 9 | `dojo-python` (prompt builders, parsers and eval harnesses; no model calls) |
| Backend Developer | HTTP, auth, data, reliability | 22 | 8 | `dojo-java` |
| Frontend Developer | HTML, CSS, the DOM and browser APIs, no framework | 25 | 9 | `dojo-web` (headless Chromium, axe-core, html-validate) |
| Full Stack Developer | browser to database: contracts, auth, caching, deploys, observability | 24 | 9 | `dojo-node`, `dojo-java`, `dojo-sql`, `dojo-docker` |
| DevOps & Cloud | CI/CD, Terraform, GitOps, releases, SLOs, DevSecOps | 25 | 8 | `dojo-devops` (actionlint, Terraform 1.17, promtool, bats), `dojo-linux`, `dojo-k8s` |
| AI Engineer | LLM features: APIs, RAG, tools, evals, guardrails | 24 | 10 | `dojo-python` (pytest against a fake, offline model) |
| Data Engineer | pipelines, modelling, lakes, streaming | 25 | 9 | `dojo-sql`, `dojo-python` and `dojo-data` (pyarrow, DuckDB) |

Every track has two checkpoints, a placement test, an interview bank and bridges from five
languages.

## Requirements

| Tool | Why |
| :--- | :--- |
| [Git](https://git-scm.com/downloads) | to get the code; your kata solutions are kept in a git repository |
| [Rust](https://rustup.rs/) (stable, via `rustup`) | the dojo is built from source |
| [Docker Desktop](https://www.docker.com/products/docker-desktop/) (running) | every kata is checked in a container |
| [VS Code](https://code.visualstudio.com/) (optional) | the dojo opens lessons and katas in it; any editor works |

You do **not** need a JDK, Maven, Python, Go, Node.js, a C++ compiler, PostgreSQL, kubectl or bash
on your machine: every kata runs in the dojo's own images, and Docker katas build your Dockerfile on
your own Docker engine. The dojo is developed on Windows 11. It also builds on macOS and Linux, but those platforms
You do **not** need a JDK, Maven, Python, Go, Node.js, a test browser, PostgreSQL, kubectl or bash on your machine:
every kata runs in the dojo's own images, and Docker katas build your Dockerfile on your own Docker
engine. The dojo is developed on Windows 11. It also builds on macOS and Linux, but those platforms
get less testing.

## Quick start

1. **Get the code.** The repository is private, so accept the GitHub invitation first.

   ```bash
   git clone https://github.com/victorptoledo/learning-dojo.git
   cd learning-dojo
   ```

2. **Start Docker Desktop.**
3. **Run the dojo** from inside the `learning-dojo` folder (it reads `content/` and
   `runner-images/` from there). The first build takes a few minutes.

   ```bash
   .\dojo
   ```

   On macOS and Linux:

   ```bash
   cargo run --release --bin dojo
   ```

4. **Pick a track** with `M`, move with `↑`/`↓`, and press `Enter` on the first lesson.

The first time a step needs something (a Docker image, a VS Code extension, a tool), the dojo
shows a **Prepare** screen with ✓/✗ for each item. Press `I` to install what it can, including
building the kata images (the Java image is the biggest, about 1 GB). To build every
image up front instead:

```bash
cargo run --release --bin dojo -- runner build
```

To update later, run `git pull` and start the dojo again; it rebuilds itself.

## How it works

1. **Read the lesson.** `Enter` opens the lesson document in your editor. The detail panel shows
   the bridge from your language, the exercise and the further reading.
2. **Pass the quiz.** Press `Q`. You need 80% to complete the lesson; a failed quiz can be retried
   after you reread the lesson.
3. **Solve the kata.** `Enter` on a kata creates its folder with the statement (`README.md`), the
   starter code and a visible test, then opens it. Write the code and save: the tests run by
   themselves and the result appears in the panel. `H` reveals a hint (each one costs 10% of the
   kata's XP), and `C` re-runs the check. When a kata lists practice katas, `F` opens the next one:
   a short kata on the same skills that gives practice XP and leaves the kata itself unchanged.
4. **Pass the checkpoint.** At the end of a phase, the quiz and the integration kata together
   unlock the next phase.
5. **Keep it fresh.** Questions from finished lessons come back after 1, 3, 7, 21 and 60 days:
   press `R` when the footer shows reviews are due.
6. **Take a sidequest.** `S` lists the four sidequests of the track. A locked one says what is
   missing ("Checkpoint 1 of Linux"); `Enter` on an unlocked one creates its project folder in
   `<workspace>/<track>/sidequests/` and opens it. Saving a file checks the open stage, and the
   tests of the stages you already passed keep running. A pass gives 30 sidequest XP, commits
   the folder and adds the next stage to its `README.md`.

Nothing is ticked by hand: a lesson is completed by its quiz and a kata by its tests.

### How katas are checked

Each kata runs in a container with no network, no capabilities and a time limit, using the image
of its language (see the table of tracks). A function or file you still have to write is marked
`todo` (`todo!()` in Rust, `raise NotImplementedError("todo ...")` in Python, `panic("todo ...")`
in Go, `throw std::logic_error("todo ...")` in C++, and so on), and the panel shows it as "not
written yet" instead of a failure.

**Docker katas** are different: you write a `Dockerfile` (and sometimes a `compose.yaml`). The dojo
builds it on your Docker engine with no network and a throwaway tag, runs it once with no network,
no capabilities and small limits, and the `dojo-docker` image checks what it saw with hadolint and
bats. The image and container are removed afterwards, and the Docker socket is never mounted.

**Kubernetes katas** need no cluster: you write manifests, Kustomize overlays or a Helm chart, and
the `dojo-k8s` image renders them (`kustomize build`, `helm template`), validates every object with
kubeconform against the Kubernetes 1.36 schemas and checks them with bats policies (selectors match
labels, Services reach a declared port, probes and resources, restricted Pod security, least-privilege
RBAC), all offline.

**C++ katas** are compiled by GCC 16 in C++23 mode with warnings on (`-Wall -Wextra -Wpedantic
-Wshadow`), and their GoogleTest tests run under AddressSanitizer and UndefinedBehaviorSanitizer
(ThreadSanitizer for the concurrency kata), with libstdc++ assertions on. A use after free, a leak,
a signed overflow, an index out of range or a data race fails the test that caused it, with the file
and line; a crash does not stop the other tests. Each of your files also gets a clang-format and a
clang-tidy result, and the compiler warnings in your files are one more test. Every kata is a CMake
project, so `cmake -B build && cmake --build build && ctest --test-dir build` runs the visible tests
on your machine if you have a compiler (optional).
**AWS katas** need no AWS account and no credentials, and never reach AWS: the `dojo-aws` image runs
with no network. CloudFormation and SAM templates are linted by cfn-lint (the AWS resource schemas)
and checked by pytest tests over the parsed template and by cfn-guard rules; IAM policies (identity,
trust, bucket, SCP, boundary) are sent a matrix of requests through the dojo's own policy evaluator,
which follows AWS's documented evaluation logic and is self-tested when the image is built; Python
code that calls AWS with boto3 runs against moto, an in-memory DynamoDB, SQS and S3. In one kata you
write the cfn-guard rules yourself and they are tested against compliant and broken templates.
**DevOps katas** need no cloud account either: you write GitHub Actions workflows, Terraform modules
or Prometheus rules, and the `dojo-devops` image checks them offline with actionlint (and
ShellCheck for every `run:` script), `terraform fmt`, `validate` and `terraform test` (with the
`random`, `null` and `local` providers baked in), `promtool check rules` and `promtool test rules`,
plus bats tests that read the files.

**SQL katas** run against a throwaway PostgreSQL 17. To try your queries on a kata's tables, run
this from the kata folder:

```bash
docker run -it --rm --network none -v "${PWD}:/work" dojo-sql play
```

**Frontend katas** are plain HTML, CSS and JavaScript. The `dojo-web` image serves the kata
folder inside the container, opens it in a headless Chromium and runs tests that click, type,
resize the window and measure where boxes land; axe-core checks accessibility and html-validate
checks the HTML. To look at a kata in your own browser, run this from its folder and open
<http://localhost:8080/>:

```bash
docker run --rm -p 8080:8080 -v "${PWD}:/work" dojo-web serve
```
**React katas** are TypeScript projects with `.tsx` components and `.ts` hooks. `tsc` type-checks
them with the React 19 types and compiles them, then the tests render your components into a jsdom
page with Testing Library and drive them with `user-event`, finding elements by role and label as a
user would. React, jsdom and Testing Library are baked into the `dojo-react` image, so checks run
offline.

**Data Structures & Algorithms katas** also time some hidden tests on large inputs, so a correct
but quadratic answer can still fail.

**Where your things live.** All of these stay on your machine and are never pushed:

| What | Where |
| :--- | :--- |
| Progress (XP, completions, reviews) | `~/.dojo/dojo.db` (SQLite) |
| Settings | `~/.dojo/config.toml` (optional) |
| Kata solutions, lesson answers, AI prompts | your workspace, a git repository: `C:\dev\dojo-workspace` on Windows, `~/dojo-workspace` elsewhere |

## Keys

The footer of each screen lists its keys. On the track screen:

| Key | Action |
| :--- | :--- |
| `M` or `T` | Choose a track |
| `↑`/`↓` or `k`/`j` | Move through the tree |
| `Enter` | Open the lesson document, or create and open the kata folder |
| `O` | Open the kata folder again |
| `Q` | Take the quiz of the lesson or checkpoint (practice only in phases you have not reached) |
| `C` | Check the kata now |
| `H` | Reveal the next kata hint |
| `F` | Open the next practice kata of a kata or checkpoint |
| `S` | Sidequests of the track (`Enter` starts or opens one, `C` checks its open stage) |
| `A` | Copy an AI review prompt for the kata or the lesson exercise |
| `R` | Spaced review of due questions |
| `P` | Placement test (on a track you have not started) |
| `/` | Study a topic across every track |
| `I` | Mock technical interview |
| `B` | Pick the language bridges are explained from |
| `G` | Career paths and next steps |
| `PgUp` / `PgDn` | Scroll the detail panel |
| `Esc` | Quit (progress is saved) |

**In a quiz:** `1`–`9` picks an option, typing answers a text question, `Enter` confirms, and `Esc`
leaves without recording anything.

**In a mock interview:** `Space` reveals what a strong answer covers, and `1`/`2`/`3` rates your
answer (covered, partly, missed). `W` opens the answers file and `E` copies a prompt that asks an
AI to evaluate your written answers. `P` copies the interview for an AI interviewer instead.

## Commands

```bash
.\dojo                          # the TUI (on macOS/Linux: cargo run --release --bin dojo -- <args>)
.\dojo vscode                   # write dojo.code-workspace (lessons + solutions) and open it in VS Code
.\dojo doctor rust              # check that this machine has everything the Rust track needs
.\dojo runner build             # build the kata images in runner-images/
.\dojo content validate         # check every track against the schemas and rules (--strict: warnings fail)
.\dojo content verify-katas     # every kata and sidequest stage: the reference must pass and the starter must fail (--track <id>, --only <text>)
.\dojo --dev                    # dev mode: tick or undo any node, ignoring the rules
```

On Windows, `dojo.cmd` builds the app and runs a copy of the binary, so a dojo left open never
blocks the next build. Only one dojo runs at a time: opening a second one offers to close the first.

## Configuration

`~/.dojo/config.toml` is optional:

```toml
workspace = 'C:\dev\dojo-workspace'   # where your kata solutions go
editor = 'code'                       # any command that opens a folder or a file
```

The default editor is VS Code if it is installed, then IntelliJ, then the file explorer. Editors
like VS Code, Cursor or Antigravity open the kata folder and its files in one window. IntelliJ
(`idea64.exe`) opens the folder as a project.

**VS Code workspace (recommended).** `.\dojo vscode` opens one window with two roots:
**📖 Lessons**, the course (read-only, with answers and grading tests hidden), and
**🥋 Solutions**, your katas. Markdown opens as a preview, rust-analyzer knows every Rust kata, the
recommended extensions are suggested, and a **Dojo** task starts the dojo in VS Code's terminal.

## Troubleshooting

| Problem | Fix |
| :--- | :--- |
| Checks fail because Docker is not reachable | Start Docker Desktop and wait until it says it is running, then press `C`. |
| A `dojo-…` image is missing | Press `I` on the Prepare screen, or run `.\dojo runner build`. |
| A bash kata fails with `$'\r': command not found` | The script was saved with Windows line ends. The dojo copes, but real servers will not: save `.sh` files with LF (the VS Code workspace does this for you). |
| `cargo` is not found | Install Rust with `rustup` and open a new terminal. |
| The first check of a Docker kata is slow | It pulls the kata's base images once (for example `golang`, `alpine`, `distroless`); later checks reuse them. |
| The first start is slow | The first build compiles the whole app and the first check of each language builds its image. Later starts are fast. |
| The dojo says another one is running | Close the other window, or let the dojo close it for you. |

## Project layout

```text
crates/
  dojo-core/      domain: tracks, phases, nodes, skills, progress, reviews, careers, interviews
  dojo-content/   loads content/ (YAML + Markdown) and validates it with JSON Schema
  dojo-store/     SQLite progress store (settings plus an append-only event log)
  dojo-runner/    runs kata checks in Docker and parses the test output
  dojo-app/       the dojo binary: TUI, screens and subcommands
content/
  schemas/        JSON Schema for every content file type
  skills/         the skill taxonomy, one file per namespace
  careers/        career paths built from track phases and skills
  tracks/<id>/    track.yaml, placement.yaml, interview.yaml and p0-first-steps/ ... p4-mastery/<NN-slug>/
                  with node.yaml, lesson.md, quiz.yaml, bridges/<language>.md
                  and kata/ (README.md, starter/, tests/, reference/)
runner-images/    one image per kata runtime: rust/, java/ (JUnit 6, Spring Boot), linux/ (bats, ShellCheck, git),
                  docker/ (hadolint, bats), sql/ (PostgreSQL 17), python/ (pytest, ruff),
                  go/ (go test -race, go vet, gofmt), node/ (TypeScript 7, node:test),
                  k8s/ (kubeconform, kustomize, helm, bats),
                  aws/ (cfn-lint, cfn-guard, IAM policy evaluator, boto3 + moto)
                  react/ (React 19, jsdom, Testing Library),
                  k8s/ (kubeconform, kustomize, helm, bats)
docs/
  architecture.md the full design and its decisions; examples/ shows every file format
```

## Development

```bash
cargo test
cargo clippy --all-targets -- -D warnings
cargo run --bin dojo -- content validate
cargo run --bin dojo -- content verify-katas    # needs Docker and the images; --track <id> for one track
```

To write content, copy the structure of a complete track (pick one whose katas use the same
image as yours), then run `content validate` until it shows no errors or warnings for your track and
`content verify-katas` until every kata's reference passes and its starter fails. The design,
the content rules and the roadmap are in [`docs/architecture.md`](docs/architecture.md).

Content and code are in English. The kata reference solutions live in `content/` (`kata/reference`):
if you are here to learn, try the hints before you peek.
