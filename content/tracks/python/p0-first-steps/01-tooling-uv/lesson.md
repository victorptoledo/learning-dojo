# Python 3.15, uv and pyproject.toml

Python is a dynamically typed, garbage-collected language with a huge standard library and
an even bigger ecosystem on PyPI. It runs web back ends, data pipelines, machine learning,
automation and the glue between all of them. This track teaches the Python written today:
typed, tested, described by `pyproject.toml` and managed with **uv**.

## Python, CPython and versions

"Python" is a language; **CPython** is its reference implementation, written in C, and the
one you get from python.org or from uv. (PyPy, a JIT-compiled implementation, and
MicroPython exist too, but nearly everything in production runs CPython.)

Since 2019 a new feature version comes out every October (PEP 602) and is supported for five
years: about two years of bug fixes, then security fixes only.

| Version | Released | What it brought, in short |
| --- | --- | --- |
| 3.11 | 2022 | A 10–60% faster interpreter, exception groups, `tomllib` |
| 3.12 | 2023 | The `type` statement and new generic syntax (PEP 695), a new f-string grammar |
| 3.13 | 2024 | A new interactive REPL, an experimental free-threaded build (no GIL) and JIT, `copy.replace` |
| 3.14 | 2025 | The free-threaded build officially supported, deferred annotations, template strings |
| **3.15** | 2026 | Lazy imports, `frozendict` and `sentinel` built-ins, UTF-8 by default, the Tachyon sampling profiler |

The dojo's katas run on **Python 3.15**, the current release. Production systems often run a
version or two behind, so the lessons mark what is new ("since 3.15"): check the project's
`requires-python` before you use a new feature at work.

> [!NOTE]
> Python 2 reached its end of life in 2020. If you meet `print "hello"` or `urllib2`, you are
> reading code that is more than six years out of date.

## Installing: uv

**uv** is a single fast binary (written in Rust) that replaces `pyenv`, `pip`, `virtualenv`,
`pip-tools`, `pipx` and most of Poetry. On Windows:

```text
winget install astral-sh.uv
uv python install 3.15        # downloads a managed CPython 3.15
uv run python --version       # Python 3.15.x
```

You do not need a system-wide Python at all: uv downloads the interpreter a project asks for
and keeps it in its own cache.

## A project: `uv init`, `uv add`, `uv run`

```text
uv init orders            # creates orders/ with pyproject.toml, main.py and .python-version
cd orders
uv add httpx              # a runtime dependency
uv add --dev pytest ruff  # tools for development only
uv run python main.py     # runs inside the project's environment
uv run pytest             # runs the tests
```

The first `uv run` (or `uv add`) creates **`.venv`**, a virtual environment, installs the
dependencies into it and writes **`uv.lock`**. Later runs check the lock and change only what
is out of date, which takes milliseconds.

### pyproject.toml

`pyproject.toml` is the standard file that describes a Python project (PEP 518, 621 and 735):

```toml
[project]
name = "orders"
version = "0.1.0"
description = "Order service"
requires-python = ">=3.15"
dependencies = [
    "httpx>=0.28",
]

[dependency-groups]
dev = ["pytest>=9", "ruff>=0.17"]

[tool.pytest.ini_options]
testpaths = ["tests"]

[tool.ruff]
line-length = 100
```

| Table | Holds |
| --- | --- |
| `[project]` | Name, version, the Python versions supported and the **runtime** dependencies, as ranges |
| `[dependency-groups]` | Groups that are never published: `dev`, `test`, `docs` |
| `[build-system]` | How to build a wheel; needed only for libraries and installable apps (see the packaging lesson) |
| `[tool.*]` | Settings for tools: pytest, ruff, mypy, uv |

### Virtual environments

A **virtual environment** is a folder with its own `site-packages` and a link to an
interpreter. Each project gets one, so two projects can depend on different versions of the
same library. Without uv you create and activate one yourself:

```text
python -m venv .venv
.venv\Scripts\activate          # Windows; source .venv/bin/activate on Linux and macOS
pip install -r requirements.txt
```

`uv run` uses the project's `.venv` without activating anything. `.venv` is a local build
artefact: **never commit it**.

### Lock files

`dependencies = ["httpx>=0.28"]` says what the project accepts. The **lock file** records what
was actually chosen: the exact version and hash of `httpx` **and of every package it depends
on**, for every platform. Commit `uv.lock`; `uv sync --locked` in CI installs exactly that
set, or fails when the lock is out of date.

A hand-written `requirements.txt` with `httpx>=0.28` pins nothing below it, so two installs a
month apart can differ. That is the usual cause of "it works on my machine". (When a tool
needs a requirements file, `uv export` writes one from the lock.)

## Scripts and tools without a project

```text
uvx ruff check .            # run a tool in a throwaway environment (like pipx run)
uv run --with rich demo.py  # run a script with an extra package
```

A single-file script can declare its own dependencies in a comment block (PEP 723), and
`uv run script.py` reads it:

```python
# /// script
# requires-python = ">=3.15"
# dependencies = ["httpx"]
# ///
import httpx

print(httpx.get("https://peps.python.org/api/peps.json").status_code)
```

## The REPL and running code

`uv run python` opens the interactive interpreter. Since 3.13 it has colours, multi-line
editing and history; `help(str)` and `dir(obj)` are the fastest documentation you have.

A module run as a program has `__name__ == "__main__"`; when it is imported, `__name__` is
its module name. That is why scripts end with:

```python
def main() -> int:
    print("hello")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
```

## How the dojo checks Python katas

A Python kata folder is a small uv project:

| File | Role |
| --- | --- |
| `README.md` | The statement |
| `greet.py` (or other modules) | Your code. Functions still to write raise `NotImplementedError("todo ...")` |
| `tests/test_visible.py` | Tests you can read and run with `uv run pytest` |
| `pyproject.toml` | The Python version, pytest as a dev dependency and pytest settings |

When you save, the dojo copies the folder into the `dojo-python` container (Python 3.15,
pytest and ruff, no network), adds the hidden grading tests next to yours and runs:

1. a syntax check of every module: a `SyntaxError` is shown like a compile error;
2. `pytest`: each test is one line in the report;
3. `ruff check` on each module, as one more test per module. It flags only real mistakes:
   syntax errors, undefined names, unused imports and variables, mutable default arguments
   and a `raise` inside `except` without `from`.

> [!TIP]
> Run `uv run pytest -x -q` while you work: `-x` stops at the first failure and `-q` keeps the
> output short. VS Code's Testing panel (Python extension) runs and debugs the same tests.

## Summary

- CPython is the Python; one version per year, five years of support. The dojo uses **3.15**.
- **uv** installs Python, creates `.venv`, resolves and locks dependencies and runs code.
- `pyproject.toml` describes the project; `uv.lock` pins every version and is committed;
  `.venv` is not.
- Katas are uv projects tested with pytest and ruff in a sandboxed container.

## Further reading

- uv documentation, "Working on projects": <https://docs.astral.sh/uv/guides/projects/>
- Python Packaging User Guide, "Writing your pyproject.toml": <https://packaging.python.org/en/latest/guides/writing-pyproject-toml/>
- What's New in Python 3.15: <https://docs.python.org/3/whatsnew/3.15.html>
- PEP 602, the annual release cycle: <https://peps.python.org/pep-0602/>
- PEP 723, inline script metadata: <https://peps.python.org/pep-0723/>
- PEP 735, dependency groups: <https://peps.python.org/pep-0735/>
- roadmap.sh, Python topic "uv": <https://github.com/nilbuild/developer-roadmap/blob/master/roadmaps/python/content/uv@p3Frfs6oxpuciUzeCEsvb.md>
