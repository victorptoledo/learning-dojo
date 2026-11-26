# Checkpoint 3: Build Planner

A tiny build tool in three parts. It reads a build file **without copying it**, groups the
targets into waves that respect their dependencies, and runs the build **in the background** on a
pool of threads that you can watch and cancel.

## Input format

One target per line: a name, a colon, and the targets it depends on, separated by spaces.

```text
# a small workspace
core:
utils: core
app: utils core
docs:
```

- Blank lines and lines starting with `#` (after trimming) are skipped.
- Names are trimmed. A target may have no dependencies (`docs:`).
- Line numbers start at 1 and count every line, skipped ones included.

## Part 1: `parse(source) -> Result<Vec<Target<'_>>, ParseError>`

`Target<'a>` holds `&'a str`, so every name must be a slice of `source`: no `String`, no
`to_string`, no leaking. The tests check that each name points inside the source text.

- Keep the order of the file. A dependency listed twice is kept once, at its first position.
- The **first** problem stops parsing:
  - `MissingColon { line }`: the line has no `:`;
  - `EmptyName { line }`: nothing before the colon;
  - `Duplicate { line, name }`: the target was already defined (`line` is the second one).
- `Display`: `line 3: missing ':'`, `line 3: empty target name`,
  ``line 4: target `app` is defined twice``.

## Part 2: `plan(targets) -> Result<Vec<Vec<&'a str>>, PlanError>`

- Wave 1 holds the targets without dependencies; each later wave holds the targets whose
  dependencies are all in earlier waves. A target goes in the **first** wave it can run in.
  Each wave is sorted by name.
- Read the signature: the names in the plan borrow from the **source** (`'a`), not from the
  `targets` slice, so the plan can outlive the list of targets.
- Errors:
  - `UnknownDependency { target, dependency }`: the first unknown dependency, in file order;
  - `Cycle { targets }`: when no more waves can be formed, every target left over, sorted
    (the ones in the cycle and the ones that depend on it). A target that depends on itself is
    a cycle.
- `Display`: ``` `b` depends on unknown target `x` ``` and `dependency cycle among b, c`.

## Part 3: `start(waves, workers, job) -> Build`

The build runs on other threads, so it cannot borrow anything: it takes **owned** waves and a
`Job`, an `Arc<dyn Fn(&str) -> Result<u64, String> + Send + Sync>` shared by every thread.

- `start` returns **at once**; the work happens in the background.
- At most `workers` jobs run at the same time (`0` means 1), and the jobs of a wave do run in
  parallel.
- A wave starts only after every job of the previous wave has returned.
- A job that returns `Err` or **panics** is a failure (a panic is reported as `"panicked"`). The
  rest of its wave still runs; every later wave is **skipped**.
- `Build::finished()` is the number of jobs that have returned so far, while the build runs.
- `Build::cancel()`: jobs already running finish and are reported; jobs not started yet are
  skipped. No job starts after `cancel`.
- `Build::wait(self) -> Report`: waits for the end. `built` and `failed` are sorted by name,
  `skipped` is sorted.
- `Build` must be `Send`, so it can be handed to another thread.

> This kata reviews the whole phase: structs that borrow with a lifetime, the elision rules and
> when they are not enough, owning data to send it to a thread, `Arc`, `Mutex`, atomics,
> channels, scoped threads and `Send`.
