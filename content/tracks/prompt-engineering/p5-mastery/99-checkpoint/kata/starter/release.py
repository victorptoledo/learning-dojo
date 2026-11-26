"""Release a prompt version: parse it, check the bump, roll it out, guard each call, decide."""

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
    raise NotImplementedError("todo: write parse_prompt_file()")


def required_bump(old: PromptVersion, new: PromptVersion) -> str | None:
    """The smallest release the difference needs, or None when nothing changed."""
    raise NotImplementedError("todo: write required_bump()")


def check_release(old: PromptVersion, new: PromptVersion) -> list[str]:
    """The reasons the new version may not replace the old one; [] means it may."""
    raise NotImplementedError("todo: write check_release()")


def bucket(key: str) -> int:
    """A stable number from 0 to 99 for the key."""
    raise NotImplementedError("todo: write bucket()")


def pick(user_id: str, stable: PromptVersion, candidate: PromptVersion, percent: int) -> PromptVersion:
    """The version this user gets while `percent` of users are on the candidate."""
    raise NotImplementedError("todo: write pick()")


def build_request(version: PromptVersion, input_text: str) -> Request:
    """The request for one call: the version's parameters and the rendered, escaped template."""
    raise NotImplementedError("todo: write build_request()")


def check_output(version: PromptVersion, reply: Reply) -> tuple[dict | None, list[str]]:
    """The parsed output and [] when the reply meets the version's contract, else None and the problems."""
    raise NotImplementedError("todo: write check_output()")


def call(
    version: PromptVersion, input_text: str, model: Model, *, fallback: PromptVersion | None = None
) -> CallRecord:
    """One guarded call: a retry for format problems only, then the fallback version."""
    raise NotImplementedError("todo: write call()")


def decide_rollout(
    records: Sequence[CallRecord], candidate: PromptVersion, *, min_calls: int, max_failure_rate: float
) -> str:
    """Decide "promote", "rollback" or "wait" from the candidate's call records."""
    raise NotImplementedError("todo: write decide_rollout()")
