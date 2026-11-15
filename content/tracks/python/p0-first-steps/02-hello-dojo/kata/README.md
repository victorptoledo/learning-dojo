# Hello, Dojo

Write two functions in `greet.py`.

`greet(name="", *, shout=False) -> str` returns the dojo's welcome:

```python
greet("Ada")                # "Welcome to the dojo, Ada!"
greet("  Grace Hopper  ")   # "Welcome to the dojo, Grace Hopper!"  (spaces around are removed)
greet()                     # "Welcome to the dojo, stranger!"
greet("   ")                # "Welcome to the dojo, stranger!"      (a blank name is no name)
greet("Ada", shout=True)    # "WELCOME TO THE DOJO, ADA!"
```

`shout` is **keyword-only**: `greet("Ada", True)` must raise `TypeError`.

`main(argv: list[str]) -> int` is the command line. `argv` holds the words after the program
name. It prints the greeting for all the words joined by single spaces, and returns `0`. The
word `--shout`, anywhere in `argv`, turns shouting on and is not part of the name:

```text
$ python greet.py Ada Lovelace
Welcome to the dojo, Ada Lovelace!
$ python greet.py --shout Ada
WELCOME TO THE DOJO, ADA!
$ python greet.py
Welcome to the dojo, stranger!
```

The `if __name__ == "__main__":` block at the end of the file already calls `main`.

## Running the tests yourself

The dojo runs the checks in Docker every time you save. To run the visible tests on your
machine, open a terminal in this folder and run:

```text
uv run pytest
```

uv reads `pyproject.toml`, installs Python 3.15 and pytest into `.venv` the first time, and
runs the tests in `tests/`.

There are 11 tests: 1 in `tests/test_visible.py` and 10 hidden ones, plus ruff, which must
find no mistakes (such as an unused import or an undefined name) in `greet.py`.
