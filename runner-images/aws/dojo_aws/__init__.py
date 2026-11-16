"""Helpers the AWS kata tests import: reading templates and policies, running cfn-guard.

    from dojo_aws import template, resources, guard
    from dojo_aws import iam

Templates are read with cfn-lint's own parser, so the short forms (`!Ref X`, `!Sub ...`,
`!GetAtt A.B`) come back as their long JSON forms (`{"Ref": "X"}`, `{"Fn::Sub": ...}`,
`{"Fn::GetAtt": ["A", "B"]}`). Nothing here talks to AWS.
"""

import json
import subprocess
from pathlib import Path
from typing import Any

from cfnlint.decode import decode_str

__all__ = ["getatt", "guard", "is_template", "load", "outputs", "parameters", "ref", "resource", "resources",
           "template"]


def _plain(value: Any) -> Any:
    """cfn-lint's node types (dict_node, str_node...) as plain dicts, lists and strings."""
    if isinstance(value, dict):
        return {str(key): _plain(item) for key, item in value.items()}
    if isinstance(value, list):
        return [_plain(item) for item in value]
    if isinstance(value, bool) or value is None:
        return value
    if isinstance(value, int):
        return int(value)
    if isinstance(value, float):
        return float(value)
    if isinstance(value, str):
        return str(value)
    return value


def load(path: str | Path) -> Any:
    """A YAML or JSON file (CloudFormation short forms allowed); ValueError if it does not parse."""
    path = Path(path)
    try:
        text = path.read_text(encoding="utf-8")
    except OSError as error:
        raise ValueError(f"{path}: cannot be read: {error}") from error
    if not text.strip():
        raise ValueError(f"{path}: the file is empty")
    if path.suffix == ".json":
        try:
            return json.loads(text)
        except json.JSONDecodeError as error:
            raise ValueError(f"{path}:{error.lineno}:{error.colno}: invalid JSON: {error.msg}") from error
    document, matches = decode_str(text)
    if matches or document is None:
        problems = "; ".join(f"line {m.linenumber}: {m.message}" for m in matches) or "invalid YAML"
        raise ValueError(f"{path}: {problems}")
    return _plain(document)


def is_template(document: Any) -> bool:
    """A CloudFormation or SAM template: a mapping with a `Resources` mapping."""
    return isinstance(document, dict) and isinstance(document.get("Resources"), dict)


def template(path: str | Path) -> dict:
    """A CloudFormation template; fails the test with a clear message if it is not one."""
    document = load(path)
    assert is_template(document), f"{path} is not a CloudFormation template (no Resources)"
    return document


def resources(tpl: dict, type_: str | None = None) -> dict[str, dict]:
    """The template's resources by logical id, only those of `type_` when given."""
    found = tpl.get("Resources", {})
    return {name: res for name, res in found.items()
            if type_ is None or res.get("Type") == type_}


def resource(tpl: dict, type_: str) -> tuple[str, dict]:
    """The one resource of `type_`; fails the test when there are none or several."""
    found = resources(tpl, type_)
    assert len(found) == 1, f"expected exactly one {type_}, found {len(found)}: {sorted(found)}"
    return next(iter(found.items()))


def ref(value: Any) -> str | None:
    """The logical id in `{"Ref": "X"}` (or `!Ref X`), else None."""
    if isinstance(value, dict) and set(value) == {"Ref"}:
        return value["Ref"]
    return None


def getatt(value: Any) -> tuple[str, str] | None:
    """(logical id, attribute) from `{"Fn::GetAtt": ["X", "Arn"]}` or `"X.Arn"`, else None."""
    if isinstance(value, dict) and set(value) == {"Fn::GetAtt"}:
        target = value["Fn::GetAtt"]
        if isinstance(target, str):
            name, _, attribute = target.partition(".")
            return name, attribute
        if isinstance(target, list) and len(target) == 2:
            return str(target[0]), str(target[1])
    return None


def parameters(tpl: dict) -> dict[str, dict]:
    return tpl.get("Parameters", {}) or {}


def outputs(tpl: dict) -> dict[str, dict]:
    return tpl.get("Outputs", {}) or {}


def guard(rules: str | Path, data: str | Path) -> list[str]:
    """Runs `cfn-guard validate`; returns the failures as readable lines (empty when all pass).

    `rules` and `data` are file paths. A rule that is skipped (its `when` guard or its
    filter matched nothing) is not a failure, as in cfn-guard itself.
    """
    found = subprocess.run(
        ["cfn-guard", "validate", "--rules", str(rules), "--data", str(data),
         "--output-format", "json", "--show-summary", "none", "--structured"],
        capture_output=True,
        text=True,
        check=False,
    )
    if found.returncode not in (0, 19):
        problem = (found.stderr.strip() or found.stdout.strip()).split("fragment", 1)[0]
        raise AssertionError(f"cfn-guard could not run: {problem[:400]}")
    failures: list[str] = []
    for report in _json_documents(found.stdout):
        for rule in report.get("not_compliant", []):
            rule_name = rule.get("Rule", {}).get("name", "?")
            checks = rule.get("Rule", {}).get("checks", [])
            messages = sorted({_check_message(check) for check in checks}) or [""]
            for message in messages:
                failures.append(f"{rule_name}: {message}".rstrip(": "))
    return failures


def _json_documents(text: str) -> list[dict]:
    decoder = json.JSONDecoder()
    position, documents = 0, []
    text = text.strip()
    while position < len(text):
        document, end = decoder.raw_decode(text, position)
        documents.extend(document if isinstance(document, list) else [document])
        position = end
        while position < len(text) and text[position].isspace():
            position += 1
    return documents


def _check_message(check: dict) -> str:
    """`Resource: message`, from the custom message (<<...>>) or cfn-guard's own."""
    messages = _find(check, "messages")
    custom = next((m.get("custom_message") for m in messages if m.get("custom_message")), None)
    error = next((m.get("error_message") for m in messages if m.get("error_message")), None)
    path = _first(check, "path") or ""
    parts = [part for part in str(path).split("/") if part]
    where = parts[1] if len(parts) >= 2 and parts[0] == "Resources" else "/".join(parts[:2])
    text = (custom or error or "not compliant").strip()
    return f"{where}: {text}" if where else text


def _find(value: Any, key: str) -> list[Any]:
    found = []
    if isinstance(value, dict):
        for name, item in value.items():
            if name == key:
                found.append(item)
            found.extend(_find(item, key))
    elif isinstance(value, list):
        for item in value:
            found.extend(_find(item, key))
    return found


def _first(value: Any, key: str) -> Any:
    items = _find(value, key)
    return next((item for item in items if item), None)
