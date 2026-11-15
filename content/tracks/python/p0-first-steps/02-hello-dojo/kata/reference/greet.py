"""The dojo's first kata: a greeting and a tiny command line."""

import sys


def greet(name: str = "", *, shout: bool = False) -> str:
    """Return the dojo's welcome for name, or for a stranger when name is blank."""
    who = name.strip() or "stranger"
    message = f"Welcome to the dojo, {who}!"
    return message.upper() if shout else message


def main(argv: list[str]) -> int:
    """Print the greeting for the words in argv and return the exit status."""
    shout = "--shout" in argv
    words = [word for word in argv if word != "--shout"]
    print(greet(" ".join(words), shout=shout))
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
