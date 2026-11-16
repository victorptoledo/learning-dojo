"""Deterministic guardrails around a model call: PII, injections, tool policy, output checks."""

from collections.abc import Mapping, Sequence
from dataclasses import dataclass, field

from llm import ToolCall

INJECTION_PATTERNS: dict[str, str] = {
    "ignore_instructions": r"\b(ignore|disregard|forget)\b.{0,30}\b(previous|prior|above|earlier)\b.{0,20}\b(instructions|rules|prompts?)\b",
    "role_override": r"\byou are now\b|\bact as\b|\bnew instructions\b",
    "prompt_extraction": r"\b(reveal|print|show|repeat)\b.{0,30}\b(system prompt|your instructions|hidden instructions)\b",
    "exfiltration": r"\b(send|forward|email|post)\b.{0,60}\b(to|at)\b\s+\S+@\S+",
}


@dataclass(frozen=True)
class Redaction:
    text: str
    found: dict[str, str]


@dataclass(frozen=True)
class Session:
    user_id: str
    tainted: bool = False


@dataclass(frozen=True)
class ToolRule:
    side_effect: bool = False
    owner_argument: str | None = None
    limits: dict[str, float] = field(default_factory=dict)


@dataclass(frozen=True)
class Decision:
    action: str  # "allow", "confirm" or "deny"
    reason: str = ""


def luhn_valid(number: str) -> bool:
    raise NotImplementedError("todo: write luhn_valid()")


def redact(text: str) -> Redaction:
    raise NotImplementedError("todo: write redact()")


def restore(text: str, found: Mapping[str, str]) -> str:
    raise NotImplementedError("todo: write restore()")


def detect_injection(text: str) -> list[str]:
    raise NotImplementedError("todo: write detect_injection()")


def wrap_untrusted(source: str, text: str) -> str:
    raise NotImplementedError("todo: write wrap_untrusted()")


class ToolPolicy:
    def __init__(self, rules: Mapping[str, ToolRule]) -> None:
        self.rules = dict(rules)

    def decide(self, call: ToolCall, session: Session) -> Decision:
        raise NotImplementedError("todo: write ToolPolicy.decide()")


def check_output(text: str, allowed_domains: Sequence[str]) -> list[str]:
    raise NotImplementedError("todo: write check_output()")
