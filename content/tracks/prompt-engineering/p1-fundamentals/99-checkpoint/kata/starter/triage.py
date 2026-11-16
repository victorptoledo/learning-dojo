"""The code around a ticket-triage prompt: messages, escaping, parsing and validation."""

from collections.abc import Iterable

CATEGORIES = ("billing", "bug", "account", "other")
PRIORITIES = ("low", "medium", "high")
FIELDS = ("category", "priority", "summary")
MAX_SUMMARY = 200


class TriageError(Exception):
    """The model's answer could not be used; .problems lists why."""

    def __init__(self, problems: list[str]) -> None:
        super().__init__("; ".join(problems))
        self.problems = problems


def system_prompt() -> str:
    """The standing instructions for the triage model."""
    raise NotImplementedError("todo: write system_prompt()")


def escape_ticket(text: str) -> str:
    """The ticket text, stripped and escaped so it cannot close the <ticket> tag."""
    raise NotImplementedError("todo: write escape_ticket()")


def validate(answer: object) -> list[str]:
    """Every problem with a parsed answer, in the documented order."""
    raise NotImplementedError("todo: write validate()")


def parse_response(text: str) -> dict:
    """Find, parse and validate the JSON object in the model's answer."""
    raise NotImplementedError("todo: write parse_response()")


def build_messages(ticket: str, examples: Iterable[tuple[str, dict]] = ()) -> list[dict]:
    """System prompt, few-shot turns, then the ticket."""
    raise NotImplementedError("todo: write build_messages()")


def retry_message(error: TriageError) -> dict:
    """The user turn that lists the problems and asks for the object again."""
    raise NotImplementedError("todo: write retry_message()")
