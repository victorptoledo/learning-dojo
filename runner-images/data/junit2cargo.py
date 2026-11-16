"""Turns a pytest JUnit XML report, plus one ruff check per module, into `cargo test` lines.

Usage: junit2cargo.py REPORT.xml MODULE...

Prints "test name ... ok|FAILED" per test, then one "---- name stdout ----" block per
failure. Exits 0 when everything passed, 1 when something failed, 2 when no test ran.
"""

import re
import subprocess
import sys
import xml.etree.ElementTree as ElementTree
from pathlib import Path

# Mistakes, not style: syntax errors, pyflakes (undefined names, unused imports and
# variables) and a few bugbear checks taught in the track (mutable defaults, raise from).
RUFF_RULES = "E9,F,B006,B012,B904"
MAX_MESSAGE_LINES = 8
GENERIC_MESSAGES = ("collection failure", "failed on setup", "failed on teardown")


def test_name(case: ElementTree.Element) -> str:
    classname = case.get("classname", "")
    parts = [part for part in classname.split(".") if part and part != "tests"]
    parts.append(case.get("name", "test"))
    return re.sub(r"\s+", "_", "::".join(parts))


def message_of(problem: ElementTree.Element) -> str:
    """The short reason a test failed, with the placeholder first when it is one."""
    message = problem.get("message", "") or ""
    body = problem.text or ""
    if not message or message.startswith(GENERIC_MESSAGES):
        error_lines = [line[1:].strip() for line in body.splitlines() if line.startswith("E ")]
        message = "\n".join(error_lines) or message or "the test failed"
    if "NotImplementedError: todo" in message or "NotImplementedError: todo" in body:
        found = re.search(r"NotImplementedError: (todo[^\n]*)", message + "\n" + body)
        if found:
            return found.group(1).strip()
    message = message.removeprefix("AssertionError: ")
    lines = [line.strip() for line in message.splitlines() if line.strip()]
    return "\n".join(lines[:MAX_MESSAGE_LINES])


def pytest_results(report: Path) -> list[tuple[str, bool, str]]:
    if not report.is_file():
        return []
    root = ElementTree.parse(report).getroot()
    results = []
    for case in root.iter("testcase"):
        if case.find("skipped") is not None:
            continue
        problem = case.find("failure")
        if problem is None:
            problem = case.find("error")
        if problem is None:
            results.append((test_name(case), True, ""))
        else:
            results.append((test_name(case), False, message_of(problem)))
    return results


def ruff_results(modules: list[str]) -> list[tuple[str, bool, str]]:
    results = []
    for module in modules:
        name = "ruff::" + re.sub(r"[^A-Za-z0-9_]+", "_", module).strip("_")
        found = subprocess.run(
            ["ruff", "check", "--isolated", "--no-cache", "--quiet", "--select", RUFF_RULES,
             "--target-version", "py315", "--output-format", "concise", module],
            capture_output=True,
            text=True,
            check=False,
        )
        if found.returncode == 0:
            results.append((name, True, ""))
        else:
            lines = [line for line in (found.stdout + found.stderr).splitlines() if line.strip()]
            results.append((name, False, "\n".join(lines[:MAX_MESSAGE_LINES])))
    return results


def main() -> int:
    report, modules = Path(sys.argv[1]), sys.argv[2:]
    tests = pytest_results(report)
    if not tests:
        return 2
    results = tests + ruff_results(modules)
    print(f"\nrunning {len(results)} tests")
    for name, passed, _ in results:
        print(f"test {name} ... {'ok' if passed else 'FAILED'}")
    failures = [(name, message) for name, passed, message in results if not passed]
    passed = len(results) - len(failures)
    if failures:
        print("\nfailures:\n")
        for name, message in failures:
            print(f"---- {name} stdout ----")
            print(f"thread '{name}' panicked at {name}:")
            print(f"{message}\n")
        print(f"failures:\n\ntest result: FAILED. {passed} passed; {len(failures)} failed")
        return 1
    print(f"\ntest result: ok. {passed} passed; 0 failed")
    return 0


if __name__ == "__main__":
    sys.exit(main())
