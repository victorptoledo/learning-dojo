---
from: python
status: draft
concept: "Type hints that are checked by a separate tool"
from_way: "Type hints are ignored by the interpreter and checked by mypy or pyright; the program runs with wrong hints."
native_way: "The same model: annotations are stripped by Node and checked by tsc; but TypeScript's checker is part of the language and strict by default."
sensei_tip: "The workflow is the one you know from mypy: the interpreter runs, the checker checks. The difference is culture: in TypeScript, unchecked code is the exception, not the norm."
---

| Python | TypeScript |
| --- | --- |
| `python app.py` (hints ignored) | `node app.ts` (types stripped) |
| `mypy .` / `pyright` | `tsc` |
| `pyproject.toml` `[tool.mypy] strict = true` | `tsconfig.json` `"strict": true` |
| `uv add --dev mypy` | `npm install -D typescript` |
| `typing.TYPE_CHECKING` imports | `import type` |
