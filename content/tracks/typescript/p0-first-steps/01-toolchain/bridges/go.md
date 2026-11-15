---
from: go
status: draft
concept: "Static types without a binary"
from_way: "go build checks types and produces a native binary; the build fails on any type error."
native_way: "tsc checks types; Node runs the stripped source directly, so a type error does not stop the program from running."
sensei_tip: "Go makes the compiler the gate. With TypeScript you build that gate yourself: run tsc in CI and in a pre-commit hook. (And the TypeScript 7 compiler itself is written in Go.)"
---

| Go | TypeScript |
| --- | --- |
| `go build` / `go vet` | `tsc` |
| `go run .` | `node main.ts` |
| `go.mod` | `package.json` |
| Types checked always | Types checked only when you run `tsc` |
