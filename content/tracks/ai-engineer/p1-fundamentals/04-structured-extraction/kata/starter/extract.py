"""Find, validate and repair JSON in model replies."""

from collections.abc import Sequence
from typing import Any

from llm import Model


class ExtractionError(Exception):
    def __init__(self, message: str, errors: Sequence[str] = (), attempts: int = 0) -> None:
        super().__init__(message)
        self.errors = list(errors)
        self.attempts = attempts


def find_json(text: str) -> dict[str, Any]:
    raise NotImplementedError("todo: write find_json()")


def validate(value: Any, schema: dict[str, Any], path: str = "$") -> list[str]:
    raise NotImplementedError("todo: write validate()")


def extract(model: Model, text: str, schema: dict[str, Any], max_attempts: int = 3) -> dict[str, Any]:
    raise NotImplementedError("todo: write extract()")
