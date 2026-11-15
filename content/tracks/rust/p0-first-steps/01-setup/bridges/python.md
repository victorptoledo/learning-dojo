---
from: python
status: draft
concept: "Build tool and package manager"
from_way: "pip, venv, pyproject.toml and a separate tool for each job: pytest, black, ruff, mypy."
native_way: "One tool, cargo, builds, tests, formats (cargo fmt), lints (cargo clippy) and publishes, with Cargo.toml and Cargo.lock."
sensei_tip: "uv feels close to cargo: a lock file, fast installs and one command. The big difference is that cargo compiles everything ahead of time, so the first build is slow and the rest are incremental."
---

| Python | Rust |
| --- | --- |
| pyproject.toml | Cargo.toml |
| uv.lock / poetry.lock | Cargo.lock |
| python main.py | cargo run |
| pytest | cargo test |
| ruff / black | cargo clippy / cargo fmt |
| PyPI | crates.io |
