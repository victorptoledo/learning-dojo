# Sidequest: File indexer

Build `findex`, a small cousin of `grep` that can also index a folder and answer word queries
from the index alone. Start from an empty `main`; the design is yours. Good places for this
phase's skills: iterator chains over files and lines, an error enum with `From` conversions
and `?`, a trait or a module per command, unit tests next to the code.

The contract tests run the `findex` binary on folders they create, then read stdout, stderr
and the exit code. Paths in the output are **relative to the folder searched** and always use
`/`, so the output is the same on every system.

- Keep the package name `findex`; use the standard library only (the checks run offline).
- Try it with `cargo run -- search . fn`.
