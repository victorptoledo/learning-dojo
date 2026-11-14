# Production Rust: Errors, Logging and Configuration

> Code that passes its tests is not yet code you can run at 3 a.m. A production program must
> explain its failures, tell you what it is doing, read its settings from the environment, and
> ship as a fast, small binary. The Rust ecosystem has a standard answer for each: `thiserror`
> and `anyhow`, `tracing`, `clap`, and `cargo build --release`.

**In this lesson you will learn**

- the difference between library errors and application errors
- `thiserror` for typed errors, `anyhow` with `context` for readable failure chains
- structured logging with `tracing`, levels and `RUST_LOG`
- configuration from command-line arguments and environment variables with `clap`
- secrets, release builds and the checks to run before you ship

---

## 1. Two kinds of errors

| | Library | Application |
| --- | --- | --- |
| Who handles the error? | the **caller**, who may want to react to each case | a **human**, reading a message or a log |
| What it needs | a public error **type** to `match` on | a clear **message** with the chain of causes |
| Tool | `thiserror` | `anyhow` |

In the config-parser kata you wrote `Display` and `Error` by hand. That is exactly what
`thiserror` generates.

## 2. `thiserror`: typed errors without boilerplate

```bash
cargo add thiserror
```

```rust
use thiserror::Error;

#[derive(Debug, Error)]
pub enum ConfigError {
    #[error("line {line}: missing '='")]
    MissingEquals { line: usize },

    #[error("unknown key `{0}`")]
    UnknownKey(String),

    #[error("cannot read the config file")]
    Io(#[from] std::io::Error),     // #[from] also generates From<io::Error>, so ? converts it
}

pub fn load(path: &str) -> Result<String, ConfigError> {
    let text = std::fs::read_to_string(path)?;   // io::Error -> ConfigError::Io
    Ok(text)
}
```

`#[error("...")]` writes the `Display` implementation, `#[from]` writes the `From` conversion and
records the cause in `source()`. Callers still get a plain enum they can `match`.

## 3. `anyhow`: one error type for the whole application

```bash
cargo add anyhow
```

```rust
use anyhow::{Context, Result, bail};

fn read_port(path: &str) -> Result<u16> {             // anyhow::Result<T> = Result<T, anyhow::Error>
    let text = std::fs::read_to_string(path)
        .with_context(|| format!("reading {path}"))?;  // adds a line to the chain
    let port: u16 = text.trim().parse().context("the port must be a number")?;
    if port < 1024 {
        bail!("port {port} needs root; pick 1024 or higher");  // return an ad-hoc error
    }
    Ok(port)
}

fn main() -> Result<()> {
    let port = read_port("port.txt")?;
    println!("listening on {port}");
    Ok(())
}
```

If `port.txt` is missing, `main` returns the error and the program prints:

```text
Error: reading port.txt

Caused by:
    The system cannot find the file specified. (os error 2)
```

`anyhow::Error` accepts **any** error type with `?`, so you stop writing conversions in
application code. You lose the ability to `match` on the exact type, which an application rarely
needs.

> [!TIP]
> Add `.context(...)` at every boundary where something can fail for reasons outside your code:
> files, network, parsing user input. A message saying *what you were trying to do* saves hours.

> [!WARNING]
> **Common mistake:** `unwrap()` on I/O in a service. One missing file and the process dies with
> `called Result::unwrap() on an Err value`, without saying which file or why. Use `?` and
> `context`; keep `expect("reason")` for true invariants.

## 4. Logging with `tracing`

`println!` is for the program's output. Diagnostics go to a **logger**, which adds a level, a
timestamp and the source, and can be filtered without recompiling.

```bash
cargo add tracing
cargo add tracing-subscriber --features env-filter
```

```rust
use tracing::{debug, error, info, warn, instrument};

#[instrument]                       // opens a span with the function's arguments as fields
fn process_order(order_id: u64, items: usize) {
    info!(items, "processing order");
    if items == 0 {
        warn!("empty order");
    }
    debug!(step = "validate", "details only shown at debug level");
}

fn main() {
    tracing_subscriber::fmt()
        .with_env_filter(tracing_subscriber::EnvFilter::from_default_env())
        .init();

    process_order(42, 3);
    error!(code = 500, "something went wrong");
}
```

```bash
RUST_LOG=info cargo run                 # info, warn and error
RUST_LOG=debug cargo run                # everything down to debug
RUST_LOG=my_app=debug,hyper=warn cargo run   # per crate
```

In PowerShell: `$env:RUST_LOG = "info"; cargo run`.

| Level | Use it for |
| --- | --- |
| `error!` | something failed and needs attention |
| `warn!` | unexpected, but the program carries on |
| `info!` | the important steps: started, connected, request done |
| `debug!` | details useful while investigating |
| `trace!` | very noisy, step by step |

**Fields** (`items`, `code = 500`) are what make logs useful in production: a log platform can
filter `code = 500` or chart `items` without parsing text. `.json()` on the subscriber switches to
one JSON object per line.

## 5. Configuration: arguments and environment

```bash
cargo add clap --features derive,env
```

```rust
use clap::Parser;

/// A tiny HTTP service.
#[derive(Debug, Parser)]
#[command(version, about)]
struct Config {
    /// Port to listen on.
    #[arg(long, env = "APP_PORT", default_value_t = 8080)]
    port: u16,

    /// Database URL (keep it out of the source code).
    #[arg(long, env = "DATABASE_URL")]
    database_url: String,

    /// Print more details.
    #[arg(short, long)]
    verbose: bool,
}

fn main() {
    let config = Config::parse();   // exits with a helpful message on bad input
    println!("port {}", config.port);
}
```

```bash
cargo run -- --port 3000 --database-url postgres://localhost/shop
APP_PORT=3000 DATABASE_URL=postgres://localhost/shop cargo run
cargo run -- --help     # generated from the doc comments
```

The order of precedence is: command-line flag, then environment variable, then default. This
fits the [twelve-factor](https://12factor.net/config) habit of configuring services through the
environment.

> [!IMPORTANT]
> **Secrets** (passwords, tokens, API keys) come from the environment or a secret manager, never
> from the source code or a committed file. Read them at start-up and fail immediately if one is
> missing, instead of failing on the first request.

## 6. Release builds

```bash
cargo build --release      # optimised binary in target/release/
```

A debug build can be 10 to 50 times slower. Tune the release profile in `Cargo.toml` when size or
speed matter:

```toml
[profile.release]
lto = true            # optimise across crates
codegen-units = 1     # slower build, faster code
strip = true          # remove debug symbols: smaller binary
panic = "abort"       # smaller, but no unwinding
```

The result is **one native binary** with no runtime to install: copy it into a small container
image (`FROM gcr.io/distroless/cc` or `scratch` with a static build) and run it.

## 7. Before you ship

| Check | Command |
| --- | --- |
| formatting | `cargo fmt --check` |
| lints, as errors | `cargo clippy --all-targets -- -D warnings` |
| tests | `cargo test` |
| known vulnerabilities in dependencies | `cargo audit` (install with `cargo install cargo-audit`) |
| outdated dependencies | `cargo update`, then the tests again |

Put the first three in CI so a red build stops a bad merge.

> [!NOTE]
> **Coming from Java:** `thiserror` replaces your custom exception classes, `anyhow` plays the
> role of "throws Exception + a good stack trace", `tracing` is SLF4J with structured fields and
> MDC built in (spans), and `clap` with `env` covers what Spring's `@Value` and profiles do for
> configuration. The deploy artefact is a single binary instead of a jar plus a JVM.

## Practice

1. Turn the config-parser kata's error enum into a `thiserror` enum and delete the hand-written
   `Display`.
2. Write a `main() -> anyhow::Result<()>` that reads a file given on the command line, with
   `.with_context` naming the file. Run it with a missing file and read the chain.
3. Add `tracing` with `EnvFilter`, log the file size at `info` and every line at `debug`, and
   switch between them with `RUST_LOG`.
4. Build with `--release` and compare the run time with the debug build on a big file.

## Cheat sheet

| I want to... | Write |
| --- | --- |
| a typed library error | `#[derive(Debug, thiserror::Error)] enum E { #[error("...")] A }` |
| convert a cause with `?` | `#[from] std::io::Error` |
| an application result | `fn main() -> anyhow::Result<()>` |
| explain a failure | `.context("doing X")?` |
| return an ad-hoc error | `bail!("message {x}")` |
| log an event with fields | `info!(user_id, "login")` |
| filter logs | `RUST_LOG=my_app=debug` |
| flags with env fallback | `#[arg(long, env = "APP_PORT")]` |
| build for production | `cargo build --release` |

**Ready?** Press **Q** in the dojo for the quiz.
