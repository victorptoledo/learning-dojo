---
from: go
status: draft
concept: "Build tool and package manager"
from_way: "go mod, go build, go test and gofmt: one toolchain with modules."
native_way: "cargo, the same all-in-one idea, with Cargo.toml for dependencies and features."
sensei_tip: "Go compiles fast and Rust compiles slowly but checks much more. Use cargo check while you code: it type-checks without generating a binary and is much faster than cargo build."
---

| Go | Rust |
| --- | --- |
| go.mod | Cargo.toml |
| go.sum | Cargo.lock |
| go run . | cargo run |
| go test ./... | cargo test |
| gofmt / go vet | cargo fmt / cargo clippy |
| pkg.go.dev | docs.rs |
