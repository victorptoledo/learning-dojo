---
from: python
status: draft
concept: "Python's compiler and interpreter"
from_way: "Calling Python purely interpreted."
native_way: "CPython tokenizes, parses with a PEG parser into an AST, compiles to stack bytecode and interprets it; the ast module exposes the tree and dis the bytecode."
sensei_tip: "`ast.parse(src)` and `ast.dump(tree, indent=2)` show the AST of any code: the same kind of tree your parser kata builds."
---

| In Python | Underneath |
| --- | --- |
| `tokenize` module | the lexer |
| `ast.parse` | the parser's AST |
| `compile(src, f, 'exec')` | AST to a code object |
| `dis.dis` | stack bytecode: LOAD_FAST, BINARY_OP |
| PyPy | a tracing JIT for Python |
