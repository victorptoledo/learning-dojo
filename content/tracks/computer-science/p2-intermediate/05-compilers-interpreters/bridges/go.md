---
from: go
status: draft
concept: "Go's compiler"
from_way: "Assuming Go has a virtual machine."
native_way: "The gc compiler parses Go into an AST (go/ast mirrors it), type-checks it, lowers it to SSA, optimises and emits machine code: no bytecode and no JIT."
sensei_tip: "`GOSSAFUNC=f go build` writes ssa.html showing every compiler pass for function f: the back end of a real compiler, step by step."
---

| In Go | Underneath |
| --- | --- |
| `go/scanner`, `go/parser` | lexer and parser |
| `go/ast`, `go/types` | the AST and type checking |
| SSA passes | the optimiser |
| `go vet`, `gofmt` | tools built on the AST |
| `text/template` | a small lexer, parser and tree-walking interpreter |
