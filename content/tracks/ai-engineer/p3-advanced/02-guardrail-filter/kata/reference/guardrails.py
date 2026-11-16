"""Deterministic guardrails around a model call: PII, injections, tool policy, output checks."""

import re
from collections.abc import Mapping, Sequence
from dataclasses import dataclass, field
from urllib.parse import urlsplit

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


_CARD = re.compile(r"(?<![\d])\d(?:[ -]?\d){12,18}(?![\d])")
_EMAIL = re.compile(r"[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}")
_PHONE = re.compile(r"\+\d[\d -]*\d")
_ZERO_WIDTH = ("​", "‌", "‍", "⁠", "﻿")
_IMAGE = re.compile(r"!\[[^\]]*\]\((https?://[^)\s]+)\)")
_URL = re.compile(r"https?://[^\s)\]]+")


def luhn_valid(number: str) -> bool:
    if not number.isdigit() or not 13 <= len(number) <= 19:
        return False
    total = 0
    for position, char in enumerate(reversed(number)):
        digit = int(char)
        if position % 2 == 1:
            digit *= 2
            if digit > 9:
                digit -= 9
        total += digit
    return total % 10 == 0


def redact(text: str) -> Redaction:
    found: dict[str, str] = {}

    def replace(pattern: re.Pattern[str], kind: str, text: str, valid) -> str:
        placeholders: dict[str, str] = {}

        def substitute(match: re.Match[str]) -> str:
            value = match.group(0)
            if not valid(value):
                return value
            if value not in placeholders:
                placeholders[value] = f"<{kind}_{len(placeholders) + 1}>"
                found[placeholders[value]] = value
            return placeholders[value]

        return pattern.sub(substitute, text)

    text = replace(_CARD, "CARD", text, lambda v: luhn_valid(re.sub(r"[ -]", "", v)))
    text = replace(_EMAIL, "EMAIL", text, lambda v: True)
    text = replace(_PHONE, "PHONE", text, lambda v: 9 <= sum(c.isdigit() for c in v) <= 15)
    return Redaction(text, found)


def restore(text: str, found: Mapping[str, str]) -> str:
    for placeholder, value in found.items():
        text = text.replace(placeholder, value)
    return text


def detect_injection(text: str) -> list[str]:
    names = [name for name, pattern in INJECTION_PATTERNS.items() if re.search(pattern, text, re.IGNORECASE)]
    if any(char in text for char in _ZERO_WIDTH):
        names.append("hidden_characters")
    if _IMAGE.search(text):
        names.append("markdown_image")
    return names


def wrap_untrusted(source: str, text: str) -> str:
    safe = text.replace("</untrusted", "&lt;/untrusted").replace("<untrusted", "&lt;untrusted")
    return f'<untrusted source="{source.replace('"', "&quot;")}">\n{safe}\n</untrusted>'


class ToolPolicy:
    def __init__(self, rules: Mapping[str, ToolRule]) -> None:
        self.rules = dict(rules)

    def decide(self, call: ToolCall, session: Session) -> Decision:
        rule = self.rules.get(call.name)
        if rule is None:
            return Decision("deny", f"tool '{call.name}' is not allowed")
        if rule.owner_argument and call.arguments.get(rule.owner_argument) != session.user_id:
            return Decision("deny", "not the session user's data")
        for argument, limit in rule.limits.items():
            value = call.arguments.get(argument)
            if value is not None and value > limit:
                return Decision("deny", f"{argument} {value} exceeds the limit {limit}")
        if rule.side_effect and session.tainted:
            return Decision("confirm", "side effect after reading untrusted content")
        if rule.side_effect:
            return Decision("confirm", "side effect needs the user's confirmation")
        return Decision("allow", "")


def _allowed(host: str, domains: Sequence[str]) -> bool:
    return any(host == domain or host.endswith("." + domain) for domain in domains)


def check_output(text: str, allowed_domains: Sequence[str]) -> list[str]:
    problems: list[tuple[int, str]] = []
    image_spans = []
    for match in _IMAGE.finditer(text):
        image_spans.append(match.span(1))
        host = urlsplit(match.group(1)).hostname or ""
        if not _allowed(host, allowed_domains):
            problems.append((match.start(), f"image from untrusted domain {host}"))
    for match in _URL.finditer(text):
        if any(start <= match.start() < end for start, end in image_spans):
            continue
        host = urlsplit(match.group(0)).hostname or ""
        if not _allowed(host, allowed_domains):
            problems.append((match.start(), f"link to untrusted domain {host}"))
    return [problem for _, problem in sorted(problems)]
