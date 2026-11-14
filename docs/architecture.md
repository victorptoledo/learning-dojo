# Learning Dojo v2 Architecture

> Status: **M0, M1 and M2a implemented** (2026-11-14): workspace, `dojo-core`, `dojo-content`, JSON Schemas, `dojo content validate`, the 29 tracks migrated as English drafts, the SQLite progress store (`dojo-store`), and kata checks in Docker (`dojo-runner`) with three Rust katas. Later milestones are still design.
> Full example content files live in [`docs/examples/`](examples/).

## 0. Decisions so far

| Topic | Decision |
|---|---|
| Audience | Personal use for now; may become published content later. No accounts, no server. |
| Interface | Terminal only (ratatui). The hexagonal core keeps a web UI possible later, but it is not planned. |
| Kata execution | Inside Docker containers from day one. |
| Second pilot track | Docker (`platform` archetype), alongside Rust (`language`). |
| Rosetta bridge | The learner picks their source language; bridges are keyed by language. |
| Placement XP | A node skipped by placement and confirmed by the belt exam grants full XP. |
| Language | All content, code, identifiers and repo docs in English. |
| AI reviewer backend | Free local model through Ollama; no paid AI. See section 8.4. |

## 1. Current state

| Aspect | Today | Problem for v2 |
|---|---|---|
| Content | 29 `track.json` files, 1–10 nodes each (~3.5 on average) | Needs 15–25 nodes; JSON is poor for long text (lessons, kata statements) |
| Model | `Node { section, prerequisites, xp, rosetta }` | No phase, checkpoint, node kind, learning objective or skill |
| Completion | `toggle_complete`: the learner ticks the kata | No evidence: nothing is tested or reviewed |
| Rosetta | `rust_way` field used by every track; always "coming from Java" | Name tied to Rust; single source language |
| Code | One crate; `AppState` mixes UI, IO and rules | Placement, runner and AI need a core testable without a terminal |
| Progress | `profile.json` with a `HashSet` of completed nodes | No attempts, scores, review history or per-skill mastery |

Kept: ratatui, folder-based loading, XP belts, global streak, the Rosetta bridge (generalised).

## 2. Principles

1. **Content is data, the engine is code.** Tracks, katas, quizzes and rubrics live in `content/` (YAML + Markdown) and are validated by JSON Schema. Adding a track needs no recompilation.
2. **Evidence before XP.** A node is completed only by passing tests, a quiz score or an approved review. The manual toggle survives only behind `--dev`.
3. **Deterministic first, AI last.** Tests and linters decide what they can; the AI judges what only a human reviewer could (readability, design, architecture) and can never contradict a measured fact.
4. **Skills are the common currency.** Every objective, question and rubric criterion points to skills in a shared taxonomy. Placement, remediation and career recommendations use the same scale.
5. **Hexagonal core.** Pure domain in `dojo-core` with ports (traits) for content, storage, execution and review. The TUI is one adapter.

## 3. Repository layout

```
learning-dojo/
├── Cargo.toml                      workspace
├── crates/
│   ├── dojo-core/                  pure domain, no IO
│   │   └── src/{track,progress,placement,mastery,scoring,recommend,ports}.rs
│   ├── dojo-content/               loads content/, validates schemas, builds the index
│   ├── dojo-store/                 SQLite (rusqlite) + profile.json migration
│   ├── dojo-runner/                runs kata checks in Docker
│   ├── dojo-review/                review pipeline: linters, structure rules, AI
│   └── dojo-app/                   `dojo` binary: TUI (ratatui) + subcommands (clap)
├── runner-images/                  Dockerfiles for each runtime (rust, docker-lint, ...)
├── content/
│   ├── schemas/                    JSON Schema per file type
│   ├── skills/                     taxonomy: core.yaml, rust.yaml, docker.yaml, ...
│   ├── careers/                    career paths
│   └── tracks/
│       └── rust/
│           ├── track.yaml          metadata, belts, phases
│           ├── placement.yaml      placement rules
│           ├── bank/               question bank (one file per phase)
│           ├── p0-first-steps/
│           │   ├── 01-install/
│           │   │   ├── node.yaml
│           │   │   └── lesson.md
│           │   └── 02-hello-world/ ...
│           ├── p1-fundamentals/
│           │   ├── 04-loops/
│           │   │   ├── node.yaml
│           │   │   ├── lesson.md
│           │   │   ├── bridges/            java.md, python.md, typescript.md ...
│           │   │   └── kata/
│           │   │       ├── README.md       statement
│           │   │       ├── starter/        what the learner gets
│           │   │       ├── tests/          visible and hidden tests
│           │   │       ├── reference/      reference solution (never copied out)
│           │   │       ├── samples/        weak solutions with expected scores (AI evals)
│           │   │       └── rubric.yaml
│           │   └── 99-checkpoint/          📋 Checkpoint 1 closes phase 1
│           ├── p2-intermediate/ ... 99-checkpoint/   📋 Checkpoint 2
│           ├── p3-advanced/
│           └── p4-mastery/
└── docs/
```

Naming: phase folders `p<N>-<slug>`, node folders `<order>-<slug>`, the checkpoint is always `99-checkpoint` inside the phase it closes. Node IDs are `<track>.p<N>.<slug>` (e.g. `rust.p1.loops`) and stay stable if the order changes.

On the learner's machine:

```
~/.dojo/
├── dojo.db                         progress, attempts, reviews (SQLite)
└── config.toml                     source language, AI backend, workspace path (never keys)

C:\dev\dojo-workspace\              the learner's solutions, a git repository of its own
├── README.md
├── .gitignore                      target/, node_modules/, editor folders
└── rust/p1-fundamentals/04-loops/  copy of the starter, where the learner codes
```

The workspace lives outside the dojo repository (default `C:\dev\dojo-workspace`, changed in `config.toml` or with `--workspace`) and is its own git repository, so the solutions keep their history. The learner codes in their own editor; the TUI watches the folder (`notify`) and re-runs the checks on save; `dojo submit` sends it for review. The dojo never overwrites a kata folder that already exists, and never pushes the workspace anywhere.

**Auto-commit on pass:** when a kata's checks pass and its folder changed since the last commit, the dojo stages only that folder and commits it as `<track>/<phase>/<kata>: passed (6/6 tests)` with the learner's own git identity. Nothing outside the kata folder is touched. If the workspace is not a git repository or git is missing, the TUI shows a warning and carries on.

## 4. Track archetypes

"Primitive types and loops" does not fit AWS or System Design. Each track declares an `archetype`; all share phases 0–4 and the two checkpoints, but what a phase contains and how a kata is checked differ.

| Archetype | Tracks | Phase 1 (Fundamentals) is... | Kata checked by |
|---|---|---|---|
| `language` | rust, go, java, python, typescript, cpp, nodejs, sql, dsa | types, control flow, functions, collections | language test runner inside its image (`cargo test`, `pytest`, `vitest`, `go test` ...) |
| `platform` | docker, kubernetes, linux, git, aws, postgresql | core commands and objects, basic configuration | verification script (Dockerfile lint + build + asserts, `kubeconform`, queries against a Postgres container, git repo state) |
| `concept` | system-design, software-architect, design-patterns, api-design, computer-science, prompt-engineering | vocabulary, mental models, trade-offs | quiz + a document (ADR, diagram, API contract) graded by rubric; code where it makes sense |
| `role` | backend, frontend, full-stack, devops, ai-engineer, data-engineer, cyber-security, react | the minimum of each pillar | composes nodes from other tracks by skill + its own integration projects |

`role` tracks do not duplicate content: `backend` references `sql.p1.*`, `api-design.p1.*` and so on, then adds projects that tie them together.

## 5. Data model

### 5.1 Entities

```
Track 1─* Phase 1─* Node ── kind: lesson | kata | quiz | checkpoint | project
                    Node *─* Skill (through objectives)
                    Node 1─* Bridge (one per source language)
Skill (global taxonomy: core.*, rust.*, docker.*)
QuestionItem *─* Skill        (question bank: placement and checkpoints)
Rubric 1─* Criterion          (per kata; criteria carry from_phase)
Career 1─* Stage ── requires: [(track, phase) | skill >= level]
```

Example files: [`track.yaml`](examples/track.yaml), [`node-kata.yaml`](examples/node-kata.yaml), [`node-checkpoint.yaml`](examples/node-checkpoint.yaml), [`quiz.yaml`](examples/quiz.yaml), [`placement.yaml`](examples/placement.yaml), [`rubric.yaml`](examples/rubric.yaml), [`skills.yaml`](examples/skills.yaml), [`career.yaml`](examples/career.yaml), [`docker-kata.yaml`](examples/docker-kata.yaml), [`bridge-java.md`](examples/bridge-java.md), [`review-output.schema.json`](examples/review-output.schema.json).

### 5.2 Rosetta bridge by source language

The learner picks a source language at onboarding (`config.toml: bridge_from = "java"`, changeable any time). Each node keeps one bridge per source language in `bridges/<lang>.md` (front matter: `concept`, `from_way`, `native_way`, `sensei_tip`). Resolution order: the chosen language → `generic.md` (concept explained with no comparison) → no bridge tab. Missing bridges can be drafted by AI and marked `status: draft` until reviewed. The existing `rosetta` blocks migrate to `bridges/java.md`.

### 5.2b Completion rules (implemented 2026-11-14)

- A lesson is completed only by passing its quiz (`quiz.yaml` next to `node.yaml`: single-choice or typed answers, typed ones compared ignoring case and spaces; `pass_percent`, default 80, rounded up). Attempts are stored (`quiz_attempt`).
- A node is *under construction* when it lacks what a learner needs: a lesson without `lesson.md` or quiz, a kata without its exercise, a checkpoint without its phase quiz and integration kata; standalone quiz and project nodes until their engines exist. A checkpoint opens only when every other node of its phase is done and is completed when both its quiz and its kata have passed, in any order. It shows 🚧, cannot be completed, and so blocks what comes after it.
- A node unlocks when its prerequisites are done **and** every node of the earlier phases is done.
- `--dev` can tick and undo anything (locks still apply).

### 5.2c Per-step setup (implemented 2026-11-14)

- `requirements` on a track apply to every node; a node adds its own, and an entry with the same key replaces the track's. Kinds: `tool` (command, version args, `min_version`, install hint, update command), `vscode-extension`, `docker-engine`, `docker-image`. `required` defaults to true.
- Opening a node runs the checks on a background thread (once per session per node). All ready: the node opens by itself. Something missing: the Prepare screen. Required gaps block `Enter`.
- The dojo installs only what is safe to automate on request (`I`): VS Code extensions, runner images, starting Docker Desktop. Tools are installed by the learner from the hint. `U` runs update commands.
- `dojo doctor <track>` prints the same report for a whole track.

### 5.3 Node state for a learner

```
Locked ──prerequisites met──▶ Available ──opened──▶ InProgress ──passed──▶ Passed ──high score──▶ Mastered
   │                                                                  ▲
   └──placement──▶ PlacedOut(provisional) ──belt exam passed──▶ PlacedOut(confirmed)
                              └──belt exam failed──▶ back to Available, only for nodes of the weak skills
```

A prerequisite is met by `Passed | Mastered | PlacedOut`. XP: `Passed` grants the node XP, `Mastered` adds a 50% bonus, `PlacedOut(confirmed)` grants the full node XP.

### 5.4 Storage (SQLite)

| Table | Main columns |
|---|---|
| `node_progress` | `node_id`, `status`, `best_score`, `attempts`, `first_passed_at`, `updated_at` |
| `skill_mastery` | `skill_id`, `alpha`, `beta`, `updated_at` (mastery = α/(α+β)) |
| `submission` | `id`, `node_id`, `code_hash`, `tests_json`, `static_json`, `review_json`, `score`, `reviewer`, `rubric_version`, `created_at` |
| `placement_session` | `track_id`, `started_at`, `answers_json`, `result_json` |
| `event` | append-only log (xp, streak, belt); the profile is derived from it |

Implemented in M1: `settings` (key/value) and `event` (`node_completed`, `node_reset`), with schema versions tracked by `PRAGMA user_version`. The profile is rebuilt by replaying events (`Profile::replay`). `node_progress`, `skill_mastery`, `submission` and `placement_session` are added by migrations in the milestones that need them (M2, M4, M5). Completion rule: lessons can be marked as read; other kinds need evidence, except in `--dev`.

Migration: on first run each node in `profile.json`'s `track_progress` becomes `Passed` (mapped via `legacy_id` in `node.yaml`), and the file is renamed `profile.v1.json`.

## 6. Pedagogical progression

| Phase | Goal | Typical nodes | Dominant kind |
|---|---|---|---|
| P0 First Steps | install, run, understand why it exists | 2–3 | lesson + trivial kata |
| P1 Fundamentals | syntax, types, flow, functions, collections | 5–6 | short kata, tests |
| 📋 Checkpoint 1 | lock in fundamentals | 1 | quiz (10 items) + integration kata |
| P2 Intermediate | abstractions, errors, data, good practices, testing | 4–6 | medium kata with clean-code rubric |
| 📋 Checkpoint 2 | integration | 1 | quiz + 2–3 file mini project |
| P3 Advanced | performance, concurrency, patterns, architecture | 3–5 | kata with a required folder structure |
| P4 Mastery | production, resilience, specialised topics | 2–4 | project with architecture review |

18–26 nodes per track. The rubric scales with the phase: in P1 the reviewer looks at naming, small functions and flow; design patterns become a criterion only from P3 (`from_phase: 3`).

## 7. Placement test

**Milestone-based per phase, adaptive within a phase, confirmed by the checkpoint.** Not full IRT/CAT, because there is no calibration data; item difficulty can be recalibrated later without changing the flow.

1. **Entry question** (self-assessment): "never used it" → start at P0, no test; "studied a bit" → test from P1; "use it at work" → test from P2.
2. **Per phase**, draw `items_per_phase` questions stratified by the phase's key skills, at medium/high difficulty for the phase. Preferred types: predict the output, fill in code, short kata. Every question has an "I don't know" option (no penalty, reduces guessing).
3. **Per-skill mastery** (Beta counts): correct `α += w`, wrong or "don't know" `β += w`, `w` from difficulty. Skills already mastered in another track (`core.*`) seed the prior.
4. **Early stop:** 2 wrong in the first 3 items of a phase ends the test there.
5. **Phase rule:**
   - score ≥ `pass` (0.8): phase likely mastered → test the next one;
   - between `partial` (0.5) and `pass`: partial → mark `PlacedOut` only for nodes whose objectives use skills with mastery ≥ 0.75, then stop;
   - below `partial`: stop; this phase is the starting point.
6. **Plan:** skipped nodes show ⏭ in the tree. The checkpoint of the last skipped phase becomes a mandatory **Belt Exam** (a practical kata, not only a quiz). Pass: skips confirmed and full XP granted. Fail: only the nodes of the failed skills go back to `Available`.

Caps: `max_items` 25, about 20 minutes. The result screen shows per phase: mastered / partial / to study, plus the estimated time saved.

```rust
pub enum PhaseVerdict { Mastered, Partial(Vec<SkillId>), NotYet }

pub struct PlacementEngine<'a> { track: &'a Track, cfg: &'a PlacementConfig, mastery: SkillMastery }

impl PlacementEngine<'_> {
    pub fn next_item(&self, phase: PhaseId, asked: &[ItemId]) -> Option<&QuestionItem>;
    pub fn record(&mut self, item: &QuestionItem, answer: Answer);
    pub fn verdict(&self, phase: PhaseId) -> PhaseVerdict;
    pub fn plan(&self) -> PlacementPlan; // PlacedOut nodes + required belt exam
}
```

Pure and deterministic (seeded draw), testable without a terminal.

**Implemented in M3+M4** (`dojo-core::placement`, `content/tracks/<track>/placement.yaml`, schema `placement.schema.json`):
- Offered with `P` only on a track the learner has not started; `Esc` records nothing, `Enter` applies the plan.
- Events `node_placed_out`, `belt_exam_assigned`, `placement_revoked` (store schema v4). A placed-out node counts as done for unlocking but gives no XP.
- Belt Exam: the checkpoint of the last fully skipped phase, or else the first checkpoint after the skipped nodes. It is never placed out, so later phases stay locked until it passes. Passing it completes every placed-out node with full XP.
- A failed Belt Exam quiz revokes only the placed-out nodes that teach a skill of a missed question (checkpoint questions declare `skills`). A failing kata revokes nothing.
- Partial phase: a node is skipped when every skill the test measured is mastered (≥ threshold); unmeasured skills are ignored.
- Not yet: short katas inside the test, cross-track priors, the 20-minute cap.

## 8. Layered code review

```
dojo submit
  │
  ├─[1] Runner      tests in a Docker container (no network, CPU/memory/time limits)   fail → test feedback + level-1 hint
  ├─[2] Static      linters, formatter, metrics in the same image                     normalised findings {tool, rule, file, line, severity}
  ├─[3] Structure   folder and dependency rules from the rubric                       e.g. domain/ must not import infra/
  ├─[4] AI review   phase-filtered rubric + findings 1–3 + code + weak skills → validated JSON
  ├─[5] Score       weighted criteria with deterministic caps
  └─[6] Mentor      failed or 2nd attempt → authored remedial kata; none → AI-generated challenge
```

### 8.1 Docker runner

- One image per runtime under `runner-images/` (`dojo-rust`, `dojo-node`, `dojo-python`, `dojo-docker-lint` ...), built locally by `dojo runner build` and tagged with the content version. Images carry the test runner and the linters, so nothing besides Docker has to be installed on the host.
- Each check runs `docker run --rm --network none --cpus 2 --memory 1g --pids-limit 256` with the workspace mounted read-only and a throwaway volume for build output (e.g. Cargo `target/`, cached per kata so re-runs are fast). Hidden tests are copied in at run time, never into the learner's workspace.
- The container writes a JSON result to stdout (test cases, linter findings, metrics); the runner parses it into `CheckReport`.
- **Docker track katas** need a Docker engine to build images. They use a separate, explicit profile: the runner builds the learner's image on the host engine with a kata-specific tag, runs `hadolint` and the kata's asserts (image size, user not root, layers, healthcheck, exposed ports) in a container, and removes everything afterwards. No privileged containers, no mounting the Docker socket into learner code.
- If Docker is not running, the TUI says so and offers quizzes/lessons only.
- **Implemented in M2a:** the check runs on a staged copy (solution + grading tests) whose files all get a fresh modification time. Copies keep the original times and the build cache volume is shared between checks of a kata, so without this an older file could look up to date and reuse a stale binary (found by `verify-katas`: starters were "passing" with the reference's build). Limits: `--network none --cpus 2 --memory 1g --pids-limit 256 --cap-drop ALL --security-opt=no-new-privileges`, user `dojo` (uid 10001), kill on `timeout_secs`. The output of stable `cargo test` is parsed (no JSON test format on stable).
- Known gap: when a solution breaks the build of a grading test, the compiler message names the hidden test file and shows the offending line. Starters therefore ship compilable stubs (`todo!()`) for everything the grading tests use.

### 8.2 Static tools per language (run inside the image)

| Language | Lint / format | Metrics |
|---|---|---|
| Rust | `clippy -D warnings`, `rustfmt --check` | function length, nesting (via `syn`) |
| TypeScript/Node/React | `tsc --noEmit`, `eslint` | cyclomatic complexity (eslint rule) |
| Python | `ruff`, `mypy` | `ruff` C901 |
| Go | `go vet`, `golangci-lint` | `gocyclo` |
| Java | `javac -Xlint`, Checkstyle/PMD | PMD |
| C++ | `clang-tidy`, `clang-format` | `lizard` |
| SQL / Docker / K8s | `sqlfluff`, `hadolint`, `kubeconform` | — |

### 8.3 Scoring

- Tests are a **gate**: without 100% of the required tests there is no pass (the review can still run to teach).
- Final score = weighted sum of rubric criteria (level 0–3 each, weights from `rubric.yaml`).
- **Deterministic caps:** the rubric links findings to criteria. Any clippy warning caps `idioms` at 1; a layer-dependency violation caps `structure` at 0. The AI cannot score above a cap.
- Pass and mastery thresholds come from `node.yaml` (e.g. `pass: 70`, `mastered: 85`).

### 8.4 AI reviewer

Port: `trait Reviewer { fn review(&self, req: ReviewRequest) -> Result<ReviewReport>; }`. Every adapter receives the same prompt and must return JSON matching [`review-output.schema.json`](examples/review-output.schema.json).

**Chosen backend: a local model through Ollama** (free, offline, code never leaves the machine).

| Adapter | How it works | Status |
|---|---|---|
| `LocalReviewer` | HTTP to Ollama at `http://localhost:11434/api/chat`, with the review JSON Schema passed in `format` (structured output) and `temperature: 0` | **default**, built in M5 |
| `OfflineReviewer` | no AI: layers 1–3 only; criteria without measurable signals are left unscored | fallback when Ollama is not running |
| `ClaudeCodeReviewer` / `ApiReviewer` | Claude via the `claude` CLI or the Anthropic API | not planned; the port keeps them possible if the dojo is shared |

Local model setup:
- Target hardware: RTX 4060 Ti 8 GB, 31 GB RAM. A 7–8B code model at 4-bit quantisation fits fully in VRAM; 14B models spill into RAM and get much slower. The model name lives in `config.toml` (`[review] model = "..."`), so it can change without code changes.
- Ollama runs either natively or as a container (`ollama/ollama` with GPU access), consistent with the Docker runner. The reviewer only needs the HTTP endpoint.
- The GPU is shared with other workloads (video renders). If Ollama is busy or slow, the review waits with a timeout and falls back to `OfflineReviewer` without blocking the test result.
- Model choice is made by evals, not by taste: candidates run against the kata `reference/` and `samples/`, and the best agreement within an acceptable time per review wins.

What a small local model is trusted with, and what it is not:
- **Trusted:** naming, function size and cohesion, readability, error-handling style, explaining test failures, hints, short practice challenges.
- **Weighted down:** design patterns and architecture criteria (P3/P4). Their score leans on the deterministic structure rules (required paths, forbidden imports); the model's level for those criteria is shown as advice and capped at 2 unless a rule confirms it.
- Small models hallucinate more, so the evidence check (below) matters even more: any issue without a matching `file:line` quote is dropped.

Common rules:
- **Prompt order (stable first):** role and fixed rules → kata rubric → statement → deterministic findings → learner code → short history (weak skills, previous attempts). Keep it short; small models lose track of long prompts, so only the phase-relevant rubric criteria are sent.
- **Reviewer rules:** never hand over the solution; every critique points to `file:line` and quotes the code; respect the phase (do not demand what the rubric does not yet require); separate strengths from improvements; feedback in English.
- **Anti-hallucination:** the app checks every piece of evidence. A missing line or a quote that does not match the file → the issue is dropped and logged.
- **Cache:** key `(code hash, rubric version, reviewer + model)`. Re-submitting identical code costs nothing.
- **Privacy:** with the local reviewer no code leaves the machine. If a remote adapter is ever added, it needs explicit consent and keys from environment variables only, never written to `dojo.db` or `config.toml`.
- **Evals:** each kata has `reference/` (expected high) and `samples/` (weak solutions with expected levels per criterion). `dojo review eval rust` measures agreement before any prompt, model or adapter change is accepted.

### 8.5 Mentoring

- **Hint ladder**, three levels on request: (1) a question pointing the way, (2) the concept and where it shows up in the code, (3) a partial skeleton without the solution.
- **Remediation:** when struggle is detected (a failed attempt, 2+ attempts, or a criterion at level ≤ 1), the mentor offers an authored remedial kata (`remedial` in `node.yaml`); if none exists, the AI generates a short challenge targeting the weak skill. It goes to a "Practice" queue and earns practice XP, not track XP.
- Generated challenges are flagged `generated` and never count towards checkpoints, because their tests are not reviewed.

**Implemented in M7a, without AI** (`dojo-core::review`, `Profile::request_hint`, `kata.hints` in `node.yaml`):
- Up to three hints per kata, opened one at a time with `H` and shown in the kata panel. Each hint opened before the kata is completed costs 10% of its XP (checkpoints included); after completion they are free. The XP stored with a completion is the XP actually earned, and belts use it.
- Spaced review instead of remedial katas: every question of a completed lesson or checkpoint becomes a card due 1, 3, 7, 21 and 60 days later. A right answer moves it one step, a miss sends it back to the first step (due the next day). `R` opens up to 8 due questions of the active track, lowest step first. Each right answer gives 5 practice XP, kept apart from track XP; a review counts as training for the streak.
- Events `hint_revealed` and `review_answered` (store schema v5). `Esc` leaves a review without recording it.
- Not yet: authored `remedial` katas and AI-generated challenges.

## 9. Careers and next steps

- **Paths** in `content/careers/*.yaml`: stages with requirements such as "track X up to phase N" or "skill Y ≥ 0.8". Examples: *Backend → Microservices Architect*, *Backend → DevOps/Cloud*, *Developer → AI Engineer*.
- **When a phase, checkpoint or track is finished**, the completion screen shows what was mastered, progress on each career path the track feeds, and the top 3 recommendations.
- **Recommendation** (pure, in `dojo-core`):

```
score(candidate) = 0.35·target_career_gap + 0.25·weak_skill_reinforcement
                 + 0.20·declared_interest + 0.10·momentum − 0.10·difficulty_jump
```

  Candidates: available nodes in the current track + entry nodes of other tracks whose skill prerequisites are met. The learner picks a target career and interests at onboarding; both are editable.

**Implemented in M7b** (`dojo-core::career`, `content/careers/*.yaml`, schema `career.schema.json`):
- Requirements: `{ track, phase }` (every node up to that phase completed), `{ skill, min }` (that share of the catalog's nodes teaching the skill completed) and `{ any_of: [...] }`. Content not written yet counts as 0, never as done; placed-out nodes count once the Belt Exam confirms them.
- A stage's progress is the mean of its requirements; the current stage is the first one not finished.
- No onboarding: `G` opens the career screen, where Enter sets or clears the target career (stored in settings). "Declared interest" is replaced by "on the active track".
- Score as above, with `career_gap` = 1 when the node teaches a skill the current stage needs and 0.5 when only its track is needed; `weak_skill` = a skill whose last review answer was wrong; `momentum` = the track is started; `difficulty_jump` = how far the node's difficulty is above the mean of the completed nodes of its track. Candidates are the open nodes of every track.
- The career screen shows the 3 best; passing a checkpoint points at the first one.
- Four paths so far: Rust Systems Engineer, Backend to Microservices Architect, Backend to DevOps and Cloud, Developer to AI Engineer.

## 10. Terminal UI

```
┌ 🦀 Rust · Green Belt · 1,240 XP · 🔥 12 · bridge: Java ────────────────────────┐
│ P0 ██████████ 3/3   P1 ███████░░░ 4/6   P2 ░░░░░░░░░░ 0/5   P3 🔒   P4 🔒          │
├──────────────────────────────┬─────────────────────────────────────────────────┤
│ ▾ P0 First Steps          ✓  │ [Lesson] [Bridge] [Kata] [Review] [History]      │
│ ▾ P1 Fundamentals            │                                                 │
│     ✓ Primitive types        │  Loops: loop, while and for                      │
│     ✓ Control flow           │  Objectives: ...                                 │
│     ◐ Loops            ← ●   │  Workspace: dojo-workspace/rust/p1/04-loops      │
ustp1│  Workspace: ~/.dojo/workspace/rust/p1/04-loops   │4-lo │
│     ○ Functions              │  Tests: 4/6 ✓  (saved 3 s ago · docker)          │
│     📋 Checkpoint 1          │                                                 │
│ ▸ P2 Intermediate         ⏭  │  [s] submit  [h] hint  [o] open folder           │
└──────────────────────────────┴─────────────────────────────────────────────────┘
```

- Icons: 🔒 locked · ○ available · ◐ in progress · ✓ passed · ★ mastered · ⏭ placed out · 📋 checkpoint.
- Screens: track selector grouped by category with progress %; placement wizard; review (findings navigable by file:line, level per criterion); career map; settings (source language, AI backend).
- One instance at a time: `~/.dojo/dojo.lock` holds the PID and start time; a second launch offers to close the first (two instances would both watch the workspace and run every check and commit twice). On Windows, `dojo.cmd` builds and runs a copy of the binary so an open Dojo never blocks the build.
- Kata panel order (after user testing on 2026-11-14): numbered steps, then the last check with one line per failing test (its panic message; `todo!` shown as "not written yet"), then the statement. Creating a kata folder does not trigger a check (3 s quiet period).
- Subcommands: `dojo`, `dojo placement <track>`, `dojo check`, `dojo submit`, `dojo hint`, `dojo runner build`, `dojo content validate`, `dojo review eval <track>`.

## 11. Content volume

29 tracks × ~20 nodes ≈ 580 nodes, ~350 katas with tests, ~2,000 questions. This is the bottleneck, not the code.

- **Pilot first:** Rust and Docker complete, to validate schema, runner, placement and reviewer on two archetypes before scaling.
- **Authoring pipeline:** AI draft → `dojo content validate` → CI checks (reference passes, starter fails, every cited skill exists, unique IDs, each phase has its checkpoint) → human review sets `status: reviewed`. Only `reviewed` content shows outside dev mode.
- **`role` tracks by composition** cut roughly a third of the writing.
- Since the dojo may become published content, sources are original (roadmap.sh is only a topic map, not copied text).

## 12. Roadmap

| Milestone | Delivery | Done when |
|---|---|---|
| M0 | Cargo workspace, `dojo-core` and `dojo-content` with schema v2 | all 29 current tracks migrated to phases (each node placed in the phase it belongs to, as English drafts) and validating — **done 2026-11-14** |
| M1 | `dojo-store` (SQLite) + `profile.json` migration + tree TUI | old progress preserved; manual toggle behind `--dev` — **done 2026-11-14** (tree TUI landed in M0; learners may still mark lessons as read) |
| M2 | Docker runner + Rust pilot track (20 nodes) | `dojo check` runs tests in a container with file watching — **M2a done 2026-11-14** (runner, workspace, auto-commit, watch, 3 Rust katas); M2b: the rest of the Rust track |
| M3 | Checkpoints and quiz engine | Rust checkpoints 1 and 2 working |
| M4 | Placement | Rust placement produces a plan and a belt exam |
| M5 | Layered review + `LocalReviewer` (Ollama) + `OfflineReviewer` + evals | local model chosen by Rust eval agreement and review time |
| M6 | Docker pilot track + Docker-specific runner profile | Docker track complete end to end |
| M7 | Mentoring, careers, recommendations | completion screen with paths and next steps |
| M8 | Content scale-up | remaining tracks, by archetype |

## 13. Open questions

None at the moment. Resolved 2026-11-14: the AI reviewer is a free local model through Ollama (section 8.4). Which model is chosen in M5 by running the evals.
