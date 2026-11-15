# Modules, Packages, Imports and Packaging

A Python file is a **module**; a folder of modules is a **package**; a built archive of a
package with its metadata is a **distribution** (a wheel) that others can install. This
lesson follows that path, from `import` to `uv publish`.

## What import does

```python
import json                    # binds the name json to the module object
from pathlib import Path       # binds Path, from inside the module
import numpy as np             # binds under another name
```

On the first import of a name, Python:

1. looks in **`sys.modules`**, the cache: if the module is there, it is reused;
2. otherwise searches **`sys.path`** in order: the folder of the script you ran (or the
   current directory with `python -m`), then the standard library, then `site-packages` of
   the active environment;
3. **executes the module's code once**, top to bottom, and stores the module in the cache.

So module-level code runs on import, exactly once per process. That is why modules should
only *define* things at the top level (functions, classes, constants) and do work in
functions or under `if __name__ == "__main__":`.

## Packages

```text
src/invoicer/
    __init__.py        # makes the folder a regular package; runs on `import invoicer`
    __main__.py        # runs on `python -m invoicer`
    cli.py
    pdf/
        __init__.py
        render.py
```

`import invoicer.pdf.render` imports `invoicer`, then `invoicer.pdf`, then the module.
Keep `__init__.py` small: a docstring, perhaps `__version__` and a few re-exports that define
the public API:

```python
# src/invoicer/__init__.py
"""Create and send invoices."""
from invoicer.model import Invoice, Line

__all__ = ["Invoice", "Line"]
```

`__all__` lists the public names; it is also what `from invoicer import *` imports (avoid
star imports in your own code: they hide where names come from).

### Absolute and relative imports

```python
from invoicer.pdf.render import render   # absolute: always clear, works from anywhere
from .render import render               # relative, inside the package only
```

Prefer absolute imports. Never use `sys.path.append(...)` to make imports work: it means the
code is not laid out as a package.

### Lazy imports (3.15)

Big applications often spend seconds at start-up importing modules they never use in that run.
Python 3.15 adds the `lazy` soft keyword (PEP 810): the import statement stays at the top of
the file, but the module is loaded only when the name is **first used**.

```python
lazy import json
lazy from invoicer.pdf import render     # the PDF libraries load only if a PDF is made

def export(invoice, fmt: str) -> bytes:
    if fmt == "pdf":
        return render(invoice)            # invoicer.pdf is imported here, on first use
    return json.dumps(invoice.to_dict()).encode()
```

Lazy imports are allowed only at module level (not inside functions, classes or `try`), and an
import error appears at the first use, with a traceback that points to both places.
`python -X lazy_imports=all` (or `PYTHON_LAZY_IMPORTS=all`) makes every import lazy, which is
a quick way to measure the start-up gain; code that must also run on older versions can list
module names in `__lazy_modules__` instead of using the keyword.

### Circular imports

If `a.py` imports `b` at the top and `b.py` imports `a` at the top, one of them sees a
half-initialised module and fails with `ImportError: cannot import name ...`. Fixes, from
best to worst: move the shared code to a third module; import the module instead of a name
(`import invoicer.a` and use `invoicer.a.thing` at call time); import inside the function
that needs it. For imports needed only by type hints, use `if TYPE_CHECKING:`. A `lazy` import also helps
when the other module's names are only used inside functions, because it is loaded later.

## The src layout

```text
invoicer/
    pyproject.toml
    README.md
    src/invoicer/...
    tests/test_render.py
```

Putting the package under `src/` means tests can only import it **as installed**, not by
accident from the current directory, so packaging mistakes (a missing file, a wrong name)
show up in your tests instead of on a user's machine. `uv init --package invoicer` creates
this layout.

## Building: pyproject.toml for a distribution

```toml
[project]
name = "invoicer"
version = "1.4.0"
description = "Create and send invoices"
readme = "README.md"
requires-python = ">=3.15"
license = "MIT"
dependencies = ["httpx>=0.28,<1", "jinja2>=3.1"]

[project.optional-dependencies]
pdf = ["weasyprint>=66"]            # pip install "invoicer[pdf]"

[project.scripts]
invoicer = "invoicer.cli:main"      # creates an `invoicer` command that calls main()

[build-system]
requires = ["uv_build>=0.9,<0.14"]
build-backend = "uv_build"
```

The **build backend** (`uv_build`, `hatchling`, `setuptools`, `flit-core`, `pdm-backend`)
turns the source tree into distributions; any standard front end (`uv build`, `pip`) can
drive it.

```text
uv build          # writes dist/invoicer-1.4.0.tar.gz (sdist) and dist/invoicer-1.4.0-py3-none-any.whl
```

A **wheel** is a zip with the code and a `METADATA` file: installing it is just unpacking. A
pure-Python wheel (`py3-none-any`) works everywhere; packages with C or Rust extensions ship
one wheel per platform.

When you run `uv sync` in the project, uv installs your own package into `.venv` in
**editable** mode, so the tests import the live source and `invoicer` works as a command.

### Versions and specifiers

Versions follow PEP 440 (`1.4.0`, `2.0.0rc1`, `1.4.0.post1`); most projects use semantic
versioning on top. Dependency specifiers:

| Specifier | Means |
| --- | --- |
| `>=3.1` | 3.1 or newer |
| `>=0.28,<1` | at least 0.28, below 1.0 |
| `~=3.1` | compatible release: `>=3.1, ==3.*` |
| `==2.2.*` | any 2.2.x |

**Libraries** declare broad ranges (their users must be able to resolve them together);
**applications** declare ranges too, but deploy from the lock file.

## Publishing and installing

```text
uv publish                       # uploads dist/* to PyPI (token or trusted publishing from CI)
uv publish --index testpypi      # practise on TestPyPI first (index defined in pyproject.toml)

uv tool install invoicer         # installs the command in its own environment (like pipx)
uv add invoicer                  # or as a dependency of another project
```

Trusted publishing (OIDC from GitHub Actions or GitLab CI) avoids long-lived API tokens.
Private packages go to a private index (Artifactory, a cloud registry) configured with
`[[tool.uv.index]]`.

## Data files and metadata at run time

```python
from importlib.metadata import version
from importlib.resources import files

version("invoicer")                                       # '1.4.0', from the installed metadata
template = files("invoicer").joinpath("templates/invoice.html").read_text()
```

Read package data through `importlib.resources`, never through paths relative to
`__file__`: it also works when the package is inside a zip.

## Summary

- import searches `sys.path`, runs a module once and caches it in `sys.modules`.
- Packages are folders with `__init__.py`; keep it small and define `__all__`; use absolute
  imports; break cycles by moving shared code.
- Use the src layout. `pyproject.toml` with `[project]`, `[project.scripts]` and
  `[build-system]` makes a distribution.
- `uv build` makes the wheel and sdist, `uv publish` uploads them, `uv tool install` installs
  a command.

## Further reading

- The Python Tutorial, "Modules": <https://docs.python.org/3/tutorial/modules.html>
- The import system (Language Reference): <https://docs.python.org/3/reference/import.html>
- Python Packaging User Guide, "Packaging Python Projects": <https://packaging.python.org/en/latest/tutorials/packaging-projects/>
- Python Packaging User Guide, "src layout vs flat layout": <https://packaging.python.org/en/latest/discussions/src-layout-vs-flat-layout/>
- uv documentation, "Building and publishing a package": <https://docs.astral.sh/uv/guides/package/>
- PEP 440, version identifiers and specifiers: <https://peps.python.org/pep-0440/>
- PEP 810, explicit lazy imports: <https://peps.python.org/pep-0810/>
- roadmap.sh, Python topic "Modules": <https://github.com/nilbuild/developer-roadmap/blob/master/roadmaps/python/content/modules@274uk28wzxn6EKWQzLpHs.md>
