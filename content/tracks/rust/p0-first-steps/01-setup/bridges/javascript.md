---
from: javascript
status: draft
concept: "Build tool and package manager"
from_way: "npm or pnpm with package.json, scripts for build and test, and node_modules per project."
native_way: "cargo with Cargo.toml: dependencies, build, test, format and lint without extra packages."
sensei_tip: "Cargo.lock is package-lock.json: commit it for applications. Features in Cargo.toml are like optional peer functionality, chosen at compile time."
---

| JavaScript / TypeScript | Rust |
| --- | --- |
| package.json | Cargo.toml |
| package-lock.json | Cargo.lock |
| npm run build | cargo build --release |
| npm test (jest, vitest) | cargo test |
| eslint / prettier | cargo clippy / cargo fmt |
| npmjs.com | crates.io |
