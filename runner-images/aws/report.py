"""Turns a pytest JUnit XML report, plus one ruff check per Python module and one cfn-lint
check per CloudFormation template, into `cargo test` lines.

Usage: report.py REPORT.xml MODULE... -- DOCUMENT...

DOCUMENTs are the kata's YAML and JSON files; the ones with a top-level `Resources` map are
CloudFormation (or SAM) templates and get a `cfn-lint::<file>` test. Prints
"test name ... ok|FAILED" per test, then one "---- name stdout ----" block per failure.
Exits 0 when everything passed, 1 when something failed, 2 when no test ran.
"""

import re
import subprocess
import sys
import xml.etree.ElementTree as ElementTree
from pathlib import Path

from dojo_aws import is_template, load

# Mistakes, not style: syntax errors, pyflakes (undefined names, unused imports and
# variables) and a few bugbear checks (mutable defaults, raise from).
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
    # A test's own message says it all; pytest's "assert x == y / + where ..." lines after
    # it are noise for a learner.
    explained = next((index for index, line in enumerate(lines) if line.startswith("assert ")), None)
    if explained:
        lines = lines[:explained]
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


def check_name(tool: str, file: str) -> str:
    return tool + "::" + re.sub(r"[^A-Za-z0-9_]+", "_", file).strip("_")


def ruff_results(modules: list[str]) -> list[tuple[str, bool, str]]:
    results = []
    for module in modules:
        found = subprocess.run(
            ["ruff", "check", "--isolated", "--no-cache", "--quiet", "--select", RUFF_RULES,
             "--target-version", "py315", "--output-format", "concise", module],
            capture_output=True,
            text=True,
            check=False,
        )
        lines = [line for line in (found.stdout + found.stderr).splitlines() if line.strip()]
        results.append((check_name("ruff", module), found.returncode == 0,
                        "\n".join(lines[:MAX_MESSAGE_LINES])))
    return results


def cfn_lint_results(documents: list[str]) -> list[tuple[str, bool, str]]:
    """cfn-lint with its bundled schemas (no network): errors and warnings fail."""
    results = []
    for document in documents:
        if not is_template(load(document)):
            continue
        found = subprocess.run(
            ["cfn-lint", "--format", "parseable", "--regions", "eu-west-1",
             "--non-zero-exit-code", "warning", "--", document],
            capture_output=True,
            text=True,
            check=False,
        )
        lines = [line for line in (found.stdout + found.stderr).splitlines() if line.strip()]
        results.append((check_name("cfn-lint", document), found.returncode == 0,
                        "\n".join(lines[:MAX_MESSAGE_LINES])))
    return results


def main() -> int:
    args = sys.argv[1:]
    split = args.index("--") if "--" in args else len(args)
    report, modules, documents = Path(args[0]), args[1:split], args[split + 1:]
    tests = pytest_results(report)
    if not tests:
        return 2
    results = tests + cfn_lint_results(documents) + ruff_results(modules)
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
