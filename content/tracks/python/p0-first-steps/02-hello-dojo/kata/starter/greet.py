"""The dojo's first kata: a greeting and a tiny command line."""

import sys


def greet(name: str = "", *, shout: bool = False) -> str:
    """Return the dojo's welcome for name, or for a stranger when name is blank."""
    raise NotImplementedError("todo: write greet()")


def main(argv: list[str]) -> int:
    """Print the greeting for the words in argv and return the exit status."""
    raise NotImplementedError("todo: write main()")


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
