"""Release a prompt version: parse it, check the bump, roll it out, guard each call, decide."""

import hashlib
import html
import json
import re
from collections.abc import Callable, Sequence
from dataclasses import dataclass

KEYS = ("id", "version", "model", "temperature", "max_tokens", "output")
SEMVER = re.compile(r"(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)")
SNAPSHOT = re.compile(r"[a-z0-9.-]+-[0-9]{8}")
FIELD = re.compile(r"[a-z_]+")
BUMPS = ("patch", "minor", "major")
RETRY = "Your answer could not be used: {problems}\nAnswer again with only the JSON object."


@dataclass(frozen=True)
class PromptVersion:
    id: str
    version: tuple[int, int, int]
    model: str
    temperature: float
    max_tokens: int
    output: tuple[str, ...]
    template: str

    @property
    def label(self) -> str:
        return f"{self.id}@{'.'.join(map(str, self.version))}"


@dataclass(frozen=True)
class Request:
    model: str
    temperature: float
    max_tokens: int
    messages: list[dict]


@dataclass(frozen=True)
class Reply:
    text: str
    stop_reason: str


@dataclass(frozen=True)
class CallRecord:
    version: str
    attempts: int
    ok: bool
    fallback_used: bool
    output: dict | None
    problems: list[str]


Model = Callable[[Request], Reply]


def parse_prompt_file(text: str) -> PromptVersion:
    """A prompt version from a file with a front matter block and the template after it."""
    lines = text.split("\n")
    if not lines or lines[0].strip() != "---":
        raise ValueError("missing front matter")
    try:
        end = next(index for index in range(1, len(lines)) if lines[index].strip() == "---")
    except StopIteration:
        raise ValueError("unclosed front matter") from None
    values: dict[str, str] = {}
    for index in range(1, end):
        line = lines[index]
        if not line.strip():
            continue
        if ":" not in line:
            raise ValueError(f"line {index + 1}: expected 'key: value'")
        key, value = (part.strip() for part in line.split(":", 1))
        if key not in KEYS:
            raise ValueError(f"line {index + 1}: unknown key '{key}'")
        if key in values:
            raise ValueError(f"line {index + 1}: duplicate key '{key}'")
        values[key] = value
    for key in KEYS:
        if key not in values:
            raise ValueError(f"missing key '{key}'")
    match = SEMVER.fullmatch(values["version"])
    if not match:
        raise ValueError(f"invalid version '{values['version']}'")
    if not SNAPSHOT.fullmatch(values["model"]):
        raise ValueError(f"model '{values['model']}' is not a pinned snapshot")
    try:
        temperature = float(values["temperature"])
    except ValueError:
        temperature = -1.0
    if not 0.0 <= temperature <= 1.0:
        raise ValueError(f"invalid temperature '{values['temperature']}'")
    if not values["max_tokens"].isdigit() or int(values["max_tokens"]) == 0:
        raise ValueError(f"invalid max_tokens '{values['max_tokens']}'")
    output = tuple(part.strip() for part in values["output"].split(","))
    if not all(FIELD.fullmatch(name) for name in output) or len(set(output)) != len(output):
        raise ValueError(f"invalid output '{values['output']}'")
    template = "\n".join(lines[end + 1 :]).strip()
    if "{{input}}" not in template:
        raise ValueError("the template has no {{input}}")
    version = (int(match.group(1)), int(match.group(2)), int(match.group(3)))
    return PromptVersion(
        values["id"], version, values["model"], temperature, int(values["max_tokens"]), output, template
    )


def required_bump(old: PromptVersion, new: PromptVersion) -> str | None:
    """The smallest release the difference needs, or None when nothing changed."""
    if old.output != new.output:
        return "major"
    if (old.model, old.temperature, old.max_tokens) != (new.model, new.temperature, new.max_tokens):
        return "minor"
    if old.template != new.template:
        return "patch"
    return None


def _text(version: tuple[int, int, int]) -> str:
    return ".".join(map(str, version))


def check_release(old: PromptVersion, new: PromptVersion) -> list[str]:
    """The reasons the new version may not replace the old one; [] means it may."""
    if old.id != new.id:
        return [f"id changed from '{old.id}' to '{new.id}'"]
    major, minor, patch = old.version
    nexts = {(major + 1, 0, 0): "major", (major, minor + 1, 0): "minor", (major, minor, patch + 1): "patch"}
    actual = nexts.get(new.version)
    if actual is None:
        return [f"{_text(new.version)} does not follow {_text(old.version)}"]
    needed = required_bump(old, new)
    if needed is None:
        return [f"nothing changed since {_text(old.version)}"]
    if BUMPS.index(actual) < BUMPS.index(needed):
        return [f"this change needs a {needed} release, not a {actual}"]
    return []


def bucket(key: str) -> int:
    """A stable number from 0 to 99 for the key."""
    return int(hashlib.sha256(key.encode("utf-8")).hexdigest(), 16) % 100


def pick(user_id: str, stable: PromptVersion, candidate: PromptVersion, percent: int) -> PromptVersion:
    """The version this user gets while `percent` of users are on the candidate."""
    if not 0 <= percent <= 100:
        raise ValueError(f"invalid percent {percent}")
    return candidate if bucket(f"{candidate.label}:{user_id}") < percent else stable


def build_request(version: PromptVersion, input_text: str) -> Request:
    """The request for one call: the version's parameters and the rendered, escaped template."""
    content = version.template.replace("{{input}}", html.escape(input_text, quote=False))
    return Request(version.model, version.temperature, version.max_tokens, [{"role": "user", "content": content}])


def check_output(version: PromptVersion, reply: Reply) -> tuple[dict | None, list[str]]:
    """The parsed output and [] when the reply meets the version's contract, else None and the problems."""
    if reply.stop_reason == "max_tokens":
        return None, ["truncated"]
    if reply.stop_reason == "refusal":
        return None, ["refused"]
    try:
        data = json.loads(reply.text.strip())
    except json.JSONDecodeError:
        return None, ["not valid JSON"]
    if not isinstance(data, dict):
        return None, ["not a JSON object"]
    problems = [f"missing key '{key}'" for key in version.output if key not in data]
    return (None, problems) if problems else (data, [])


def call(
    version: PromptVersion, input_text: str, model: Model, *, fallback: PromptVersion | None = None
) -> CallRecord:
    """One guarded call: a retry for format problems only, then the fallback version."""
    request = build_request(version, input_text)
    reply = model(request)
    attempts = 1
    data, problems = check_output(version, reply)
    if problems and problems not in (["truncated"], ["refused"]):
        messages = request.messages + [
            {"role": "assistant", "content": reply.text},
            {"role": "user", "content": RETRY.format(problems="; ".join(problems))},
        ]
        reply = model(Request(request.model, request.temperature, request.max_tokens, messages))
        attempts += 1
        data, problems = check_output(version, reply)
    if not problems:
        return CallRecord(version.label, attempts, True, False, data, [])
    if fallback is None:
        return CallRecord(version.label, attempts, False, False, None, problems)
    fallback_data, fallback_problems = check_output(fallback, model(build_request(fallback, input_text)))
    problems = problems + [f"fallback: {problem}" for problem in fallback_problems]
    return CallRecord(version.label, attempts + 1, not fallback_problems, True, fallback_data, problems)


def decide_rollout(
    records: Sequence[CallRecord], candidate: PromptVersion, *, min_calls: int, max_failure_rate: float
) -> str:
    """Decide "promote", "rollback" or "wait" from the candidate's call records."""
    mine = [record for record in records if record.version == candidate.label]
    failures = sum(1 for record in mine if not record.ok or record.fallback_used)
    if len(mine) < min_calls:
        return "rollback" if failures > max_failure_rate * min_calls else "wait"
    return "rollback" if failures / len(mine) > max_failure_rate else "promote"
